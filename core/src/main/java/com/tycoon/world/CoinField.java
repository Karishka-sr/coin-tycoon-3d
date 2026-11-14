package com.tycoon.world;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.RandomXS128;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;

/**
 * Gameplay logic for the coins lying on the ground: spawning, the magnet pull
 * and collection.
 *
 * <p>This class owns no rendering state, so it can be unit-tested headlessly.
 * The renderer simply reads {@link #coins()} each frame.
 */
public class CoinField {

    /** Coins closer than this to the player are collected. */
    public static final float PICKUP_RADIUS = 0.9f;

    /** Resting height of a coin above the ground plane. */
    public static final float REST_HEIGHT = 0.9f;

    /** Value multiplier applied to the rare "big" coins. */
    public static final double BIG_COIN_MULTIPLIER = 5.0;

    private static final float BOB_AMPLITUDE = 0.12f;
    private static final float SPIN_SPEED = 90f;
    private static final float BOB_SPEED = 3f;

    private final Array<CoinData> coins = new Array<>();
    private final RandomXS128 rng = new RandomXS128(2024L);

    private int capacity = 12;
    private float spawnInterval = 1.0f;
    private float spawnTimer = 0f;
    private double baseValue = 1.0;
    private double bigCoinChance = 0.0;
    private float magnetSpeed = 10f;
    private boolean bigCoinCollected = false;

    public Array<CoinData> coins() {
        return coins;
    }

    /** Whether the most recent {@link #update} collected a big coin. */
    public boolean bigCoinCollected() {
        return bigCoinCollected;
    }

    public int capacity() {
        return capacity;
    }

    public void setSeed(long seed) {
        rng.setSeed(seed);
    }

    /** Applies the values derived from the player's tycoon upgrades. */
    public void configure(int capacity, float spawnInterval, double baseValue) {
        this.capacity = Math.max(1, capacity);
        this.spawnInterval = Math.max(0.05f, spawnInterval);
        this.baseValue = Math.max(0.01, baseValue);
    }

    public void setMagnetSpeed(float magnetSpeed) {
        this.magnetSpeed = Math.max(0f, magnetSpeed);
    }

    public void setBigCoinChance(double bigCoinChance) {
        this.bigCoinChance = MathUtils.clamp(bigCoinChance, 0.0, 1.0);
    }

    /**
     * Advances the field by one step.
     *
     * @return the total value collected during this step (before any value multiplier).
     */
    public double update(float delta, Vector3 playerPosition, float magnetRadius) {
        double collected = 0.0;
        bigCoinCollected = false;
        float magnetSq = magnetRadius * magnetRadius;
        float pickupSq = PICKUP_RADIUS * PICKUP_RADIUS;

        for (int i = coins.size - 1; i >= 0; i--) {
            CoinData coin = coins.get(i);
            coin.spin = (coin.spin + delta * SPIN_SPEED) % 360f;
            coin.bob += delta * BOB_SPEED;
            coin.position.y = REST_HEIGHT + MathUtils.sin(coin.bob) * BOB_AMPLITUDE;

            float dx = playerPosition.x - coin.position.x;
            float dz = playerPosition.z - coin.position.z;
            float distanceSq = dx * dx + dz * dz;

            if (distanceSq < pickupSq) {
                collected += coin.value;
                if (coin.big) {
                    bigCoinCollected = true;
                }
                coins.removeIndex(i);
                continue;
            }

            if (distanceSq < magnetSq) {
                float distance = (float) Math.sqrt(distanceSq);
                float pull = Math.min(1f, magnetSpeed * delta / Math.max(distance, 0.001f));
                coin.position.x += dx * pull;
                coin.position.z += dz * pull;
            }
        }

        spawnTimer += delta;
        while (spawnTimer >= spawnInterval && coins.size < capacity) {
            spawnTimer -= spawnInterval;
            spawnCoin();
        }
        if (coins.size >= capacity) {
            spawnTimer = Math.min(spawnTimer, spawnInterval);
        }

        return collected;
    }

    /** Fills the arena up to capacity, used when the game starts. */
    public void fill() {
        while (coins.size < capacity) {
            spawnCoin();
        }
    }

    public void clear() {
        coins.clear();
        spawnTimer = 0f;
    }

    private void spawnCoin() {
        float limit = Arena.HALF - 3f;
        float x = MathUtils.lerp(-limit, limit, rng.nextFloat());
        float z = MathUtils.lerp(-limit, limit, rng.nextFloat());
        boolean big = rng.nextDouble() < bigCoinChance;
        double value = baseValue * MathUtils.lerp(0.8f, 1.4f, rng.nextFloat());
        if (big) {
            value *= BIG_COIN_MULTIPLIER;
        }
        coins.add(new CoinData());
        coins.peek().respawn(x, z, value, rng.nextFloat() * 360f, rng.nextFloat() * MathUtils.PI2);
        coins.peek().big = big;
    }
}
