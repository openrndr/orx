package primitives

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.isolated
import org.openrndr.extra.shapes.primitives.regularPolygon
import org.openrndr.math.map
import kotlin.math.cos

/**
 * Demonstrates how to use `regularPolygon()` to create regular `ShapeContour`s.
 * By specifying the number of side the program creates a triangle, a square,
 * a pentagon, a hexagon, a heptagon and an octagon.
 *
 * The radius is animated with the cosine of time.
 *
 * The position is calculated by converting the shape index into columns and rows
 * (using modulo and the integer division), then mapped to window coordinates.
 *
 * A continuous rotation is applied to all polygons for an animated effect.
 *
 */
fun main() = application {
    program {
        extend {
            drawer.fill = ColorRGBa.PINK
            drawer.stroke = ColorRGBa.WHITE

            for (sides in 0 until 8) {
                val radius0 = cos(seconds + sides) * 20.0 + 40.0
                val polygon = regularPolygon(sides + 3, radius = radius0)

                drawer.isolated {
                    translate(
                            (sides % 4.0).map(0.0, 3.0,
                                    width * 0.2, width * 0.8),
                            (sides / 4).toDouble().map(0.0, 1.0,
                                    height * 0.3, height * 0.7))
                    rotate(seconds * 45.0)
                    contour(polygon)
                }
            }
        }
    }
}