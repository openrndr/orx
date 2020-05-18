package org.openrndr.extra.olive

import org.openrndr.ApplicationBuilder
import org.openrndr.Program
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.streams.toList

open class OliveProgram(private val sourceLocation: String, private val scriptHost: OliveScriptHost) : Program() {
    val olive = extend(Olive<OliveProgram>(scriptMode = ScriptMode.OLIVE_PROGRAM)) {
        script = sourceLocation
    }
}

fun stackRootClassName(thread: Thread = Thread.currentThread(), sanitize: Boolean = true): String {
    val root = Thread.currentThread().stackTrace.last()
    val rootClass = root.className
    return if (sanitize) rootClass.replace(Regex("Kt$"), "") else rootClass
}

fun ApplicationBuilder.oliveProgram(scriptHost: OliveScriptHost = OliveScriptHost.JSR223_REUSE, init: OliveProgram.() -> Unit): OliveProgram {
    val rootClassName = stackRootClassName(sanitize = true).split(".").last()

    var sourceLocation = "src/main/kotlin/$rootClassName.kt"
    val candidateFile = File(sourceLocation)
    if (!candidateFile.exists()) {
        val otherCandidates = Files.walk(Paths.get("."))
                .filter { Files.isRegularFile(it) && it.toString().endsWith("$rootClassName.kt") }.toList()
        if (otherCandidates.size == 1) {
            sourceLocation = otherCandidates.first().toString()
        } else {
            error("multiple source candidates found: $otherCandidates")
        }
    }
    program = object : OliveProgram(sourceLocation, scriptHost) {
        override fun setup() {
            super.setup()
            init()
        }
    }
    return program as OliveProgram
}