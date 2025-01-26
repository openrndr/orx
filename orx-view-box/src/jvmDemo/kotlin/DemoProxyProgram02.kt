import org.openrndr.*
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.viewbox.ViewBox
import org.openrndr.extra.viewbox.viewBox
import org.openrndr.math.Vector2
import kotlin.math.cos

/**
 * Demonstrates how to use two proxy programs and
 * toggle between them by clicking the mouse.
 *
 * programA draws a circle and can be moved by pressing the
 * arrow keys.
 *
 * programB draws a ring located at the current mouse
 * position.
 *
 * Note that programA keeps listening to the key events
 * even if programB is currently displayed.
 */
fun Program.programA() {
    var pos = drawer.bounds.center
    extend {
        drawer.fill = ColorRGBa.PINK
        drawer.circle(pos, cos(seconds) * 50.0 + 50.0)
    }
    keyboard.keyDown.listen {
        when (it.key) {
            KEY_ARROW_UP -> pos -= Vector2.UNIT_Y * 20.0
            KEY_ARROW_DOWN -> pos += Vector2.UNIT_Y * 20.0
            KEY_ARROW_LEFT -> pos -= Vector2.UNIT_X * 20.0
            KEY_ARROW_RIGHT -> pos += Vector2.UNIT_X * 20.0
        }
    }
}

fun Program.programB() {
    extend {
        drawer.fill = ColorRGBa.TRANSPARENT
        drawer.stroke = ColorRGBa.CYAN
        drawer.strokeWeight = 50.0
        drawer.circle(mouse.position, 200.0)
    }
}

fun main() = application {
    configure {
        width = 720
        height = 360
    }
    program {
        val proxyBoxes = mutableListOf<ViewBox>()
        proxyBoxes.add(viewBox(drawer.bounds).apply { programA() })
        proxyBoxes.add(viewBox(drawer.bounds).apply { programB() })

        var currentBox = 1
        extend {
            proxyBoxes[currentBox].draw()
        }
        mouse.buttonDown.listen {
            currentBox = (currentBox + 1) % proxyBoxes.size
        }
    }
}
