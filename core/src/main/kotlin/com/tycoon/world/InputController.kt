package com.tycoon.world

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.math.Vector2

/**
 * Maps keyboard, mouse and touch input to a normalised movement vector.
 *
 * <p>The same vector drives the player on desktop (WASD / arrows / right-mouse
 * drag) and on touch devices (drag anywhere on screen).
 */
class InputController {

    private val move = Vector2()
    private var touchOrigin: Vector2? = null

    fun movement(): Vector2 {
        move.setZero()

        if (Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP)) move.y += 1f
        if (Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN)) move.y -= 1f
        if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT)) move.x -= 1f
        if (Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT)) move.x += 1f

        if (move.isZero && Gdx.input.isTouched) {
            val x = Gdx.input.x
            val y = Gdx.input.y
            val origin = touchOrigin
            if (origin == null) {
                touchOrigin = Vector2(x.toFloat(), y.toFloat())
            } else {
                move.set(x - origin.x, -(y - origin.y)).scl(0.06f)
            }
        } else if (!Gdx.input.isTouched) {
            touchOrigin = null
        }

        return move
    }
}
