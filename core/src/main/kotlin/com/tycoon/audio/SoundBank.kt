package com.tycoon.audio

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Sound
import com.tycoon.tycoon.Settings
import java.util.EnumMap

/**
 * Loads the synthesised effects and plays them on demand.
 *
 * Android's `SoundPool` only accepts a file or an asset descriptor, so the WAV
 * bytes are written to local storage once and loaded from there. Failures are
 * swallowed: a device with a broken audio stack should still run the game.
 */
class SoundBank(private val settings: Settings) {

    private val sounds = EnumMap<SfxId, Sound>(SfxId::class.java)

    init {
        for (id in SfxId.entries) {
            runCatching {
                val file = Gdx.files.local("sfx/${id.name.lowercase()}.wav")
                file.parent().mkdirs()
                file.writeBytes(SoundSynth.wav(id), false)
                sounds[id] = Gdx.audio.newSound(file)
            }.onFailure { Gdx.app.error(TAG, "Не удалось загрузить звук ${id.name}", it) }
        }
    }

    /** Plays [id] with the current volume, or does nothing when sound is muted. */
    fun play(id: SfxId) {
        if (!settings.sfxEnabled) return
        val volume = id.gain * settings.masterVolume
        if (volume <= 0f) return
        runCatching { sounds[id]?.play(volume, id.pitch, 0f) }
    }

    fun dispose() {
        sounds.values.forEach { it.dispose() }
        sounds.clear()
    }

    private companion object {
        const val TAG = "SoundBank"
    }
}
