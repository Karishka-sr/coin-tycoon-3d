package com.tycoon.tycoon

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.utils.Json
import com.badlogic.gdx.utils.JsonWriter

/**
 * Reads and writes a single JSON file in the local storage. Failures are logged
 * rather than thrown: a broken or unwritable save must never take the game down.
 */
internal object JsonStore {

    private val json = Json().apply {
        setOutputType(JsonWriter.OutputType.json)
        setIgnoreUnknownFields(true)
    }

    fun write(fileName: String, value: Any) {
        runCatching {
            Gdx.files.local(fileName).writeString(json.prettyPrint(value), false, "UTF-8")
        }.onFailure { Gdx.app.error("JsonStore", "Не удалось записать $fileName", it) }
    }

    fun <T : Any> read(fileName: String, type: Class<T>): T? = runCatching {
        val file = Gdx.files.local(fileName)
        if (!file.exists()) null else json.fromJson(type, file.readString("UTF-8"))
    }.onFailure { Gdx.app.error("JsonStore", "Не удалось прочитать $fileName", it) }.getOrNull()

    fun delete(fileName: String) {
        runCatching { Gdx.files.local(fileName).delete() }
    }
}
