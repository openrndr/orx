package org.openrndr.extra.olive

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import mu.KotlinLogging
import org.openrndr.Extension
import org.openrndr.Program
import org.openrndr.draw.Session
import org.openrndr.events.Event
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

class Olive<P : Program>(val resources: Resources? = null) : Extension {
    override var enabled: Boolean = true
    var session: Session? = null

    internal var scriptChange: (String)->Unit = {}

    var script = "src/main/kotlin/live.kts"
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

        fun setupScript(scriptFile:String) {
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

            watcher = program.watchFile(File(script)) {
                try {
                    val futureFunc = GlobalScope.async {
                        val start = System.currentTimeMillis()
                        val f = loadFromScriptKSH<P.() -> Unit>(it)
                        val end = System.currentTimeMillis()
                        logger.info { "loading script took ${end - start}ms" }
                        f
                    }

                    program.launch {
                        val func =  futureFunc.await()
                        program.extensions.clear()
                        program.extensions.addAll(originalExtensions)

                        trackedListeners.forEach { l -> l.restoreListeners(store) }
                        session?.end()
                        session = Session.root.fork()

                        @Suppress("UNCHECKED_CAST")
                        func(program as P)

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