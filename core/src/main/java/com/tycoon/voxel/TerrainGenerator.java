package com.tycoon.voxel;

/**
 * Builds the starting terrain.
 *
 * <p>Everything comes from a small deterministic value-noise function seeded from
 * a single {@code long}, so the same seed always produces the same world and the
 * generator stays dependency-free (and therefore unit-testable headlessly).
 *
 * <p>The shape is deliberately Minecraft-like: a bedrock floor, stone with ore
 * veins, a dirt layer capped with grass, sand around the shoreline, water filling
 * the low spots, and the occasional tree.
 */
public final class TerrainGenerator {

    private final long seed;

    public TerrainGenerator(long seed) {
        this.seed = seed;
    }

    /** Clears {@code world} and fills it with fresh terrain. */
    public void generate(VoxelWorld world) {
        world.fill(BlockType.AIR);

        int[][] heights = new int[VoxelWorld.SIZE_X][VoxelWorld.SIZE_Z];
        for (int x = 0; x < VoxelWorld.SIZE_X; x++) {
            for (int z = 0; z < VoxelWorld.SIZE_Z; z++) {
                int height = surfaceHeight(x, z);
                heights[x][z] = height;
                fillColumn(world, x, z, height);
            }
        }

        plantTrees(world, heights);
    }

    /** Terrain surface height for a column, in blocks. */
    public int surfaceHeight(int x, int z) {
        // Two octaves: broad hills plus a finer wobble.
        float broad = noise(x * 0.045f, z * 0.045f);
        float detail = noise(x * 0.11f + 31.7f, z * 0.11f + 12.3f);
        float h = broad * 7f + detail * 2.5f;

        int height = VoxelWorld.SEA_LEVEL + Math.round(h);
        return Math.max(4, Math.min(VoxelWorld.SIZE_Y - 12, height));
    }

    private void fillColumn(VoxelWorld world, int x, int z, int height) {
        world.set(x, 0, z, BlockType.BEDROCK);

        boolean underwater = height < VoxelWorld.SEA_LEVEL;

        for (int y = 1; y <= height; y++) {
            BlockType type;
            if (y == height) {
                type = underwater ? BlockType.SAND : BlockType.GRASS;
            } else if (y >= height - 3) {
                type = underwater ? BlockType.SAND : BlockType.DIRT;
            } else {
                type = BlockType.STONE;
            }

            if (type == BlockType.STONE) {
                BlockType ore = oreAt(x, y, z);
                if (ore != null) {
                    type = ore;
                }
            }
            world.set(x, y, z, type);
        }

        if (underwater) {
            for (int y = height + 1; y <= VoxelWorld.SEA_LEVEL; y++) {
                world.set(x, y, z, BlockType.WATER);
            }
        }
    }

    /**
     * Picks an ore for a stone block, or {@code null} for plain stone. Rarer ores
     * sit deeper: the probability is gated on the depth below the surface band.
     */
    private BlockType oreAt(int x, int y, int z) {
        double r = hash01(x, y, z, 0x9E37L);
        if (r < 0.0060 && y <= 12) {
            return BlockType.DIAMOND_ORE;
        }
        if (r < 0.0140 && y <= 16) {
            return BlockType.GOLD_ORE;
        }
        if (r < 0.0330 && y <= 26) {
            return BlockType.IRON_ORE;
        }
        if (r < 0.0700) {
            return BlockType.COAL_ORE;
        }
        return null;
    }

    private void plantTrees(VoxelWorld world, int[][] heights) {
        for (int x = 3; x < VoxelWorld.SIZE_X - 3; x++) {
            for (int z = 3; z < VoxelWorld.SIZE_Z - 3; z++) {
                int height = heights[x][z];
                if (height <= VoxelWorld.SEA_LEVEL) {
                    continue;
                }
                if (world.get(x, height, z) != BlockType.GRASS) {
                    continue;
                }
                if (hash01(x, 7, z, 0x51EDL) >= 0.012) {
                    continue;
                }
                growTree(world, x, height + 1, z);
            }
        }
    }

    private void growTree(VoxelWorld world, int x, int baseY, int z) {
        int trunk = 4 + (int) (hash01(x, 3, z, 0xBEEFL) * 3);

        for (int i = 0; i < trunk; i++) {
            world.set(x, baseY + i, z, BlockType.WOOD);
        }

        int canopyY = baseY + trunk - 1;
        for (int dy = -1; dy <= 2; dy++) {
            int radius = dy <= 0 ? 2 : 1;
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    // Trim the corners so the crown looks round rather than cubic.
                    if (Math.abs(dx) == radius && Math.abs(dz) == radius && (dy >= 1 || hash01(x + dx, canopyY + dy, z + dz, 0x7F3L) < 0.5)) {
                        continue;
                    }
                    int bx = x + dx;
                    int by = canopyY + dy;
                    int bz = z + dz;
                    if (world.isAir(bx, by, bz)) {
                        world.set(bx, by, bz, BlockType.LEAVES);
                    }
                }
            }
        }
    }

    // ---- noise -------------------------------------------------------------

    /** Smooth value noise in roughly [-1, 1]. */
    private float noise(float x, float z) {
        int x0 = (int) Math.floor(x);
        int z0 = (int) Math.floor(z);
        float fx = x - x0;
        float fz = z - z0;

        float v00 = value(x0, z0);
        float v10 = value(x0 + 1, z0);
        float v01 = value(x0, z0 + 1);
        float v11 = value(x0 + 1, z0 + 1);

        // Smoothstep keeps the lattice from showing as ridges.
        float sx = fx * fx * (3f - 2f * fx);
        float sz = fz * fz * (3f - 2f * fz);

        float top = v00 + (v10 - v00) * sx;
        float bottom = v01 + (v11 - v01) * sx;
        return (top + (bottom - top) * sz) * 2f - 1f;
    }

    private float value(int x, int z) {
        return (float) hash01(x, 0, z, 0xC0FFEEL);
    }

    /** Deterministic hash of a coordinate triple into [0, 1). */
    private double hash01(int x, int y, int z, long salt) {
        long h = seed + salt;
        h = h * 6364136223846793005L + 1442695040888963407L;
        h ^= x * 0x9E3779B97F4A7C15L;
        h = h * 6364136223846793005L + 1442695040888963407L;
        h ^= y * 0xC2B2AE3D27D4EB4FL;
        h = h * 6364136223846793005L + 1442695040888963407L;
        h ^= z * 0x165667B19E3779F9L;
        h ^= (h >>> 29);
        h *= 0xBF58476D1CE4E5B9L;
        h ^= (h >>> 32);
        return (h >>> 11) / (double) (1L << 53);
    }
}
