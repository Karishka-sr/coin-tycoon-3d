package com.tycoon.world

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector3

/**
 * A smooth third-person chase camera that follows the player from above and
 * behind, orbiting with the right mouse button.
 */
class ChaseCamera(private val camera: PerspectiveCamera) {

    var orbit = 0f
    var distance = 18f
    var height = 13f
    var pitch = -42f

    private val desired = Vector3()
    private val target = Vector3()

    fun update(delta: Float, playerPosition: Vector3, playerHeading: Float) {
        if (Gdx.input.isButtonPressed(com.badlogic.gdx.Input.Buttons.RIGHT)) {
            orbit -= Gdx.input.deltaX * 0.4f
        } else {
            orbit = MathUtils.lerpAngle(orbit, playerHeading, Math.min(1f, delta * 1.5f))
        }

        val rad = orbit * MathUtils.degreesToRadians
        val flat = distance * MathUtils.cos(pitch * MathUtils.degreesToRadians)

        desired.set(
            playerPosition.x - MathUtils.sin(rad) * flat,
            height,
            playerPosition.z + MathUtils.cos(rad) * flat,
        )

        camera.position.lerp(desired, Math.min(1f, delta * 5f))
        target.set(playerPosition.x, playerPosition.y + 1.2f, playerPosition.z)
        camera.lookAt(target)
        camera.up.set(Vector3.Y)
        camera.update()
    }
}
