package com.tycoon.world;

import com.badlogic.gdx.math.Vector3;

/**
 * The mutable state of a single coin pickup.
 *
 * <p>Deliberately free of any rendering state so the pickup logic can run in a
 * headless test without an OpenGL context.
 */
public class CoinData {

    public final Vector3 position = new Vector3();
    public double value = 1.0;
    public float spin = 0f;
    public float bob = 0f;

    /** True for the rare, high-value coins; drives the richer pickup sound. */
    public boolean big = false;

    public void respawn(float x, float z, double value, float spin, float bob) {
        position.set(x, CoinField.REST_HEIGHT, z);
        this.value = value;
        this.spin = spin;
        this.bob = bob;
    }
}
