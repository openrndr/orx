package org.openrndr.extra.olive

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import mu.KotlinLogging
import org.openrndr.Extension
import org.openrndr.Program
import org.openrndr.draw.Session
import org.openrndr.events.Event
import org.openrndr.exceptions.stackRootClassName
import org.openrndr.extra.kotlinparser.extractProgram
import org.openrndr.launch
import org.operndr.extras.filewatcher.stop
import org.operndr.extras.filewatcher.triggerChange
import org.operndr.extras.filewatcher.watchFile
import java.io.File

private val logger = KotlinLogging.logger {}


private fun <T> Event<T>.saveListeners(store: MutableMap<Event<*>, List<(Any) -> Unit>>) {
    @Suppress("UNCHECKED_CAST")
    store[this] = listeners.map { it } as List<(Any) -> Unit>
}

private fun <T> Event<T>.restoreListeners(store: Map<Event<*>, List<(Any) -> Unit>>) {
    listeners.retainAll(store[this] ?: emptyList<T>())
}

enum class OliveScriptHost {
    JSR223,
    JSR223_REUSE,
    KOTLIN_SCRIPT
}

data class ScriptLoadedEvent(val scriptFile: String)

enum class ScriptMode {
    KOTLIN_SCRIPT,
    OLIVE_PROGRAM
}

class Olive<P : Program>(val resources: Resources? = null, private var scriptMode: ScriptMode = ScriptMode.KOTLIN_SCRIPT) : Extension {
    override var enabled: Boolean = true
    var session: Session? = null
    var scriptHost = OliveScriptHost.JSR223_REUSE

    val scriptLoaded = Event<ScriptLoadedEvent>()

    internal var scriptChange: (String) -> Unit = {}

    var script = when (scriptMode) {
        ScriptMode.KOTLIN_SCRIPT -> "src/main/kotlin/${stackRootClassName().split(".").last()}.kts"
        else -> "src/main/kotlin/${stackRootClassName().split(".").last()}.kt"
    }
        set(value) {
            field = value
            scriptChange(value)
        }

    /**
     * reloads the active script
     */
    fun reload() {
        watcher?.triggerChange()
    }

    private var watcher: (() -> Unit)? = null

    override fun setup(program: Program) {
        System.setProperty("idea.io.use.fallback", "true")
        System.setProperty("org.openrndr.ignoreShadeStyleErrors", "true")

        val store = mutableMapOf<Event<*>, List<(Any) -> Unit>>()
        val originalExtensions = program.extensions.map { it }
        val trackedListeners = listOf<Event<*>>(program.mouse.buttonDown,
                program.mouse.buttonUp,
                program.mouse.clicked,
                program.mouse.dragged,
                program.mouse.moved,
                program.mouse.scrolled,
                program.keyboard.keyUp,
                program.keyboard.keyDown,
                program.keyboard.keyRepeat,
                program.window.drop,
                program.window.focused,
                program.window.minimized,
                program.window.moved,
                program.window.sized,
                program.window.unfocused)

        trackedListeners.forEach { it.saveListeners(store) }

        fun setupScript(scriptFile: String) {
            watcher?.stop()
            val f = File(scriptFile)
            if (!f.exists()) {
                f.parentFile.mkdirs()
                var className = program.javaClass.name
                if (className.contains("$"))
                    className = "Program"

                f.writeText("""
                @file:Suppress("UNUSED_LAMBDA_EXPRESSION")
                import org.openrndr.Program
                import org.openrndr.draw.*

                { program: $className ->
                    program.apply {
                        extend {

                        }
                    }
                }
            """.trimIndent())
            }

            val jsr233ObjectLoader = if (scriptHost == OliveScriptHost.JSR223_REUSE) ScriptObjectLoader() else null

            watcher = program.watchFile(File(script)) {
                try {
                    logger.info("change detected, reloading script")

                    val scriptContents = when (scriptMode) {
                        ScriptMode.KOTLIN_SCRIPT -> it.readText()
                        ScriptMode.OLIVE_PROGRAM -> {
                            val source = it.readText()
                            val programSource = extractProgram(source, programIdentifier = "oliveProgram")
                            generateScript<OliveProgram>(programSource)
                        }
                    }

                    val futureFunc = GlobalScope.async {
                        val start = System.currentTimeMillis()
                        val loadedFunction = when (scriptHost) {
                            OliveScriptHost.JSR223_REUSE -> loadFromScriptContents(scriptContents, jsr233ObjectLoader!!)
                            OliveScriptHost.JSR223 -> loadFromScriptContents(scriptContents)
                            OliveScriptHost.KOTLIN_SCRIPT -> loadFromScriptContentsKSH<P.() -> Unit>(scriptContents)
                        }

                        val end = System.currentTimeMillis()
                        logger.info { "loading script took ${end - start}ms" }
                        loadedFunction
                    }

                    program.launch {
                        val func = futureFunc.await()
                        program.extensions.forEach {extension ->
                            extension.shutdown(program)
                        }
                        program.extensions.clear()
                        program.extensions.addAll(originalExtensions)

                        trackedListeners.forEach { l -> l.restoreListeners(store) }
                        session?.end()
                        session = Session.root.fork()

                        @Suppress("UNCHECKED_CAST")
                        func(program as P)
                        scriptLoaded.trigger(ScriptLoadedEvent(scriptFile))
                        Unit
                    }
                    Unit
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
            }
        }
        setupScript(script)
        scriptChange = ::setupScript

        if (resources != null) {
            val srcPath = "src/main/resources"
            var src = File(srcPath)

            resources.watch(src) { file ->
                val dest = "build/resources/main"
                val filePath = file.path.split(Regex(srcPath), 2).getOrNull(1)

                val destFile = File("$dest/${filePath}").absoluteFile

                program.watchFile(file) {
                    if (resources[file]!! && filePath != null) {
                        file.copyTo(destFile, overwrite = true)
                        reload()
                    } else {
                        resources[file] = true
                    }
                }
            }
        }
    }
}