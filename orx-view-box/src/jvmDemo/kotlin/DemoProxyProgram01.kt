import org.openrndr.Program
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.viewbox.viewBox
import org.openrndr.math.Vector2
import kotlin.math.cos

/**
 * Demonstrates how to use a proxy program inside a [viewBox],
 * how the main program can access its variables and methods,
 * and execute its `extend` block by calling its `draw()` method.
 */
fun Program.program01() {
    var color: ColorRGBa by this.userProperties
    color = ColorRGBa.WHITE

    var someFunction: () -> Unit by this.userProperties
    someFunction = {
        color = ColorRGBa(Math.random(), Math.random(), Math.random())
    }

    extend {
        drawer.fill = color
        drawer.circle(Vector2(width / 2.0, height / 2.0), cos(seconds) * 50.0 + 50.0)
    }
}

fun main() = application {
    configure {
        width = 720
        height = 360
    }
    program {
        val proxyBox = viewBox(drawer.bounds).apply { program01() }
        var color: ColorRGBa by proxyBox.userProperties
        val someFunction: () -> Unit by proxyBox.userProperties
        color = ColorRGBa.PINK
        extend {
            if (Math.random() < 0.01) {
                someFunction()
            }
            proxyBox.draw()
        }
    }
}
