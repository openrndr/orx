import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.isolated
import org.openrndr.extra.remotecontrol.RemoteControl
import org.openrndr.math.Vector2
import kotlin.math.cos
import kotlin.math.sin

/**
 * A small, visibly-reactive program to drive with [RemoteControl]: draws a circle that
 * follows the (possibly emulated) mouse position, flips the background color on spacebar,
 * and orbits a dot based on [org.openrndr.Program.seconds] -- so freezing it with `/clock`
 * visibly stops the dot instead of just leaving a static scene unprovable either way. Useful
 * for checking, end to end, that HTTP requests to `/screenshot`, `/mouse/move`,
 * `/keyboard/down` and `/clock` actually reach the running program.
 */
fun main() {
    application {
        configure {
            width = 400
            height = 400
        }
        program {
            var inverted = false
            keyboard.keyDown.listen {
                if (it.name == "spacebar") {
                    inverted = !inverted
                }
            }

            extend(RemoteControl()) {
                port = 9000
                network = "127.0.0.1"
            }
            extend {
                drawer.clear(if (inverted) ColorRGBa.WHITE else ColorRGBa.PINK.shade(0.3))
                drawer.isolated {
                    fill = if (inverted) ColorRGBa.PINK.shade(0.3) else ColorRGBa.WHITE
                    stroke = null
                    circle(mouse.position, 30.0)

                    val center = Vector2(width / 2.0, height / 2.0)
                    val orbit = center + Vector2(cos(seconds), sin(seconds)) * 150.0
                    circle(orbit, 10.0)
                }
            }
        }
    }
}
