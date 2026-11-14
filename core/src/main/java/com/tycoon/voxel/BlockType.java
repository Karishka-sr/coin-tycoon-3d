package com.tycoon.voxel;

/**
 * The blocks the voxel world is built from.
 *
 * <p>Each type carries the colours used for its top, side and bottom faces (so a
 * grass block gets a green lid and dirt walls without needing textures), plus the
 * two flags the rest of the engine cares about: whether the block hides the face
 * of its neighbour ({@link #isOpaque()}) and whether the player collides with it
 * ({@link #isSolid()}).
 *
 * <p>Mining pays coins, which is what ties this mode back into the tycoon
 * economy: common dirt is worth almost nothing, ore is worth a lot.
 */
public enum BlockType {

    AIR(rgb(0f, 0f, 0f), false, false, 0.0),
    BEDROCK(rgb(0.20f, 0.20f, 0.22f), true, true, 0.0),
    STONE(rgb(0.52f, 0.52f, 0.55f), true, true, 0.5),
    COBBLESTONE(rgb(0.44f, 0.44f, 0.47f), true, true, 0.6),
    DIRT(rgb(0.45f, 0.32f, 0.20f), true, true, 0.4),
    GRASS(rgb(0.30f, 0.60f, 0.24f), rgb(0.44f, 0.32f, 0.20f), rgb(0.45f, 0.32f, 0.20f), true, true, 0.8),
    SAND(rgb(0.85f, 0.79f, 0.55f), true, true, 0.5),
    WOOD(rgb(0.55f, 0.40f, 0.24f), rgb(0.36f, 0.25f, 0.14f), rgb(0.55f, 0.40f, 0.24f), true, true, 2.0),
    PLANK(rgb(0.66f, 0.50f, 0.30f), true, true, 2.0),
    LEAVES(rgb(0.24f, 0.48f, 0.20f), false, true, 0.6),
    GLASS(rgb(0.72f, 0.86f, 0.92f), false, true, 3.0),
    BRICK(rgb(0.62f, 0.28f, 0.22f), true, true, 2.5),
    COAL_ORE(rgb(0.32f, 0.32f, 0.34f), true, true, 6.0),
    IRON_ORE(rgb(0.68f, 0.55f, 0.42f), true, true, 14.0),
    GOLD_ORE(rgb(0.92f, 0.76f, 0.24f), true, true, 30.0),
    DIAMOND_ORE(rgb(0.36f, 0.86f, 0.88f), true, true, 60.0),
    WATER(rgb(0.20f, 0.42f, 0.78f), false, false, 0.0);

    private final float[] top;
    private final float[] side;
    private final float[] bottom;
    private final boolean opaque;
    private final boolean solid;
    private final double coinValue;

    BlockType(float[] colour, boolean opaque, boolean solid, double coinValue) {
        this(colour, colour, colour, opaque, solid, coinValue);
    }

    BlockType(float[] top, float[] side, float[] bottom, boolean opaque, boolean solid, double coinValue) {
        this.top = top;
        this.side = side;
        this.bottom = bottom;
        this.opaque = opaque;
        this.solid = solid;
        this.coinValue = coinValue;
    }

    public static BlockType byId(int id) {
        BlockType[] values = values();
        return id >= 0 && id < values.length ? values[id] : AIR;
    }

    public boolean isAir() {
        return this == AIR;
    }

    /** Whether this block hides the touching face of its neighbour. */
    public boolean isOpaque() {
        return opaque;
    }

    /** Whether the player collides with this block. */
    public boolean isSolid() {
        return solid;
    }

    /** Coins awarded when the block is mined. */
    public double coinValue() {
        return coinValue;
    }

    /** What this block turns into when mined; stone breaks into cobblestone. */
    public BlockType drop() {
        if (this == STONE) {
            return COBBLESTONE;
        }
        if (this == GRASS) {
            return DIRT;
        }
        return this;
    }

    /** Whether the block can be placed from the hotbar. */
    public boolean isPlaceable() {
        return this != AIR && this != WATER && this != BEDROCK;
    }

    /** Colour for one face, before the face shading is applied. */
    public float[] colourFor(int faceIndex) {
        switch (faceIndex) {
            case BlockGeometry.TOP:
                return top;
            case BlockGeometry.BOTTOM:
                return bottom;
            default:
                return side;
        }
    }

    private static float[] rgb(float r, float g, float b) {
        return new float[] {r, g, b};
    }
}
