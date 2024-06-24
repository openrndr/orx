package org.openrndr.extra.olive

import org.openrndr.ApplicationBuilder
import org.openrndr.ProgramImplementation
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.reflect.KProperty

open class OliveProgram(private val sourceLocation: String, private val scriptHost: OliveScriptHost, resources: Resources?) : ProgramImplementation() {
    val olive = extend(Olive<OliveProgram>(scriptMode = ScriptMode.OLIVE_PROGRAM, resources = resources)) {
        script = sourceLocation
        scriptHost = this@OliveProgram.scriptHost
    }
}

/**
 * Delegate used to create instances exactly once. Instances survive a script reload.
 */
class Once<T:Any>(val build:() -> T) {
    companion object {
        private val values = mutableMapOf<String, Any>()
    }
    @Suppress("UNCHECKED_CAST")
    operator fun getValue(thisRef:Any?, property:KProperty<*>) : T = values.getOrPut(property.name) { build() } as T
}

fun stackRootClassName(thread: Thread = Thread.currentThread(), sanitize: Boolean = true): String {
    val root = Thread.currentThread().stackTrace.last()
    val rootClass = root.className
    return if (sanitize) rootClass.replace(Regex("Kt$"), "") else rootClass
}

fun ApplicationBuilder.oliveProgram(scriptHost: OliveScriptHost = OliveScriptHost.JSR223, resources: Resources? = null, init: OliveProgram.() -> Unit): OliveProgram {
    val rootClassName = stackRootClassName(sanitize = true).split(".").last()

    var sourceLocation = "src/main/kotlin/$rootClassName.kt"
    val candidateFile = File(sourceLocation)
    val rootClassNameCleaned = if (rootClassName.startsWith("_")) rootClassName.drop(1) else rootClassName

    if (!candidateFile.exists()) {
        val otherCandidates = Files.walk(Paths.get("."))
                .filter { Files.isRegularFile(it) && it.toString().endsWith("$rootClassNameCleaned.kt") }.toList()
        if (otherCandidates.size == 1) {
            sourceLocation = otherCandidates.first().toString()
        } else {
            error("multiple source candidates found for $rootClassName: $otherCandidates")
        }
    }
    program = object : OliveProgram(sourceLocation, scriptHost, resources) {
        override suspend fun setup() {
            super.setup()
            init()
        }
    }
    return program as OliveProgram
}