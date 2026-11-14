package com.tycoon.voxel;

/**
 * Walks a ray through the block grid, the classic Amanatides & Woo voxel
 * traversal. The result is the first solid block the ray enters plus the face it
 * entered through, which is exactly what the player needs to mine a block and to
 * place a new one against it.
 */
public final class VoxelRaycast {

    /** Nothing within range was hit. */
    public static final int NO_FACE = -1;

    public int blockX;
    public int blockY;
    public int blockZ;
    public int face = NO_FACE;
    public boolean hit;

    /** Distance along the ray to the hit, in blocks. */
    public float distance;

    /**
     * Casts from {@code origin} along {@code direction}.
     *
     * @return true when a solid block was hit, in which case the public fields
     *         describe it.
     */
    public boolean cast(VoxelWorld world, float ox, float oy, float oz, float dx, float dy, float dz, float maxDistance) {
        hit = false;
        face = NO_FACE;
        distance = maxDistance;

        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1e-6f) {
            return false;
        }
        dx /= len;
        dy /= len;
        dz /= len;

        int x = (int) Math.floor(ox);
        int y = (int) Math.floor(oy);
        int z = (int) Math.floor(oz);

        int stepX = dx > 0 ? 1 : -1;
        int stepY = dy > 0 ? 1 : -1;
        int stepZ = dz > 0 ? 1 : -1;

        float tDeltaX = dx == 0f ? Float.POSITIVE_INFINITY : Math.abs(1f / dx);
        float tDeltaY = dy == 0f ? Float.POSITIVE_INFINITY : Math.abs(1f / dy);
        float tDeltaZ = dz == 0f ? Float.POSITIVE_INFINITY : Math.abs(1f / dz);

        float tMaxX = dx == 0f ? Float.POSITIVE_INFINITY : ((dx > 0 ? x + 1 - ox : ox - x) * tDeltaX);
        float tMaxY = dy == 0f ? Float.POSITIVE_INFINITY : ((dy > 0 ? y + 1 - oy : oy - y) * tDeltaY);
        float tMaxZ = dz == 0f ? Float.POSITIVE_INFINITY : ((dz > 0 ? z + 1 - oz : oz - z) * tDeltaZ);

        // The starting cell is tested first: the eye normally sits in air, so this
        // only matters when it ends up inside a block, where the hit has no face.
        int enteredFace = NO_FACE;
        float travelled = 0f;

        while (true) {
            if (world.isSolid(x, y, z) && y >= 0) {
                blockX = x;
                blockY = y;
                blockZ = z;
                face = enteredFace;
                distance = travelled;
                hit = true;
                return true;
            }

            if (tMaxX < tMaxY) {
                if (tMaxX < tMaxZ) {
                    if (tMaxX > maxDistance) {
                        break;
                    }
                    x += stepX;
                    enteredFace = stepX > 0 ? BlockGeometry.NEG_X : BlockGeometry.POS_X;
                    travelled = tMaxX;
                    tMaxX += tDeltaX;
                } else {
                    if (tMaxZ > maxDistance) {
                        break;
                    }
                    z += stepZ;
                    enteredFace = stepZ > 0 ? BlockGeometry.NEG_Z : BlockGeometry.POS_Z;
                    travelled = tMaxZ;
                    tMaxZ += tDeltaZ;
                }
            } else {
                if (tMaxY < tMaxZ) {
                    if (tMaxY > maxDistance) {
                        break;
                    }
                    y += stepY;
                    enteredFace = stepY > 0 ? BlockGeometry.NEG_Y : BlockGeometry.POS_Y;
                    travelled = tMaxY;
                    tMaxY += tDeltaY;
                } else {
                    if (tMaxZ > maxDistance) {
                        break;
                    }
                    z += stepZ;
                    enteredFace = stepZ > 0 ? BlockGeometry.NEG_Z : BlockGeometry.POS_Z;
                    travelled = tMaxZ;
                    tMaxZ += tDeltaZ;
                }
            }
        }

        face = NO_FACE;
        return false;
    }

    /** The empty block the ray entered just before the hit: where a new block goes. */
    public int placementX() {
        return blockX + normalX(face);
    }

    public int placementY() {
        return blockY + normalY(face);
    }

    public int placementZ() {
        return blockZ + normalZ(face);
    }

    public static int normalX(int face) {
        return face == BlockGeometry.POS_X ? 1 : face == BlockGeometry.NEG_X ? -1 : 0;
    }

    public static int normalY(int face) {
        return face == BlockGeometry.POS_Y ? 1 : face == BlockGeometry.NEG_Y ? -1 : 0;
    }

    public static int normalZ(int face) {
        return face == BlockGeometry.POS_Z ? 1 : face == BlockGeometry.NEG_Z ? -1 : 0;
    }
}
