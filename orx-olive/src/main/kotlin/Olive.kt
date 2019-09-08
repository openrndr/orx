package org.openrndr.extra.olive

import org.openrndr.Extension
import org.openrndr.Program
import org.openrndr.draw.Session
import org.openrndr.events.Event
import org.operndr.extras.filewatcher.stop
import org.operndr.extras.filewatcher.watchFile
import java.io.File

fun <T> Event<T>.saveListeners(store: MutableMap<Event<*>, List<(Any) -> Unit>>) {
    @Suppress("UNCHECKED_CAST")
    store[this] = listeners.map { it } as List<(Any) -> Unit>
}

fun <T> Event<T>.restoreListeners(store: Map<Event<*>, List<(Any) -> Unit>>) {
    listeners.retainAll(store[this] ?: emptyList<T>())
}

class Olive<P : Program> : Extension {
    override var enabled: Boolean = true
    var session: Session? = null



    var running = false

    internal var scriptChange: (String)->Unit = {}

    var script = "src/main/kotlin/live.kts"
        set(value) {
            scriptChange(value)
            field = value
        }

    override fun setup(program: Program) {
        System.setProperty("idea.io.use.fallback", "true")
        System.setProperty("org.openrndr.ignoreShadeStyleErrors", "true")

        var watcher: (() -> Unit)? = null

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
                    val script = it.readText()
                    val func = KtsObjectLoader().load<P.() -> Unit>(script)

                    program.extensions.clear()
                    program.extensions.addAll(originalExtensions)

                    trackedListeners.forEach { l -> l.restoreListeners(store) }
                    session?.end()

                    session = Session()
                    session?.start()

                    @Suppress("UNCHECKED_CAST")
                    func(program as P)
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
            }
        }
        setupScript(script)
        scriptChange = ::setupScript
    }
}