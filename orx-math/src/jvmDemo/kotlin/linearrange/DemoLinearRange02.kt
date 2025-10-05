package linearrange

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.isolated
import org.openrndr.extra.math.linearrange.rangeTo
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import kotlin.math.cos
import kotlin.math.sin

/**
 * Demonstrate how to create a 1D linear range between two instances of a `LinearType`, in this case,
 * a horizontal `Rectangle` and a vertical one.
 *
 * Notice how the `..` operator is used to construct the `LinearRange1D`.
 *
 * The resulting `LinearRange1D` provides a `value()` method that takes a normalized
 * input and returns an interpolated value between the two input elements.
 *
 * This example draws a grid of rectangles interpolated between the horizontal and the vertical
 * triangles. The x and y coordinates and the `seconds` variable are used to specify the
 * interpolation value for each grid cell.
 *
 * One can use the `LinearRange` class to construct
 * - a `LinearRange2D` out of two `LinearRange1D`
 * - a `LinearRange3D` out of two `LinearRange2D`
 * - a `LinearRange4D` out of two `LinearRange3D`
 *
 * (not demonstrated here)
 *
 */
fun main() {
    application {
        configure {
            width = 720
            height = 720
        }
        program {
            val range = Rectangle.fromCenter(Vector2(36.0, 36.0), 72.0, 18.0)..
                    Rectangle.fromCenter(Vector2(36.0, 36.0), 18.0, 72.0)
            extend {
                drawer.fill = ColorRGBa.PINK.opacify(0.9)
                drawer.stroke = null
                for (y in 0 until height step 72) {
                    for (x in 0 until width step 72) {
                        val u = cos(seconds + x * 0.007) * 0.5 + 0.5
                        val s = sin(seconds * 1.03 + y * 0.0075) * 0.5 + 0.5
                        drawer.isolated {
                            drawer.translate(x.toDouble(), y.toDouble())
                            drawer.rectangle(range.value(u * s))
                        }
                    }
                }
            }
        }
    }
}