package com.tycoon.audio;

/**
 * The sound effects the game can play, with the mixing parameters each one is
 * synthesised and replayed with.
 *
 * <p>Every effect is built from a small recipe instead of a recorded file, so
 * the game ships without audio assets.
 */
public enum SfxId {

    /** Quick blip when a normal coin is picked up. */
    COIN_PICKUP(0.55f, 1.0f, new Tone(880.0, 1320.0, 0.10, 0.55, Wave.SINE)),

    /** Richer, higher arpeggio for the rare big coins. */
    BIG_COIN(0.70f, 1.0f,
            new Tone(660.0, 990.0, 0.09, 0.45, Wave.SINE),
            new Tone(990.0, 1480.0, 0.16, 0.50, Wave.SINE)),

    /** Rising two-note confirmation for a successful purchase. */
    PURCHASE(0.60f, 1.0f,
            new Tone(523.25, 523.25, 0.10, 0.50, Wave.SQUARE),
            new Tone(783.99, 783.99, 0.18, 0.45, Wave.SQUARE)),

    /** Short, dull double thud when a purchase is refused. */
    DENIED(0.50f, 1.0f,
            new Tone(200.0, 140.0, 0.11, 0.60, Wave.SQUARE),
            new Tone(150.0, 110.0, 0.12, 0.40, Wave.SQUARE)),

    /** Soft tick under the UI buttons. */
    UI_CLICK(0.40f, 1.0f, new Tone(600.0, 700.0, 0.045, 0.70, Wave.SINE)),

    /** Dull thud when a block is broken in the voxel world. */
    BLOCK_DIG(0.55f, 1.0f,
            new Tone(220.0, 90.0, 0.10, 0.55, Wave.SQUARE),
            new Tone(160.0, 70.0, 0.07, 0.35, Wave.SQUARE)),

    /** Short clack when a block is placed. */
    BLOCK_PLACE(0.45f, 1.0f, new Tone(420.0, 300.0, 0.06, 0.60, Wave.SQUARE));

    /** Base gain in 0..1, before the player's volume setting is applied. */
    public final float gain;

    /** Playback speed multiplier; above 1 sounds higher and shorter. */
    public final float pitch;

    private final Tone[] recipe;

    SfxId(float gain, float pitch, Tone... recipe) {
        this.gain = gain;
        this.pitch = pitch;
        this.recipe = recipe;
    }

    Tone[] recipe() {
        return recipe;
    }

    /** A single swept oscillator: pitch glides from [fromHz] to [toHz] over [seconds]. */
    public static final class Tone {

        public final double fromHz;
        public final double toHz;
        public final double seconds;
        public final double amplitude;
        public final Wave wave;

        public Tone(double fromHz, double toHz, double seconds, double amplitude, Wave wave) {
            this.fromHz = fromHz;
            this.toHz = toHz;
            this.seconds = seconds;
            this.amplitude = amplitude;
            this.wave = wave;
        }
    }

    /** Oscillator shapes; deliberately cheap so no lookup tables are needed. */
    public enum Wave {
        SINE,
        SQUARE
    }
}
