package primitives

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.noise.scatter
import org.openrndr.extra.shapes.primitives.Tear
import org.openrndr.math.Vector2
import org.openrndr.shape.Circle
import kotlin.random.Random

fun main() {
    application {
        configure {
            width = 720
            height = 720
        }
        program {
            val points = drawer.bounds.scatter(40.0, distanceToEdge = 150.0, random = Random(0))
            val tears = points.map {
                Tear(it - Vector2(0.0, 20.0), Circle(it + Vector2(0.0, 20.0), 20.0))
            }

            extend {
                drawer.clear(ColorRGBa.BLACK)
                drawer.fill = ColorRGBa.PINK
                drawer.stroke = ColorRGBa.WHITE
                drawer.contours(tears.map { it.contour })
            }
        }
    }
}