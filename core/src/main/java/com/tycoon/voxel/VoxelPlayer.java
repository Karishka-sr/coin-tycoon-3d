package com.tycoon.voxel;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;

/**
 * The player's physics: walking, gravity, jumping and collision against the voxel
 * grid. Deliberately independent of input handling and rendering, so movement can
 * be stepped in a headless test.
 *
 * <p>Collision resolves one axis at a time. Moving on a single axis at a time is
 * what lets the player slide along a wall instead of catching on it, and each
 * axis is advanced in small steps so a fast fall cannot tunnel through a block.
 */
public class VoxelPlayer {

    public static final float WIDTH = 0.6f;
    public static final float HEIGHT = 1.8f;
    public static final float EYE_HEIGHT = 1.62f;

    private static final float GRAVITY = 26f;
    private static final float JUMP_SPEED = 8.6f;
    private static final float TERMINAL_VELOCITY = 40f;
    private static final float MAX_STEP = 0.05f;
    private static final float SKIN = 1e-4f;

    /** Feet position: the bottom centre of the player box. */
    public final Vector3 position = new Vector3();
    public final Vector3 velocity = new Vector3();

    public float yaw = 0f;
    public float pitch = 0f;

    public boolean onGround = false;
    public boolean flying = false;

    public VoxelPlayer() {
    }

    /** Puts the player above the terrain surface at the given column. */
    public void spawn(VoxelWorld world, float x, float z) {
        int bx = MathUtils.clamp((int) Math.floor(x), 0, VoxelWorld.SIZE_X - 1);
        int bz = MathUtils.clamp((int) Math.floor(z), 0, VoxelWorld.SIZE_Z - 1);
        int surface = world.highestSolidY(bx, bz);
        position.set(x, Math.max(0, surface + 1) + SKIN, z);
        velocity.setZero();
        onGround = false;
    }

    /**
     * Applies a step of movement.
     *
     * @param moveX desired horizontal movement, world space
     * @param moveZ desired horizontal movement, world space
     * @param vertical +1 to ascend, -1 to descend, 0 otherwise (only when flying)
     */
    public void update(VoxelWorld world, float delta, float moveX, float moveZ, float vertical) {
        if (flying) {
            moveAxis(world, 0, moveX);
            moveAxis(world, 2, moveZ);
            moveAxis(world, 1, vertical * FLY_SPEED * delta);
            velocity.setZero();
            onGround = false;
            return;
        }

        velocity.y -= GRAVITY * delta;
        velocity.y = Math.max(velocity.y, -TERMINAL_VELOCITY);

        moveAxis(world, 0, moveX);
        moveAxis(world, 2, moveZ);

        boolean blockedDown = moveAxis(world, 1, velocity.y * delta);
        if (blockedDown) {
            onGround = velocity.y <= 0f;
            velocity.y = 0f;
        } else if (velocity.y < 0f) {
            onGround = false;
        }
    }

    /** Jumps when standing on something; does nothing in the air. */
    public void jump() {
        if (onGround && !flying) {
            velocity.y = JUMP_SPEED;
            onGround = false;
        }
    }

    /**
     * Moves along one axis in small steps, stopping at the first block it hits.
     *
     * @return true when the movement was blocked
     */
    private boolean moveAxis(VoxelWorld world, int axis, float amount) {
        if (Math.abs(amount) < 1e-6f) {
            return false;
        }

        float remaining = amount;
        float sign = Math.signum(amount);

        while (Math.abs(remaining) > 1e-6f) {
            float step = sign * Math.min(Math.abs(remaining), MAX_STEP);
            add(axis, step);
            if (collides(world)) {
                add(axis, -step);
                return true;
            }
            remaining -= step;
        }
        return false;
    }

    private void add(int axis, float value) {
        switch (axis) {
            case 0:
                position.x += value;
                break;
            case 1:
                position.y += value;
                break;
            default:
                position.z += value;
                break;
        }
    }

    /** Whether the player box currently overlaps a solid block. */
    public boolean collides(VoxelWorld world) {
        float half = WIDTH / 2f;
        return world.intersectsSolid(
                position.x - half, position.y + SKIN, position.z - half,
                position.x + half, position.y + HEIGHT - SKIN, position.z + half);
    }

    /** Eye position, where the camera and the block ray start. */
    public Vector3 eye(Vector3 out) {
        return out.set(position.x, position.y + EYE_HEIGHT, position.z);
    }

    /** Unit look direction from the current yaw and pitch. */
    public Vector3 look(Vector3 out) {
        float yawRad = yaw * MathUtils.degreesToRadians;
        float pitchRad = pitch * MathUtils.degreesToRadians;
        float cosPitch = MathUtils.cos(pitchRad);
        return out.set(
                -MathUtils.sin(yawRad) * cosPitch,
                MathUtils.sin(pitchRad),
                -MathUtils.cos(yawRad) * cosPitch);
    }

    public static final float FLY_SPEED = 14f;
}
