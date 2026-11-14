package com.tycoon.world

import com.badlogic.gdx.InputAdapter

/**
 * Turns drags that start outside the movement stick into yaw / pitch changes.
 *
 * <p>The joystick lives in the left half of the screen and consumes its own
 * touches, so everything that reaches this processor is a look gesture.
 */
class LookInput(private val camera: FirstPersonCamera) : InputAdapter() {

    private var pointer = -1
    private var lastX = 0f
    private var lastY = 0f

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        if (this.pointer != -1) return false
        this.pointer = pointer
        lastX = screenX.toFloat()
        lastY = screenY.toFloat()
        return true
    }

    override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
        if (pointer != this.pointer) return false
        camera.rotate(screenX - lastX, screenY - lastY)
        lastX = screenX.toFloat()
        lastY = screenY.toFloat()
        return true
    }

    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        if (pointer == this.pointer) this.pointer = -1
        return true
    }
}
