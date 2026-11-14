package com.tycoon.world

import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import kotlin.math.sqrt

/**
 * Converts a stick input into a world-space direction on the ground plane.
 *
 * <p>Pure maths with no LibGDX input dependency, so it can be unit-tested
 * headlessly. Forward is the direction the camera looks, strafe is to its
 * right; the result is normalised when the stick is pushed past the unit
 * circle, which keeps diagonal movement from being faster.
 */
object InputMath {

    /**
     * @param inputX strafe, +1 is right
     * @param inputY forward, +1 is ahead
     * @param yawDegrees camera yaw, 0 looks towards -Z
     */
    fun worldMove(inputX: Float, inputY: Float, yawDegrees: Float, out: Vector2): Vector2 {
        val yaw = yawDegrees * MathUtils.degreesToRadians
        val sin = MathUtils.sin(yaw)
        val cos = MathUtils.cos(yaw)

        val x = -sin * inputY + cos * inputX
        val z = -cos * inputY - sin * inputX

        out.set(x, z)
        val len2 = out.len2()
        if (len2 > 1f) out.scl(1f / sqrt(len2))
        return out
    }
}
