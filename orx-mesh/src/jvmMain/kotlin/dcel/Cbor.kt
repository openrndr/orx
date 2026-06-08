package org.openrndr.extra.mesh.dcel

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import java.io.File

/**
 * Loads a DCEL (Doubly Connected Edge List) instance from a CBOR file.
 *
 * @param file The file from which the DCEL instance will be loaded. The file should contain
 *             a valid CBOR representation of a DCEL.
 * @return A DCEL instance populated with the data loaded from the specified CBOR file.
 */
@OptIn(ExperimentalSerializationApi::class)
fun Dcel.Companion.loadFromCborFile(file: File): Dcel {
    return file.inputStream().use { input ->
        Cbor.decodeFromByteArray(input.readBytes())
    }
}

/**
 * Saves the current DCEL (Doubly Connected Edge List) instance to a CBOR file.
 *
 * @param file The file where the DCEL instance will be saved in CBOR format.
 */

@OptIn(ExperimentalSerializationApi::class)
fun Dcel.saveToCborFile(file: File) {
    file.outputStream().use { output ->
        output.write(Cbor.encodeToByteArray(this@saveToCborFile))
    }
}