package com.tycoon.tycoon

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.utils.Json
import com.badlogic.gdx.utils.JsonWriter

/** Reads and writes the single save slot used by the game. */
object SaveManager {

    private const val SAVE_FILE = "coin-tycoon-save.json"

    private val json = Json().apply {
        setOutputType(JsonWriter.OutputType.json)
        setIgnoreUnknownFields(true)
    }

    fun save(state: TycoonState) {
        runCatching {
            Gdx.files.local(SAVE_FILE).writeString(json.prettyPrint(state.snapshot()), false, "UTF-8")
        }.onFailure { Gdx.app.error("SaveManager", "Не удалось сохранить игру", it) }
    }

    fun load(): SaveData? = runCatching {
        val file = Gdx.files.local(SAVE_FILE)
        if (!file.exists()) null else json.fromJson(SaveData::class.java, file.readString("UTF-8"))
    }.onFailure { Gdx.app.error("SaveManager", "Не удалось загрузить игру", it) }.getOrNull()

    fun delete() {
        runCatching { Gdx.files.local(SAVE_FILE).delete() }
    }
}
