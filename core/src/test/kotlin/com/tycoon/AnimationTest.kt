package com.tycoon

import com.tycoon.anim.CubicBezier
import com.tycoon.anim.EasingStyle
import com.tycoon.anim.Spring
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.abs

/**
 * Checks for the easing and spring maths ported from Roblox's `otter`
 * (https://github.com/roblox/otter, MIT). The curves must start at 0, end at 1
 * and stay monotonic where the upstream curve is, since the HUD relies on that
 * to look like a clean pop rather than a jump.
 */
class EasingTest {

    @Test
    fun `every curve starts at zero and ends at one`() {
        for (style in EasingStyle.entries) {
            assertEquals(0.0, style.apply(0.0), 1e-9, "$style at 0")
            assertEquals(1.0, style.apply(1.0), 1e-9, "$style at 1")
        }
    }

    @Test
    fun `the simple power curves follow their names`() {
        assertEquals(0.25, EasingStyle.QUAD.apply(0.5), 1e-9)
        assertEquals(0.125, EasingStyle.CUBIC.apply(0.5), 1e-9)
        assertEquals(0.0625, EasingStyle.QUART.apply(0.5), 1e-9)
        assertEquals(0.03125, EasingStyle.QUINT.apply(0.5), 1e-9)
    }

    @Test
    fun `curves stay inside zero to one for most of their range`() {
        // Back, elastic and bounce deliberately overshoot; the rest should not.
        val overshooting = setOf(EasingStyle.BACK, EasingStyle.ELASTIC, EasingStyle.BOUNCE)
        for (style in EasingStyle.entries) {
            if (style in overshooting) continue
            for (i in 0..20) {
                val t = i / 20.0
                val v = style.apply(t)
                assertTrue(v >= -1e-9 && v <= 1.0 + 1e-9, "$style at $t gave $v")
            }
        }
    }

    @Test
    fun `back dips below zero before settling`() {
        // otter's back curve is Penner's ease-in-back: it pulls back past the
        // start, which is what gives the pop its wind-up.
        val dip = (1..99).minOf { EasingStyle.BACK.apply(it / 100.0) }
        assertTrue(dip < 0.0, "back should dip below zero, lowest was $dip")
    }

    @Test
    fun `the bounce curve never exceeds one`() {
        for (i in 0..100) {
            assertTrue(EasingStyle.BOUNCE.apply(i / 100.0) <= 1.0 + 1e-9)
        }
    }

    @Test
    fun `cubic bezier reproduces the linear curve`() {
        // Control points on the diagonal give the identity curve.
        val linear = CubicBezier(0.0, 0.0, 1.0, 1.0)
        for (i in 0..10) {
            val t = i / 10.0
            assertEquals(t, linear.apply(t), 1e-3, "bezier at $t")
        }
    }

    @Test
    fun `cubic bezier clamps outside the unit interval`() {
        val curve = CubicBezier(0.42, 0.0, 0.58, 1.0)
        assertEquals(0.0, curve.apply(-0.5), 1e-9)
        assertEquals(1.0, curve.apply(1.5), 1e-9)
    }

    @Test
    fun `cubic bezier is monotonic for a standard ease`() {
        val curve = CubicBezier(0.25, 0.1, 0.25, 1.0)
        var previous = -1.0
        for (i in 0..50) {
            val v = curve.apply(i / 50.0)
            assertTrue(v >= previous - 1e-6, "bezier went backwards at $i")
            previous = v
        }
    }
}

class SpringTest {

    @Test
    fun `the spring reaches its goal and reports completion`() {
        val spring = Spring(dampingRatio = 1.0, frequency = 2.0)
        repeat(600) { spring.update(10.0, 1f / 60f) }
        assertTrue(spring.complete, "spring never settled")
        assertEquals(10.0, spring.value, 1e-3)
        assertEquals(0.0, spring.velocity, 1e-6)
    }

    @Test
    fun `an underdamped spring overshoots the goal`() {
        val spring = Spring(dampingRatio = 0.3, frequency = 2.0)
        var overshot = false
        repeat(600) {
            spring.update(1.0, 1f / 60f)
            if (spring.value > 1.0 + 1e-4) overshot = true
        }
        assertTrue(overshot, "a lightly damped spring should overshoot")
        assertEquals(1.0, spring.value, 1e-2)
    }

    @Test
    fun `an overdamped spring approaches without overshooting`() {
        val spring = Spring(dampingRatio = 3.0, frequency = 2.0)
        repeat(1200) {
            spring.update(1.0, 1f / 60f)
            assertTrue(spring.value <= 1.0 + 1e-6, "overdamped spring overshot to ${spring.value}")
        }
        assertEquals(1.0, spring.value, 1e-2)
    }

    @Test
    fun `a large time step still lands on the goal`() {
        // A stalled frame must not blow the solver up.
        val spring = Spring(dampingRatio = 1.0, frequency = 1.0)
        repeat(30) { spring.update(5.0, 0.5f) }
        assertEquals(5.0, spring.value, 1e-2)
    }

    @Test
    fun `the spring is stable at damping ratios just under one`() {
        // This is the numerically awkward band otter's series expansion guards.
        for (ratio in listOf(0.999, 0.9999, 1.0, 1.0001)) {
            val spring = Spring(dampingRatio = ratio, frequency = 2.0)
            repeat(300) { spring.update(1.0, 1f / 60f) }
            assertTrue(abs(spring.value - 1.0) < 0.05, "ratio $ratio settled at ${spring.value}")
            assertFalse(spring.value.isNaN(), "ratio $ratio produced NaN")
        }
    }

    @Test
    fun `a moving goal is tracked`() {
        val spring = Spring(dampingRatio = 1.0, frequency = 3.0)
        repeat(200) { i -> spring.update((i / 10).toDouble(), 1f / 60f) }
        assertTrue(spring.value > 15.0, "spring fell behind a moving goal: ${spring.value}")
    }
}
