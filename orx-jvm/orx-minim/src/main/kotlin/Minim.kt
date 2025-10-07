package org.openrndr.extra.minim

import ddf.minim.Minim
import org.openrndr.Program
import java.io.File
import java.io.InputStream

class MinimObject {
    @Suppress("UNUSED_PARAMETER")
    fun sketchPath(fileName: String) = fileName
    fun createInput(fileName: String) = File(fileName).inputStream() as InputStream
}

fun Program.minim(): Minim {
    val minim = Minim(MinimObject())
    ended.listen {
        minim.stop()

    }
    return minim
}