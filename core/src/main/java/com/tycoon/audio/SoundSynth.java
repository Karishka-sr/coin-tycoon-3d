package com.tycoon.audio;

import com.tycoon.audio.SfxId.Tone;
import com.tycoon.audio.SfxId.Wave;

/**
 * Renders the {@link SfxId} recipes into in-memory WAV files.
 *
 * <p>Pure arithmetic on a byte array: no LibGDX, no OpenGL and no filesystem,
 * so the synthesis can be exercised in a headless unit test. {@link SoundBank}
 * is what turns the bytes into something playable.
 */
public final class SoundSynth {

    public static final int SAMPLE_RATE = 44_100;

    /** Fade applied to each tone so the cuts do not click. */
    private static final double FADE_SECONDS = 0.004;

    private static final int HEADER_BYTES = 44;

    private SoundSynth() {
    }

    /** Renders one effect as a complete, self-contained 16-bit mono WAV file. */
    public static byte[] wav(SfxId id) {
        Tone[] recipe = id.recipe();
        int totalSamples = 0;
        for (Tone tone : recipe) {
            totalSamples += samplesFor(tone.seconds);
        }

        short[] pcm = new short[totalSamples];
        int cursor = 0;
        for (Tone tone : recipe) {
            cursor += render(tone, pcm, cursor);
        }

        return encodeWav(pcm);
    }

    private static int samplesFor(double seconds) {
        return (int) Math.round(seconds * SAMPLE_RATE);
    }

    /**
     * Writes one swept tone into [pcm] starting at [offset].
     *
     * <p>The phase is integrated from the instantaneous frequency rather than
     * computed as `sin(2*pi*f*t)`; a sweep with the latter jumps when the
     * frequency changes and the discontinuity is audible.
     */
    private static int render(Tone tone, short[] pcm, int offset) {
        int count = samplesFor(tone.seconds);
        if (count <= 0) {
            return 0;
        }

        int fade = Math.min(samplesFor(FADE_SECONDS), count / 2);
        double phase = 0.0;

        for (int i = 0; i < count; i++) {
            double progress = (double) i / count;
            double hz = tone.fromHz + (tone.toHz - tone.fromHz) * progress;
            phase += 2.0 * Math.PI * hz / SAMPLE_RATE;

            double sample = tone.wave == Wave.SQUARE
                    ? (Math.sin(phase) >= 0.0 ? 1.0 : -1.0)
                    : Math.sin(phase);

            pcm[offset + i] = toPcm(sample * tone.amplitude * envelope(i, count, fade));
        }
        return count;
    }

    /** Linear fade in/out; 1 in the middle when the tone is long enough. */
    private static double envelope(int index, int count, int fade) {
        if (fade <= 0) {
            return 1.0;
        }
        if (index < fade) {
            return (double) index / fade;
        }
        int fromEnd = count - 1 - index;
        return fromEnd < fade ? (double) fromEnd / fade : 1.0;
    }

    private static short toPcm(double value) {
        double clamped = Math.max(-1.0, Math.min(1.0, value));
        return (short) Math.round(clamped * Short.MAX_VALUE);
    }

    /** Wraps PCM samples in a canonical little-endian WAV container. */
    private static byte[] encodeWav(short[] pcm) {
        int dataBytes = pcm.length * 2;
        byte[] wav = new byte[HEADER_BYTES + dataBytes];

        putAscii(wav, 0, "RIFF");
        putInt(wav, 4, 36 + dataBytes);
        putAscii(wav, 8, "WAVE");
        putAscii(wav, 12, "fmt ");
        putInt(wav, 16, 16);                      // PCM chunk size
        putShort(wav, 20, 1);                     // format: PCM
        putShort(wav, 22, 1);                     // channels: mono
        putInt(wav, 24, SAMPLE_RATE);
        putInt(wav, 28, SAMPLE_RATE * 2);         // byte rate
        putShort(wav, 32, 2);                     // block align
        putShort(wav, 34, 16);                    // bits per sample
        putAscii(wav, 36, "data");
        putInt(wav, 40, dataBytes);

        for (int i = 0; i < pcm.length; i++) {
            putShort(wav, HEADER_BYTES + i * 2, pcm[i]);
        }
        return wav;
    }

    private static void putAscii(byte[] target, int offset, String text) {
        for (int i = 0; i < text.length(); i++) {
            target[offset + i] = (byte) text.charAt(i);
        }
    }

    private static void putInt(byte[] target, int offset, int value) {
        target[offset] = (byte) value;
        target[offset + 1] = (byte) (value >> 8);
        target[offset + 2] = (byte) (value >> 16);
        target[offset + 3] = (byte) (value >> 24);
    }

    private static void putShort(byte[] target, int offset, int value) {
        target[offset] = (byte) value;
        target[offset + 1] = (byte) (value >> 8);
    }
}
