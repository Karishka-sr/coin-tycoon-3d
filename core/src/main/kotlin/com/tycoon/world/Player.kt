package com.tycoon.world

import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3

/**
 * The controllable player character. Movement is restricted to the XZ plane;
 * the vertical component is animated as a running bob.
 */
class Player {

    val position = Vector3(0f, 0f, 0f)
    val facing = Vector2(0f, -1f)
    var speed = 10f
    var isMoving = false
        private set

    private var walkPhase = 0f

    /** Bob offset applied to the model on top of [position].y. */
    val bobOffset: Float get() = if (isMoving) MathUtils.sin(walkPhase * 12f) * 0.08f else 0f

    /** Yaw in degrees, used to rotate the model towards the movement direction. */
    val headingDegrees: Float
        get() = MathUtils.atan2(facing.x, facing.y) * MathUtils.radiansToDegrees

    fun update(delta: Float, input: Vector2) {
        isMoving = input.len2() > 0.0001f
        if (isMoving) {
            input.nor()
            facing.set(input)
            position.x += input.x * speed * delta
            position.z += input.y * speed * delta
            walkPhase += delta
        }

        val limit = Arena.HALF - 1.5f
        position.x = MathUtils.clamp(position.x, -limit, limit)
        position.z = MathUtils.clamp(position.z, -limit, limit)
    }

    fun reset() {
        position.set(0f, 0f, 0f)
        facing.set(0f, -1f)
        walkPhase = 0f
        isMoving = false
    }
}
