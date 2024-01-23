package primitives

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.shapes.primitives.Pulley
import org.openrndr.math.Vector2
import org.openrndr.shape.Circle

fun main() {
    application {
        configure {
            width = 720
            height = 720
        }
        program {
            extend {
                drawer.clear(ColorRGBa.BLACK)
                drawer.stroke = ColorRGBa.WHITE
                drawer.fill = ColorRGBa.PINK
                val pulley = Pulley(
                    Circle(drawer.bounds.center - Vector2(100.0, 100.0), 150.0),
                    Circle(drawer.bounds.center + Vector2(150.0, 150.0), 75.0)
                )
                drawer.contour(pulley.contour)
            }
        }
    }
}