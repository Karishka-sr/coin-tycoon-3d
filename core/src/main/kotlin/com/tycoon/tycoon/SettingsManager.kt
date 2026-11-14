package com.tycoon.tycoon

/** Reads and writes the player's options. */
object SettingsManager {

    private const val SETTINGS_FILE = "coin-tycoon-settings.json"

    /** Loads the stored options, falling back to defaults when none exist yet. */
    fun load(): Settings = Settings().apply {
        JsonStore.read(SETTINGS_FILE, SettingsData::class.java)?.let(::restore)
    }

    fun save(settings: Settings) = JsonStore.write(SETTINGS_FILE, settings.snapshot())

    fun delete() = JsonStore.delete(SETTINGS_FILE)
}
