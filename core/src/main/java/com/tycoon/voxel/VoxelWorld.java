package com.tycoon.voxel;

import java.util.ArrayList;
import java.util.List;

/**
 * A dense grid of blocks: the world state, with no rendering and no LibGDX
 * dependency, so terrain generation, collision and raycasting are all testable
 * headlessly.
 *
 * <p>The grid is stored flat in an {@code int[]} of {@link BlockType} ordinals.
 * Out-of-bounds reads return air (and {@link #isSolid} treats everything below
 * the bottom layer as solid), which lets the collision and meshing code skip
 * bounds checks at every call site.
 *
 * <p>Player edits are recorded in {@link #edits()} so they can be written to the
 * save file and replayed on top of freshly generated terrain.
 */
public class VoxelWorld {

    public static final int SIZE_X = 64;
    public static final int SIZE_Y = 48;
    public static final int SIZE_Z = 64;

    /** Y level the terrain generator aims the surface at. */
    public static final int SEA_LEVEL = 18;

    private final int[] blocks = new int[SIZE_X * SIZE_Y * SIZE_Z];
    private final List<int[]> editLog = new ArrayList<>();

    /** A mined or placed block: x, y, z and the {@link BlockType} ordinal. */
    public List<int[]> edits() {
        return editLog;
    }

    public static int index(int x, int y, int z) {
        return (y * SIZE_Z + z) * SIZE_X + x;
    }

    public static boolean inBounds(int x, int y, int z) {
        return x >= 0 && x < SIZE_X && y >= 0 && y < SIZE_Y && z >= 0 && z < SIZE_Z;
    }

    public BlockType get(int x, int y, int z) {
        if (!inBounds(x, y, z)) {
            return BlockType.AIR;
        }
        return BlockType.byId(blocks[index(x, y, z)]);
    }

    /** Sets a block without recording an edit; used by the terrain generator. */
    public void set(int x, int y, int z, BlockType type) {
        if (!inBounds(x, y, z)) {
            return;
        }
        blocks[index(x, y, z)] = type.ordinal();
    }

    /** Sets a block and records it in the edit log so it survives a save. */
    public void setAndRecord(int x, int y, int z, BlockType type) {
        if (!inBounds(x, y, z)) {
            return;
        }
        set(x, y, z, type);
        editLog.add(new int[] {x, y, z, type.ordinal()});
    }

    /** Re-applies a recorded edit; called after terrain generation on load. */
    public void applyEdit(int x, int y, int z, BlockType type) {
        set(x, y, z, type);
        editLog.add(new int[] {x, y, z, type.ordinal()});
    }

    public boolean isAir(int x, int y, int z) {
        return get(x, y, z).isAir();
    }

    /**
     * Whether a block stops the player. Below the world floor everything is
     * solid, so a fall can never leave the map.
     */
    public boolean isSolid(int x, int y, int z) {
        if (y < 0) {
            return true;
        }
        if (y >= SIZE_Y) {
            return false;
        }
        return get(x, y, z).isSolid();
    }

    public boolean isOpaque(int x, int y, int z) {
        return get(x, y, z).isOpaque();
    }

    /**
     * Whether the axis-aligned box overlaps any solid block. Used for player
     * collision, one axis at a time.
     */
    public boolean intersectsSolid(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        int x0 = (int) Math.floor(minX);
        int x1 = (int) Math.floor(maxX);
        int y0 = (int) Math.floor(minY);
        int y1 = (int) Math.floor(maxY);
        int z0 = (int) Math.floor(minZ);
        int z1 = (int) Math.floor(maxZ);

        for (int y = y0; y <= y1; y++) {
            for (int z = z0; z <= z1; z++) {
                for (int x = x0; x <= x1; x++) {
                    if (isSolid(x, y, z)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /** Highest solid block in a column, or -1 when the column is empty. */
    public int highestSolidY(int x, int z) {
        for (int y = SIZE_Y - 1; y >= 0; y--) {
            if (isSolid(x, y, z)) {
                return y;
            }
        }
        return -1;
    }

    /** Fills the whole grid with a single block; used to reset before generation. */
    public void fill(BlockType type) {
        java.util.Arrays.fill(blocks, type.ordinal());
    }

    public void clearEdits() {
        editLog.clear();
    }
}
