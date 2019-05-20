package org.openrndr.extra.olive

import org.openrndr.Extension
import org.openrndr.Program
import org.openrndr.draw.Session
import org.operndr.extras.filewatcher.watchFile
import java.io.File

class Olive<P:Program>():Extension {
    override var enabled: Boolean = true
    var session: Session? = null

    var script = "src/main/kotlin/live.kts"

    override fun setup(program: Program) {
        val f = File(script)



        if (!f.exists()) {
            f.parentFile.mkdirs()
            f.writeText("""
                @file:Suppress("UNUSED_LAMBDA_EXPRESSION")
                import org.openrndr.Program
                import org.openrndr.draw.*

                { program: ${program.javaClass.name} ->
                    program.apply {
                        extend {

                        }
                    }
                }
            """.trimIndent())
        }

        program.watchFile(File(script)) {
            try {

                val script =it.readText()

                println(script)
                val func = KtsObjectLoader().load<P.() -> Unit>(script)

                program.extensions.clear()

                program.keyboard.keyDown.listeners.clear()
                program.keyboard.keyUp.listeners.clear()
                program.keyboard.character.listeners.clear()
                program.keyboard.keyRepeat.listeners.clear()
                program.mouse.clicked.listeners.clear()
                program.mouse.buttonDown.listeners.clear()
                program.mouse.dragged.listeners.clear()
                program.mouse.buttonUp.listeners.clear()
                program.mouse.moved.listeners.clear()
                program.window.drop.listeners.clear()
                program.window.focused.listeners.clear()
                program.window.minimized.listeners.clear()
                program.window.unfocused.listeners.clear()
                program.window.restored.listeners.clear()
                program.window.sized.listeners.clear()
                session?.end()

                session = Session()
                session?.start()

                func(program as P)
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }
}