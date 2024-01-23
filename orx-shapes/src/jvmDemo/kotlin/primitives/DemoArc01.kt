package primitives

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.shapes.primitives.Arc

fun main() {
    application {
        configure {
            width = 720
            height = 720
        }
        program {
            extend {
                val a = Arc(drawer.bounds.center, 100.0, 0.0 + seconds * 36.0, -180.0 + seconds * 36.0)
                drawer.clear(ColorRGBa.PINK)
                drawer.contour(a.contour)
                drawer.circle(a.position(0.0), 5.0)
                drawer.circle(a.position(0.5), 5.0)
                drawer.circle(a.position(1.0), 5.0)
            }
        }
    }
}