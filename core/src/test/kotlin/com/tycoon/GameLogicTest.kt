package com.tycoon

import com.badlogic.gdx.math.Vector3
import com.tycoon.tycoon.Settings
import com.tycoon.tycoon.TycoonState
import com.tycoon.tycoon.Upgrade
import com.tycoon.world.CoinField
import com.tycoon.world.Player
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Headless checks for the economy and the coin loop; no OpenGL context needed. */
class TycoonStateTest {

    @Test
    fun `upgrade prices grow and purchases deduct coins`() {
        val state = TycoonState()
        state.addCoins(1_000.0)

        val firstCost = state.costOf(Upgrade.COIN_VALUE)
        assertTrue(state.buy(Upgrade.COIN_VALUE))
        assertEquals(1, state.levelOf(Upgrade.COIN_VALUE))
        assertTrue(state.costOf(Upgrade.COIN_VALUE) > firstCost)
        assertEquals(1_000.0 - firstCost, state.coins, 1e-6)
    }

    @Test
    fun `purchase is rejected without enough coins`() {
        val state = TycoonState()
        assertFalse(state.buy(Upgrade.MAGNET))
        assertEquals(0, state.levelOf(Upgrade.MAGNET))
    }

    @Test
    fun `passive income accumulates over time`() {
        val state = TycoonState()
        state.addCoins(1_000.0)
        state.buy(Upgrade.PASSIVE_INCOME)

        val before = state.coins
        state.update(2f)
        assertEquals(before + 2.0, state.coins, 1e-6)
    }

    @Test
    fun `coin value upgrade raises the per-coin payout`() {
        val state = TycoonState()
        val base = state.coinValue
        state.addCoins(1_000.0)
        state.buy(Upgrade.COIN_VALUE)
        assertEquals(base * 1.25, state.coinValue, 1e-6)
    }

    @Test
    fun `upgrades widen the magnet and raise the arena capacity`() {
        val state = TycoonState()
        val radiusBefore = state.magnetRadius
        val capacityBefore = state.capacity

        state.addCoins(10_000.0)
        state.buy(Upgrade.MAGNET)
        state.buy(Upgrade.CAPACITY)

        assertTrue(state.magnetRadius > radiusBefore)
        assertTrue(state.capacity > capacityBefore)
    }
}

class CoinFieldTest {

    private val origin = Vector3(0f, 0f, 0f)

    @Test
    fun `field fills up to capacity and never exceeds it`() {
        val field = CoinField()
        field.configure(10, 0.5f, 1.0)
        field.fill()
        assertEquals(10, field.coins().size)

        repeat(100) { field.update(0.1f, origin, 0f) }
        assertEquals(10, field.coins().size)
    }

    @Test
    fun `coins inside the pickup radius are collected`() {
        val field = CoinField()
        field.configure(1, 100f, 1.0)
        field.fill()

        val coin = field.coins().first()
        coin.position.set(0.2f, CoinField.REST_HEIGHT, 0.2f)

        val collected = field.update(0.016f, origin, 0f)
        assertTrue(collected > 0.0)
        assertEquals(0, field.coins().size)
    }

    @Test
    fun `magnet pulls distant coins towards the player`() {
        val field = CoinField()
        field.configure(1, 100f, 1.0)
        field.fill()
        field.setMagnetSpeed(30f)

        val coin = field.coins().first()
        coin.position.set(5f, CoinField.REST_HEIGHT, 0f)
        val distanceBefore = coin.position.dst(origin)

        field.update(0.05f, origin, 8f)
        assertTrue(coin.position.dst(origin) < distanceBefore)
    }

    @Test
    fun `spawn rate upgrade produces more coins over the same window`() {
        val slow = CoinField().apply { configure(100, 1.0f, 1.0) }
        val fast = CoinField().apply { configure(100, 0.2f, 1.0) }
        slow.fill(); slow.clear(); fast.fill(); fast.clear()

        repeat(100) {
            slow.update(0.05f, origin, 0f)
            fast.update(0.05f, origin, 0f)
        }
        assertTrue(fast.coins().size > slow.coins().size)
    }

    @Test
    fun `player stays inside the arena bounds`() {
        val player = Player()
        repeat(200) { player.update(0.1f, com.badlogic.gdx.math.Vector2(1f, 1f)) }
        assertTrue(player.position.x <= com.tycoon.world.Arena.HALF)
        assertTrue(player.position.z <= com.tycoon.world.Arena.HALF)
    }
}

/** Camera-relative movement maths; pure functions, no GL context required. */
class InputMathTest {

    private val out = com.badlogic.gdx.math.Vector2()

    @Test
    fun `forward at yaw zero moves towards negative Z`() {
        com.tycoon.world.InputMath.worldMove(0f, 1f, 0f, out)
        assertEquals(0f, out.x, 1e-5f)
        assertEquals(-1f, out.y, 1e-5f)
    }

    @Test
    fun `strafe right at yaw zero moves towards positive X`() {
        com.tycoon.world.InputMath.worldMove(1f, 0f, 0f, out)
        assertEquals(1f, out.x, 1e-5f)
        assertEquals(0f, out.y, 1e-5f)
    }

    @Test
    fun `forward follows the yaw angle`() {
        // Yaw 90 looks along -X.
        com.tycoon.world.InputMath.worldMove(0f, 1f, 90f, out)
        assertEquals(-1f, out.x, 1e-5f)
        assertEquals(0f, out.y, 1e-5f)
    }

    @Test
    fun `diagonal input is normalised so it is not faster`() {
        com.tycoon.world.InputMath.worldMove(1f, 1f, 0f, out)
        assertEquals(1f, out.len(), 1e-5f)
    }

    @Test
    fun `partial stick input keeps its magnitude`() {
        com.tycoon.world.InputMath.worldMove(0f, 0.5f, 0f, out)
        assertEquals(0.5f, out.len(), 1e-5f)
    }
}

/** First-person camera angles; only arithmetic, so it runs headlessly. */
class FirstPersonCameraTest {

    private val camera = com.badlogic.gdx.graphics.PerspectiveCamera(67f, 800f, 480f)
    private val firstPerson = com.tycoon.world.FirstPersonCamera(camera)

    companion object {
        init {
            // PerspectiveCamera builds its frustum through JNI; load the natives once.
            com.badlogic.gdx.utils.GdxNativesLoader.load()
        }
    }

    @Test
    fun `yaw turns and wraps around`() {
        firstPerson.rotate(-100f, 0f)
        assertTrue(firstPerson.yaw in 0f..360f)
    }

    @Test
    fun `pitch is clamped to the limits`() {
        firstPerson.rotate(0f, -10_000f)
        assertEquals(com.tycoon.world.FirstPersonCamera.PITCH_LIMIT, firstPerson.pitch, 1e-4f)
        firstPerson.rotate(0f, 10_000f)
        assertEquals(-com.tycoon.world.FirstPersonCamera.PITCH_LIMIT, firstPerson.pitch, 1e-4f)
    }

    @Test
    fun `camera sits at eye height above the player`() {
        val position = Vector3(3f, 0f, -4f)
        firstPerson.update(position, 0f)
        assertEquals(3f, camera.position.x, 1e-5f)
        assertEquals(firstPerson.eyeHeight, camera.position.y, 1e-5f)
        assertEquals(-4f, camera.position.z, 1e-5f)
    }

    @Test
    fun `dragging up looks up unless the vertical axis is inverted`() {
        val normal = com.tycoon.world.FirstPersonCamera(camera)
        normal.rotate(0f, -50f)
        assertTrue(normal.pitch > 0f)

        val inverted = com.tycoon.world.FirstPersonCamera(camera).apply { invertY = true }
        inverted.rotate(0f, -50f)
        assertTrue(inverted.pitch < 0f)
    }

    @Test
    fun `higher sensitivity turns further for the same drag`() {
        val slow = com.tycoon.world.FirstPersonCamera(camera).apply { sensitivity = 0.1f }
        val fast = com.tycoon.world.FirstPersonCamera(camera).apply { sensitivity = 0.4f }
        slow.rotate(-40f, 0f)
        fast.rotate(-40f, 0f)
        assertTrue(fast.yaw > slow.yaw)
    }
}

class SettingsTest {

    @Test
    fun `sensitivity is clamped to the allowed range`() {
        val settings = Settings()
        settings.lookSensitivity = 100f
        assertEquals(Settings.MAX_SENSITIVITY, settings.lookSensitivity, 1e-6f)
        settings.lookSensitivity = -5f
        assertEquals(Settings.MIN_SENSITIVITY, settings.lookSensitivity, 1e-6f)
    }

    @Test
    fun `snapshot and restore round trip every field`() {
        val original = Settings().apply {
            lookSensitivity = 1.75f
            invertLookY = true
            showCrosshair = false
            showJoystick = false
            sfxEnabled = false
            masterVolume = 0.4f
        }

        val restored = Settings().apply { restore(original.snapshot()) }

        assertEquals(1.75f, restored.lookSensitivity, 1e-6f)
        assertTrue(restored.invertLookY)
        assertFalse(restored.showCrosshair)
        assertFalse(restored.showJoystick)
        assertFalse(restored.sfxEnabled)
        assertEquals(0.4f, restored.masterVolume, 1e-6f)
    }

    @Test
    fun `a fresh settings object uses defaults`() {
        val settings = Settings()
        assertEquals(1f, settings.lookSensitivity, 1e-6f)
        assertFalse(settings.invertLookY)
        assertTrue(settings.showCrosshair)
        assertTrue(settings.showJoystick)
        assertTrue(settings.sfxEnabled)
        assertEquals(1f, settings.masterVolume, 1e-6f)
    }

    @Test
    fun `master volume is clamped to the allowed range`() {
        val settings = Settings()
        settings.masterVolume = 5f
        assertEquals(Settings.MAX_VOLUME, settings.masterVolume, 1e-6f)
        settings.masterVolume = -2f
        assertEquals(Settings.MIN_VOLUME, settings.masterVolume, 1e-6f)
    }
}

/** The procedural audio synthesis; pure arithmetic on byte arrays. */
class SoundSynthTest {

    @Test
    fun `every effect renders a valid mono 16-bit wav`() {
        for (id in com.tycoon.audio.SfxId.entries) {
            val wav = com.tycoon.audio.SoundSynth.wav(id)

            assertEquals("RIFF", ascii(wav, 0, 4), "заголовок RIFF для $id")
            assertEquals("WAVE", ascii(wav, 8, 4), "метка WAVE для $id")
            assertEquals("data", ascii(wav, 36, 4), "чанк data для $id")
            assertEquals(1, leShort(wav, 22), "моно для $id")
            assertEquals(16, leShort(wav, 34), "16 бит для $id")
            assertEquals(com.tycoon.audio.SoundSynth.SAMPLE_RATE, leInt(wav, 24), "частота для $id")
            assertEquals(wav.size - 44, leInt(wav, 40), "размер данных для $id")
        }
    }

    @Test
    fun `rendered samples stay inside the 16-bit range`() {
        for (id in com.tycoon.audio.SfxId.entries) {
            val wav = com.tycoon.audio.SoundSynth.wav(id)
            assertTrue(wav.size > 44, "непустой звук для $id")
            for (offset in 44 until wav.size step 2) {
                val sample = pcmSample(wav, offset)
                assertTrue(sample in Short.MIN_VALUE..Short.MAX_VALUE, "сэмпл в диапазоне для $id")
            }
        }
    }

    @Test
    fun `the waveform actually carries signal`() {
        val wav = com.tycoon.audio.SoundSynth.wav(com.tycoon.audio.SfxId.COIN_PICKUP)
        var peak = 0
        for (offset in 44 until wav.size step 2) {
            peak = maxOf(peak, kotlin.math.abs(pcmSample(wav, offset).toInt()))
        }
        assertTrue(peak > 1000, "пик слышимой амплитуды, получено $peak")
    }

    @Test
    fun `bigger recipes render longer effects`() {
        val click = com.tycoon.audio.SoundSynth.wav(com.tycoon.audio.SfxId.UI_CLICK).size
        val purchase = com.tycoon.audio.SoundSynth.wav(com.tycoon.audio.SfxId.PURCHASE).size
        assertTrue(purchase > click, "покупка длиннее клика")
    }

    private fun ascii(bytes: ByteArray, offset: Int, length: Int): String =
        String(bytes, offset, length, Charsets.US_ASCII)

    private fun leInt(bytes: ByteArray, offset: Int): Int =
        (bytes[offset].toInt() and 0xFF) or
            ((bytes[offset + 1].toInt() and 0xFF) shl 8) or
            ((bytes[offset + 2].toInt() and 0xFF) shl 16) or
            ((bytes[offset + 3].toInt() and 0xFF) shl 24)

    private fun leShort(bytes: ByteArray, offset: Int): Int =
        (bytes[offset].toInt() and 0xFF) or ((bytes[offset + 1].toInt() and 0xFF) shl 8)

    /** Reads a little-endian sample as a signed 16-bit value. */
    private fun pcmSample(bytes: ByteArray, offset: Int): Int {
        val raw = leShort(bytes, offset)
        return if (raw >= 0x8000) raw - 0x10000 else raw
    }
}
