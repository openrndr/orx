package org.openrndr.extra.minim

import ddf.minim.Minim
import org.openrndr.Program
import java.io.File

class MinimObject {
    fun sketchPath(fileName:String) = "./"
    fun createInput(fileName: String) = File(fileName).inputStream()
}

fun Program.minim(): Minim  {
    val minim = Minim(MinimObject())
    ended.listen {
        minim.stop()

    }
    return minim
}