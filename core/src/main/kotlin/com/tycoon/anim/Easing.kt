package com.tycoon.anim

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Easing curves for animating UI and world values.
 *
 * Ported to Kotlin from Roblox's `otter` animation library, `ease.lua`
 * (MIT licence, https://github.com/roblox/otter). The curve maths is kept as
 * it is upstream so the shapes match: Robert Penner's easing family plus the
 * cubic-bezier solver `otter` uses for arbitrary curves.
 *
 * The vendored original lives in `resources/vendor/roblox-otter/ease.lua`.
 */
enum class EasingStyle {
    LINEAR, QUAD, CUBIC, QUART, QUINT, EXPONENTIAL, SINE, BACK, BOUNCE, ELASTIC, CIRCULAR;

    /** Evaluates the curve at [t] in 0..1. */
    fun apply(t: Double): Double = when (this) {
        LINEAR -> t
        QUAD -> t * t
        CUBIC -> t * t * t
        QUART -> t * t * t * t
        QUINT -> t * t * t * t * t
        EXPONENTIAL -> if (t == 0.0) 0.0 else 2.0.pow(10.0 * (t - 1.0))
        SINE -> 1.0 - cos(t * Math.PI / 2.0)
        ELASTIC -> when (t) {
            0.0 -> 0.0
            1.0 -> 1.0
            else -> -2.0.pow(10.0 * t - 10.0) * sin((t * 10.0 - 10.75) * (2.0 * Math.PI / 3.0))
        }
        BACK -> {
            val c1 = 1.70158
            val c3 = c1 + 1.0
            c3 * t * t * t - c1 * t * t
        }
        BOUNCE -> 1.0 - easeOutBounce(1.0 - t)
        CIRCULAR -> -(sqrt(1.0 - t * t) - 1.0)
    }

    private fun easeOutBounce(t: Double): Double {
        val n1 = 7.5625
        val d1 = 2.75
        return when {
            t < 1.0 / d1 -> n1 * t * t
            t < 2.0 / d1 -> n1 * (t - 1.5 / d1) * (t - 1.5 / d1) + 0.75
            t < 2.5 / d1 -> n1 * (t - 2.25 / d1) * (t - 2.25 / d1) + 0.9375
            else -> n1 * (t - 2.625 / d1) * (t - 2.625 / d1) + 0.984375
        }
    }
}

/** A cubic-bezier curve defined by two control points, as used by CSS and `otter`. */
class CubicBezier(private val x1: Double, private val y1: Double, private val x2: Double, private val y2: Double) {

    init {
        require(x1 in 0.0..1.0 && x2 in 0.0..1.0) { "x control points must lie in 0..1" }
    }

    /** Evaluates y at the given x by inverting the x curve with a binary search. */
    fun apply(t: Double): Double {
        if (t <= 0.0) return 0.0
        if (t >= 1.0) return 1.0

        var left = 0.0
        var right = 1.0
        repeat(ITERATIONS) {
            val mid = (left + right) / 2.0
            val x = evaluate(mid, x1, x2)
            if (abs(x - t) < EPSILON) return evaluate(mid, y1, y2)
            if (x < t) left = mid else right = mid
        }
        return evaluate((left + right) / 2.0, y1, y2)
    }

    private fun evaluate(t: Double, p1: Double, p2: Double): Double {
        val oneMinusT = 1.0 - t
        val oneMinusT2 = oneMinusT * oneMinusT
        val t2 = t * t
        return 3.0 * oneMinusT2 * t * p1 + 3.0 * oneMinusT * t2 * p2 + t * t2
    }

    private companion object {
        const val ITERATIONS = 10
        const val EPSILON = 0.0001
    }
}
