package com.tycoon.world

import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector3

/**
 * A first-person camera. It sits at eye height above the player and looks along
 * [yaw] and [pitch], which are driven by drag input.
 *
 * Yaw 0 looks towards -Z, increasing yaw turns left, increasing pitch looks up.
 */
class FirstPersonCamera(private val camera: PerspectiveCamera) {

    var yaw = 0f
        private set
    var pitch = 0f
        private set
    var eyeHeight = 1.7f
    var sensitivity = 0.18f
    var invertY = false

    private val direction = Vector3()

    /** Applies a drag of [dx] / [dy] screen pixels. */
    fun rotate(dx: Float, dy: Float) {
        yaw = wrap(yaw - dx * sensitivity)
        val vertical = if (invertY) dy else -dy
        pitch = MathUtils.clamp(pitch + vertical * sensitivity, -PITCH_LIMIT, PITCH_LIMIT)
    }

    /** Turns the camera by [degrees] without changing the pitch. */
    fun turn(degrees: Float) {
        yaw = wrap(yaw + degrees)
    }

    /**
     * Places the camera at the player's eye and aims it along the current
     * angles. [bob] adds a small vertical offset while walking.
     */
    fun update(playerPosition: Vector3, bob: Float) {
        val yawRad = yaw * MathUtils.degreesToRadians
        val pitchRad = pitch * MathUtils.degreesToRadians
        val cosPitch = MathUtils.cos(pitchRad)

        direction.set(
            -MathUtils.sin(yawRad) * cosPitch,
            MathUtils.sin(pitchRad),
            -MathUtils.cos(yawRad) * cosPitch,
        )

        camera.position.set(playerPosition.x, playerPosition.y + eyeHeight + bob, playerPosition.z)
        camera.direction.set(direction)
        camera.up.set(Vector3.Y)
        camera.update()
    }

    private fun wrap(degrees: Float): Float = ((degrees % 360f) + 360f) % 360f

    companion object {
        const val PITCH_LIMIT = 88f
    }
}
