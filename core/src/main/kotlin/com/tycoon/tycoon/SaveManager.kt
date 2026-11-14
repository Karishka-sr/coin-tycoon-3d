package com.tycoon.tycoon

/** Reads and writes the single save slot used by the game. */
object SaveManager {

    private const val SAVE_FILE = "coin-tycoon-save.json"

    fun save(state: TycoonState) = JsonStore.write(SAVE_FILE, state.snapshot())

    fun load(): SaveData? = JsonStore.read(SAVE_FILE, SaveData::class.java)

    fun delete() = JsonStore.delete(SAVE_FILE)
}
