package org.openrndr.extra.mesh.dcel

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import java.io.File

/**
 * Loads a DCEL (Doubly Connected Edge List) instance from a JSON file.
 *
 * @param file The file from which the DCEL instance will be loaded. The file should contain
 *             a valid JSON representation of a DCEL.
 * @return A DCEL instance populated with the data loaded from the specified JSON file.
 */
@OptIn(ExperimentalSerializationApi::class)
fun Dcel.Companion.loadFromJsonFile(file: File): Dcel {
    return file.inputStream().use { input ->
        Json.decodeFromStream(input)
    }
}

/**
 * Saves the current DCEL (Doubly Connected Edge List) instance to a JSON file.
 *
 * @param file The file where the DCEL instance will be saved in JSON format.
 */
@OptIn(ExperimentalSerializationApi::class)
fun Dcel.saveToJsonFile(file: File) {
    file.outputStream().use { output ->
        Json.encodeToStream(this, output)
    }
}