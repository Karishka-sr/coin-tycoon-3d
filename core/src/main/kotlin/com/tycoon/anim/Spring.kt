package com.tycoon.anim

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * A damped spring that eases a value towards a goal, used for the "juice" on
 * coins and HUD numbers.
 *
 * Ported to Kotlin from Roblox's `otter` animation library, `spring.lua`
 * (MIT licence, https://github.com/roblox/otter). The solver is the same
 * analytical solution of the damped harmonic oscillator, including the
 * series expansion `otter` uses to stay stable for damping ratios near 1.
 *
 * The vendored original lives in `resources/vendor/roblox-otter/spring.lua`.
 */
class Spring(
    /** Damping ratio: 1 is critically damped, below 1 overshoots, above 1 crawls. */
    private val dampingRatio: Double = 1.0,
    /** Frequency in hertz. */
    private val frequency: Double = 1.0,
    private val restingVelocityLimit: Double = DEFAULT_RESTING_VELOCITY_LIMIT,
    private val restingPositionLimit: Double = DEFAULT_RESTING_POSITION_LIMIT,
    start: Double = 0.0,
) {

    var value: Double = start
        private set

    var velocity: Double = 0.0
        private set

    /** True once the spring has settled on its goal. */
    var complete: Boolean = false
        private set

    /**
     * Advances the spring towards [goal] by [dt] seconds and returns the new
     * value. Call once per frame.
     */
    fun update(goal: Double, dt: Float): Double = update(goal, dt.toDouble())

    fun update(goal: Double, dt: Double): Double {
        val d = dampingRatio
        val f = frequency * 2.0 * Math.PI // rad/s

        val p0 = value
        val v0 = velocity
        val offset = p0 - goal
        val decay = exp(-dt * d * f)

        val p1: Double
        val v1: Double
        when {
            d == 1.0 -> { // critically damped
                p1 = (v0 * dt + offset * (f * dt + 1.0)) * decay + goal
                v1 = (v0 - f * dt * (offset * f + v0)) * decay
            }

            d < 1.0 -> { // underdamped
                val c = sqrt(1.0 - d * d)
                val i = cos(f * c * dt)
                val j = sin(f * c * dt)

                // sin(dt*f*c)/c loses precision as c approaches 0, so fall back
                // to the 5th-order series expansion there (as otter does).
                val z = if (c > SERIES_THRESHOLD) {
                    j / c
                } else {
                    val a = dt * f
                    a + ((a * a) * (c * c) * (c * c) / 20.0 - c * c) * (a * a * a) / 6.0
                }

                val y = if (f * c > SERIES_THRESHOLD) {
                    j / (f * c)
                } else {
                    val b = f * c
                    dt + ((dt * dt) * (b * b) * (b * b) / 20.0 - b * b) * (dt * dt * dt) / 6.0
                }

                p1 = (offset * (i + d * z) + v0 * y) * decay + goal
                v1 = (v0 * (i - z * d) - offset * (z * f)) * decay
            }

            else -> { // overdamped
                val c = sqrt(d * d - 1.0)
                val r1 = -f * (d - c)
                val r2 = -f * (d + c)
                val co2 = (v0 - r1 * offset) / (2.0 * f * c)
                val co1 = offset - co2
                val e1 = co1 * exp(r1 * dt)
                val e2 = co2 * exp(r2 * dt)
                p1 = e1 + e2 + goal
                v1 = r1 * e1 + r2 * e2
            }
        }

        complete = abs(v1) < restingVelocityLimit && abs(p1 - goal) < restingPositionLimit
        if (complete) {
            value = goal
            velocity = 0.0
        } else {
            value = p1
            velocity = v1
        }
        return value
    }

    companion object {
        private const val DEFAULT_RESTING_VELOCITY_LIMIT = 1e-3
        private const val DEFAULT_RESTING_POSITION_LIMIT = 1e-2
        private const val SERIES_THRESHOLD = 1e-4
    }
}
