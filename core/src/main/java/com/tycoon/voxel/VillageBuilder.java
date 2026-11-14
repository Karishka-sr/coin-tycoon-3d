package com.tycoon.voxel;

/**
 * Builds the village that stands on the generated terrain: a market square in the
 * middle, the market stalls around it, and cottages scattered over the flat spots.
 *
 * <p>This replaces the plain trees the world used to grow. A village gives the
 * block world something to look at and, more importantly, the stalls are the
 * farms: each one holds a {@link BlockType#STALL} block that pays coins on a
 * timer, so a player who builds a row of them earns while they dig.
 *
 * <p>Deterministic for a given seed, and free of LibGDX, so it can be exercised
 * in a headless test.
 */
public final class VillageBuilder {

    /** Half-extent of the flattened market square, in blocks. */
    public static final int PLAZA_RADIUS = 9;

    /** Footprint of a cottage, outer walls included. */
    public static final int HOUSE_SIZE = 5;

    /** Wall height of a cottage, in blocks, not counting the roof. */
    private static final int HOUSE_WALLS = 3;

    /** Height a house needs cleared above its floor before the roof goes on. */
    private static final int HOUSE_CLEARANCE = 9;

    private final long seed;

    public VillageBuilder(long seed) {
        this.seed = seed;
    }

    public void build(VoxelWorld world, int[][] heights) {
        int centreX = VoxelWorld.SIZE_X / 2;
        int centreZ = VoxelWorld.SIZE_Z / 2;

        int plazaY = buildPlaza(world, heights, centreX, centreZ);
        buildStalls(world, centreX, centreZ, plazaY);
        buildHouses(world, heights, centreX, centreZ);
    }

    // ---- market square -----------------------------------------------------

    /**
     * Levels a square patch to one height and lays a stone floor with plank
     * walkways, so the stalls and the player have somewhere flat to stand.
     *
     * @return the floor height
     */
    private int buildPlaza(VoxelWorld world, int[][] heights, int centreX, int centreZ) {
        int floorY = VoxelWorld.SEA_LEVEL + 1;
        for (int x = centreX - PLAZA_RADIUS; x <= centreX + PLAZA_RADIUS; x++) {
            for (int z = centreZ - PLAZA_RADIUS; z <= centreZ + PLAZA_RADIUS; z++) {
                floorY = Math.max(floorY, heights[x][z]);
            }
        }
        // The square never floats above the terrain's own ceiling.
        floorY = Math.min(floorY, VoxelWorld.SIZE_Y - 20);

        for (int x = centreX - PLAZA_RADIUS; x <= centreX + PLAZA_RADIUS; x++) {
            for (int z = centreZ - PLAZA_RADIUS; z <= centreZ + PLAZA_RADIUS; z++) {
                for (int y = floorY + 1; y < VoxelWorld.SIZE_Y; y++) {
                    world.set(x, y, z, BlockType.AIR);
                }
                for (int y = 1; y < floorY; y++) {
                    BlockType existing = world.get(x, y, z);
                    if (existing.isAir() || existing == BlockType.WATER) {
                        world.set(x, y, z, BlockType.DIRT);
                    }
                }
                world.set(x, floorY, z, BlockType.STONE);
            }
        }

        for (int i = -PLAZA_RADIUS; i <= PLAZA_RADIUS; i++) {
            world.set(centreX + i, floorY, centreZ, BlockType.PLANK);
            world.set(centreX, floorY, centreZ + i, BlockType.PLANK);
        }
        return floorY;
    }

    /** Ring of market stalls on the square, each one facing the middle. */
    private void buildStalls(VoxelWorld world, int centreX, int centreZ, int floorY) {
        int[][] offsets = {
            {-6, -6}, {0, -6}, {6, -6},
            {-6, 0}, {6, 0},
            {-6, 6}, {0, 6}, {6, 6},
        };
        for (int[] offset : offsets) {
            buildStall(world, centreX + offset[0], centreZ + offset[1], floorY);
        }
    }

    /**
     * One stall: a plank counter with corner posts and a golden awning block on
     * top. The awning is the farm the player can mine back out, so the structure
     * is worth exactly one {@link BlockType#STALL} to the economy.
     */
    private void buildStall(VoxelWorld world, int x, int z, int floorY) {
        int left = x - 1;
        int front = z - 1;
        int counterY = floorY + 1;

        for (int dx = 0; dx <= 2; dx++) {
            for (int dz = 0; dz <= 2; dz++) {
                world.set(left + dx, counterY, front + dz, BlockType.PLANK);
            }
        }

        world.set(left, counterY + 1, front, BlockType.WOOD);
        world.set(left + 2, counterY + 1, front, BlockType.WOOD);
        world.set(left, counterY + 1, front + 2, BlockType.WOOD);
        world.set(left + 2, counterY + 1, front + 2, BlockType.WOOD);

        world.set(left + 1, counterY + 1, front + 1, BlockType.STALL);
    }

    // ---- cottages ----------------------------------------------------------

    private void buildHouses(VoxelWorld world, int[][] heights, int centreX, int centreZ) {
        int step = HOUSE_SIZE + 3;
        for (int x = 2; x + HOUSE_SIZE < VoxelWorld.SIZE_X - 2; x += step) {
            for (int z = 2; z + HOUSE_SIZE < VoxelWorld.SIZE_Z - 2; z += step) {
                if (nearPlaza(x, z, centreX, centreZ)) {
                    continue;
                }
                if (hash01(x, 0, z) >= 0.72) {
                    continue;
                }

                int baseY = flatFootprint(heights, x, z);
                if (baseY <= VoxelWorld.SEA_LEVEL) {
                    continue;
                }
                BlockType wall = hash01(x, 1, z) < 0.5 ? BlockType.PLANK : BlockType.BRICK;
                buildHouse(world, x, z, baseY, wall);
            }
        }
    }

    /** A cottage needs a footprint that is flat and clear of the water. */
    private int flatFootprint(int[][] heights, int x, int z) {
        int lowest = Integer.MAX_VALUE;
        int highest = Integer.MIN_VALUE;
        for (int dx = -1; dx <= HOUSE_SIZE; dx++) {
            for (int dz = -1; dz <= HOUSE_SIZE; dz++) {
                int height = heights[x + dx][z + dz];
                lowest = Math.min(lowest, height);
                highest = Math.max(highest, height);
            }
        }
        if (highest - lowest > 1) {
            return -1;
        }
        return highest;
    }

    private boolean nearPlaza(int x, int z, int centreX, int centreZ) {
        int margin = PLAZA_RADIUS + HOUSE_SIZE;
        return Math.abs(x + HOUSE_SIZE / 2 - centreX) < margin
                && Math.abs(z + HOUSE_SIZE / 2 - centreZ) < margin;
    }

    private void buildHouse(VoxelWorld world, int x, int z, int baseY, BlockType wall) {
        int size = HOUSE_SIZE;

        for (int dx = 0; dx < size; dx++) {
            for (int dz = 0; dz < size; dz++) {
                for (int y = baseY + 1; y < baseY + HOUSE_CLEARANCE; y++) {
                    world.set(x + dx, y, z + dz, BlockType.AIR);
                }
                // Fill any dip under the floor so the cottage does not float.
                for (int y = baseY; y >= 1; y--) {
                    BlockType existing = world.get(x + dx, y, z + dz);
                    if (!existing.isAir() && existing != BlockType.WATER) {
                        break;
                    }
                    world.set(x + dx, y, z + dz, BlockType.DIRT);
                }
                world.set(x + dx, baseY, z + dz, BlockType.PLANK);
            }
        }

        for (int dx = 0; dx < size; dx++) {
            for (int dz = 0; dz < size; dz++) {
                boolean edge = dx == 0 || dz == 0 || dx == size - 1 || dz == size - 1;
                if (!edge) {
                    continue;
                }
                boolean corner = (dx == 0 || dx == size - 1) && (dz == 0 || dz == size - 1);
                for (int y = baseY + 1; y <= baseY + HOUSE_WALLS; y++) {
                    world.set(x + dx, y, z + dz, corner ? BlockType.WOOD : wall);
                }
            }
        }

        int doorX = x + size / 2;
        world.set(doorX, baseY + 1, z, BlockType.AIR);
        world.set(doorX, baseY + 2, z, BlockType.AIR);

        addWindows(world, x, z, baseY, size, doorX);
        addRoof(world, x, z, baseY + HOUSE_WALLS + 1, size);
    }

    private void addWindows(VoxelWorld world, int x, int z, int baseY, int size, int doorX) {
        int y = baseY + 2;
        for (int i = 1; i < size - 1; i++) {
            if (x + i != doorX) {
                world.set(x + i, y, z, BlockType.GLASS);
            }
            world.set(x + i, y, z + size - 1, BlockType.GLASS);
            world.set(x, y, z + i, BlockType.GLASS);
            world.set(x + size - 1, y, z + i, BlockType.GLASS);
        }
    }

    /** Stepped pyramid: a full ring, then narrower rings, ending in a single tile. */
    private void addRoof(VoxelWorld world, int x, int z, int roofY, int size) {
        int layers = size / 2 + 1;
        for (int layer = 0; layer < layers; layer++) {
            int low = layer;
            int high = size - 1 - layer;
            for (int dx = low; dx <= high; dx++) {
                for (int dz = low; dz <= high; dz++) {
                    boolean edge = dx == low || dx == high || dz == low || dz == high;
                    boolean apex = layer == layers - 1;
                    if (edge || apex) {
                        world.set(x + dx, roofY + layer, z + dz, BlockType.ROOF);
                    }
                }
            }
        }
    }

    /** Deterministic hash of a coordinate triple into [0, 1). */
    private double hash01(int x, int y, int z) {
        long h = seed + 0x5EEDL;
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
