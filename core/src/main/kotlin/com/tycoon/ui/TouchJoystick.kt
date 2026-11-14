package com.tycoon.ui

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.scenes.scene2d.ui.Widget
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import kotlin.math.sqrt

/**
 * A floating on-screen stick. Touching anywhere inside the actor drops the base
 * under the finger, so the thumb never has to find a fixed control.
 */
class TouchJoystick(
    private val baseDrawable: Drawable,
    private val knobDrawable: Drawable,
) : Widget() {

    /** Normalised stick position: x strafes right, y moves forward. */
    val output = Vector2()

    private var pointer = -1
    private val base = Vector2()
    private val knob = Vector2()
    private var radius = 120f

    init {
        addListener(object : InputListener() {
            override fun touchDown(event: InputEvent, x: Float, y: Float, pointer: Int, button: Int): Boolean {
                if (this@TouchJoystick.pointer != -1) return false
                this@TouchJoystick.pointer = pointer
                base.set(
                    MathUtils.clamp(x, radius, width - radius),
                    MathUtils.clamp(y, radius, height - radius),
                )
                apply(x, y)
                return true
            }

            override fun touchDragged(event: InputEvent, x: Float, y: Float, pointer: Int) {
                if (pointer == this@TouchJoystick.pointer) apply(x, y)
            }

            override fun touchUp(event: InputEvent, x: Float, y: Float, pointer: Int, button: Int) {
                if (pointer == this@TouchJoystick.pointer) {
                    this@TouchJoystick.pointer = -1
                    output.setZero()
                    resetBase()
                }
            }
        })
    }

    override fun sizeChanged() {
        resetBase()
    }

    private fun resetBase() {
        radius = MathUtils.clamp(Math.min(width, height) * 0.20f, 70f, 240f)
        base.set(radius + 28f, radius + 28f)
        knob.set(base)
    }

    private fun apply(x: Float, y: Float) {
        var dx = x - base.x
        var dy = y - base.y
        val length = sqrt(dx * dx + dy * dy)
        if (length > radius) {
            dx = dx / length * radius
            dy = dy / length * radius
        }
        knob.set(base.x + dx, base.y + dy)
        output.set(dx / radius, dy / radius)
    }

    override fun draw(batch: Batch, parentAlpha: Float) {
        val diameter = radius * 2f
        baseDrawable.draw(batch, x + base.x - radius, y + base.y - radius, diameter, diameter)

        val knobRadius = radius * 0.5f
        knobDrawable.draw(
            batch,
            x + knob.x - knobRadius,
            y + knob.y - knobRadius,
            knobRadius * 2f,
            knobRadius * 2f,
        )
    }
}
