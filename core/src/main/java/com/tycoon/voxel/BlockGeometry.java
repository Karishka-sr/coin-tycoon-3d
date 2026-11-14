package com.tycoon.voxel;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.ShortArray;

/**
 * Turns blocks into triangles, skipping every face that touches a neighbour.
 *
 * <p>Hidden-face removal is what makes a blocky world affordable: only the blocks
 * on the surface contribute geometry, so a 64x48x64 world becomes a few thousand
 * quads instead of a quarter of a million cubes.
 *
 * <p>The world is meshed in vertical chunks. Each chunk is kept well under the
 * 65535-vertex ceiling of 16-bit indices, which lets the renderer use
 * {@code glDrawElements} with unsigned shorts.
 */
public final class BlockGeometry {

    // Face indices, also used by the raycast to report which side was hit.
    public static final int TOP = 0;
    public static final int BOTTOM = 1;
    public static final int POS_X = 2;
    public static final int NEG_X = 3;
    public static final int POS_Z = 4;
    public static final int NEG_Z = 5;

    /** Aliases so the raycast can talk about faces as signed axes. */
    public static final int POS_Y = TOP;
    public static final int NEG_Y = BOTTOM;

    /** Chunk footprint; the whole world is 64x64, so a chunk is a 16x16 column. */
    public static final int CHUNK_SIZE = 16;

    public static final int CHUNKS_X = VoxelWorld.SIZE_X / CHUNK_SIZE;
    public static final int CHUNKS_Z = VoxelWorld.SIZE_Z / CHUNK_SIZE;
    public static final int CHUNK_COUNT = CHUNKS_X * CHUNKS_Z;

    /**
     * Per-face brightness. Without textures a flat colour reads as a flat blob,
     * so each face is shaded to hint at the light direction.
     */
    private static final float[] FACE_SHADE = {1.0f, 0.55f, 0.82f, 0.70f, 0.90f, 0.62f};

    // Unit normals, indexed by face.
    private static final int[][] NORMALS = {
        {0, 1, 0}, {0, -1, 0}, {1, 0, 0}, {-1, 0, 0}, {0, 0, 1}, {0, 0, -1},
    };

    // The four corners of each face, counter-clockwise seen from outside.
    private static final int[][][] CORNERS = {
        // TOP (+Y)
        {{0, 1, 1}, {1, 1, 1}, {1, 1, 0}, {0, 1, 0}},
        // BOTTOM (-Y)
        {{0, 0, 0}, {1, 0, 0}, {1, 0, 1}, {0, 0, 1}},
        // POS_X (+X)
        {{1, 0, 1}, {1, 0, 0}, {1, 1, 0}, {1, 1, 1}},
        // NEG_X (-X)
        {{0, 0, 0}, {0, 0, 1}, {0, 1, 1}, {0, 1, 0}},
        // POS_Z (+Z)
        {{0, 0, 1}, {0, 1, 1}, {1, 1, 1}, {1, 0, 1}},
        // NEG_Z (-Z)
        {{1, 0, 0}, {1, 1, 0}, {0, 1, 0}, {0, 0, 0}},
    };

    // Neighbour offset per face, in the same order.
    private static final int[][] NEIGHBOURS = {
        {0, 1, 0}, {0, -1, 0}, {1, 0, 0}, {-1, 0, 0}, {0, 0, 1}, {0, 0, -1},
    };

    private BlockGeometry() {
    }

    /** Chunk index for a block, or -1 when out of range. */
    public static int chunkOf(int x, int z) {
        if (!VoxelWorld.inBounds(x, 0, z)) {
            return -1;
        }
        return (z / CHUNK_SIZE) * CHUNKS_X + (x / CHUNK_SIZE);
    }

    /**
     * Builds the geometry for one chunk. Positions are absolute world coordinates,
     * so the caller can drop the mesh straight into the scene without a transform.
     */
    public static VoxelMesh build(VoxelWorld world, int chunk) {
        FloatArray vertices = new FloatArray(4096);
        ShortArray indices = new ShortArray(6144);

        int chunkX = (chunk % CHUNKS_X) * CHUNK_SIZE;
        int chunkZ = (chunk / CHUNKS_X) * CHUNK_SIZE;

        for (int x = chunkX; x < chunkX + CHUNK_SIZE; x++) {
            for (int z = chunkZ; z < chunkZ + CHUNK_SIZE; z++) {
                for (int y = 0; y < VoxelWorld.SIZE_Y; y++) {
                    BlockType type = world.get(x, y, z);
                    if (type.isAir()) {
                        continue;
                    }
                    emitBlock(world, vertices, indices, x, y, z, type);
                }
            }
        }

        return new VoxelMesh(vertices, indices);
    }

    private static void emitBlock(VoxelWorld world, FloatArray vertices, ShortArray indices,
                                  int x, int y, int z, BlockType type) {
        for (int face = 0; face < 6; face++) {
            int[] n = NEIGHBOURS[face];
            BlockType neighbour = world.get(x + n[0], y + n[1], z + n[2]);

            // A face is visible unless the neighbour is opaque, or both blocks are
            // the same see-through type (water against water, glass against glass).
            boolean hidden = neighbour.isOpaque() || (neighbour == type && !type.isOpaque());
            if (hidden) {
                continue;
            }

            addFace(vertices, indices, x, y, z, face, type);
        }
    }

    private static void addFace(FloatArray vertices, ShortArray indices,
                                int x, int y, int z, int face, BlockType type) {
        short base = (short) (vertices.size / VoxelMesh.FLOATS_PER_VERTEX);

        float[] colour = type.colourFor(face);
        float shade = FACE_SHADE[face];
        float packed = Color.toFloatBits(colour[0] * shade, colour[1] * shade, colour[2] * shade, 1f);

        int[] normal = NORMALS[face];
        int[][] corners = CORNERS[face];

        for (int[] corner : corners) {
            vertices.add(x + corner[0]);
            vertices.add(y + corner[1]);
            vertices.add(z + corner[2]);
            vertices.add(normal[0]);
            vertices.add(normal[1]);
            vertices.add(normal[2]);
            vertices.add(packed);
        }

        // Two triangles; winding matches the counter-clockwise corner order above.
        indices.add(base);
        indices.add((short) (base + 1));
        indices.add((short) (base + 2));
        indices.add(base);
        indices.add((short) (base + 2));
        indices.add((short) (base + 3));
    }
}
