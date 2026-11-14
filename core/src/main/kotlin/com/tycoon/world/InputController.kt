package com.tycoon.world

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.math.Vector2

/**
 * Merges keyboard movement with the on-screen stick into a single normalised
 * vector, and reports keyboard camera turning.
 *
 * <p>The returned movement has x = strafe right and y = forward, both relative
 * to where the camera looks; [InputMath] turns that into a world direction.
 */
class InputController {

    private val move = Vector2()

    fun movement(stick: Vector2): Vector2 {
        move.setZero()

        if (key(Input.Keys.W) || key(Input.Keys.UP)) move.y += 1f
        if (key(Input.Keys.S) || key(Input.Keys.DOWN)) move.y -= 1f
        if (key(Input.Keys.A)) move.x -= 1f
        if (key(Input.Keys.D)) move.x += 1f

        move.add(stick)
        if (move.len2() > 1f) move.nor()
        return move
    }

    /** Degrees to turn the camera this frame from the arrow / QE keys. */
    fun turn(delta: Float): Float {
        var direction = 0f
        if (key(Input.Keys.LEFT) || key(Input.Keys.Q)) direction += 1f
        if (key(Input.Keys.RIGHT) || key(Input.Keys.E)) direction -= 1f
        return direction * TURN_SPEED * delta
    }

    private fun key(code: Int): Boolean = Gdx.input.isKeyPressed(code)

    companion object {
        const val TURN_SPEED = 140f
    }
}
