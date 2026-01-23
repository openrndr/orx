package primitives

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.shapes.primitives.regularStar
import org.openrndr.math.Vector2
import org.openrndr.shape.contains
import kotlin.math.cos
import kotlin.math.sin

/**
 * Demonstrates how to create a 12-point regular star, and one approach to filling
 * the star with a grid of circles: testing whether various Vector2 coordinates are `in` the
 * `ShapeContour` or not.
 */
fun main() = application {
    program {
        extend {
            drawer.fill = ColorRGBa.PINK
            drawer.stroke = ColorRGBa.WHITE
            val radius0 = cos(seconds) * 50.0 + 130.0
            val radius1 = sin(seconds * 2.0) * 50.0 + 130.0

            val star = regularStar(12, radius0, radius1)

            drawer.translate(width / 2.0, height / 2.0)
            drawer.rotate(seconds * 45.0)
            drawer.fill = null
            drawer.strokeWeight = 2.0
            drawer.contour(star)

            drawer.strokeWeight = 1.0
            drawer.fill = ColorRGBa.WHITE

            for (j in -20 until 20) {
                for (i in -20 until 20) {
                    val q = Vector2(i * 10.0, j * 10.0)
                    if (q in star) {
                        drawer.circle(i * 10.0, j * 10.0, 5.0)
                    }
                }
            }
        }
    }
}