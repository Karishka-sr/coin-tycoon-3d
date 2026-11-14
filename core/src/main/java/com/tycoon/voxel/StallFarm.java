package com.tycoon.voxel;

import java.util.ArrayList;
import java.util.List;

/**
 * The farm side of the block world: every {@link BlockType#STALL} block is a
 * market stall that pays coins on a timer.
 *
 * <p>Stalls are found by scanning the world once, then kept up to date as the
 * player places and mines them, so the per-frame cost does not depend on the
 * world size. The class owns no rendering and no LibGDX types, so the payout
 * rules can be unit-tested headlessly.
 */
public class StallFarm {

    /** Seconds between two payouts of a single stall. */
    public static final float PAYOUT_INTERVAL = 2.0f;

    /** Coins one stall pays per second, before the passive-income upgrade. */
    public static final double BASE_COINS_PER_SECOND = 1.5;

    private final VoxelWorld world;
    private final List<int[]> stalls = new ArrayList<>();

    private float sinceLastPayout = 0f;

    public StallFarm(VoxelWorld world) {
        this.world = world;
        index();
    }

    /** Records every stall already in the world. */
    public void index() {
        stalls.clear();
        for (int x = 0; x < VoxelWorld.SIZE_X; x++) {
            for (int z = 0; z < VoxelWorld.SIZE_Z; z++) {
                for (int y = 0; y < VoxelWorld.SIZE_Y; y++) {
                    if (world.get(x, y, z) == BlockType.STALL) {
                        stalls.add(new int[] {x, y, z});
                    }
                }
            }
        }
    }

    /** Number of stalls standing, which is the size of the farm. */
    public int stallCount() {
        return stalls.size();
    }

    /**
     * Coins a second the whole farm produces.
     *
     * @param multiplier value of one stall's output, raised by upgrades
     */
    public double coinsPerSecond(double multiplier) {
        return stalls.size() * BASE_COINS_PER_SECOND * Math.max(0.0, multiplier);
    }

    /**
     * Advances the payout clock and returns the coins earned this step.
     *
     * <p>The timer is kept as a remainder so a long frame does not swallow a
     * payout, and several stalls pay together rather than each on its own clock.
     */
    public double update(float delta, double multiplier) {
        double perSecond = coinsPerSecond(multiplier);
        if (perSecond <= 0.0) {
            sinceLastPayout = 0f;
            return 0.0;
        }

        sinceLastPayout += delta;
        if (sinceLastPayout < PAYOUT_INTERVAL) {
            return 0.0;
        }

        int payouts = (int) (sinceLastPayout / PAYOUT_INTERVAL);
        sinceLastPayout -= payouts * PAYOUT_INTERVAL;
        return perSecond * PAYOUT_INTERVAL * payouts;
    }

    /** Registers a newly placed stall; ignored when it is already known. */
    public void onPlaced(int x, int y, int z) {
        for (int[] stall : stalls) {
            if (stall[0] == x && stall[1] == y && stall[2] == z) {
                return;
            }
        }
        stalls.add(new int[] {x, y, z});
    }

    /** Removes a stall that was mined; ignored when it was never registered. */
    public void onRemoved(int x, int y, int z) {
        stalls.removeIf(stall -> stall[0] == x && stall[1] == y && stall[2] == z);
    }
}
