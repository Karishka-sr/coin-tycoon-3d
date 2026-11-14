package com.tycoon.world

import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3

/**
 * The controllable player. Movement is restricted to the XZ plane; the
 * vertical component is exposed as a running bob for the camera.
 */
class Player {

    val position = Vector3(0f, 0f, 0f)
    var speed = 10f
    var isMoving = false
        private set

    private var walkPhase = 0f

    /** Bob offset applied to the camera while walking. */
    val bobOffset: Float get() = if (isMoving) MathUtils.sin(walkPhase * 12f) * 0.06f else 0f

    /** [movement] is a world-space direction on the ground plane. */
    fun update(delta: Float, movement: Vector2) {
        isMoving = movement.len2() > 0.0001f
        if (isMoving) {
            position.x += movement.x * speed * delta
            position.z += movement.y * speed * delta
            walkPhase += delta
        }

        val limit = Arena.HALF - 1.5f
        position.x = MathUtils.clamp(position.x, -limit, limit)
        position.z = MathUtils.clamp(position.z, -limit, limit)
    }

    fun reset() {
        position.set(0f, 0f, 0f)
        walkPhase = 0f
        isMoving = false
    }
}
