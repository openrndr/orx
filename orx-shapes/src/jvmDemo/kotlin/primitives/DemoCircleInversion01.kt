package primitives

import org.openrndr.application
import org.openrndr.extra.shapes.primitives.invert
import org.openrndr.shape.Circle

fun main() = application {
    configure {
        width = 720
        height = 720
    }
    program {
        extend {
            val c = Circle(drawer.bounds.center, 100.0)
            drawer.circle(c.invert(mouse.position),10.0)
        }
    }
}