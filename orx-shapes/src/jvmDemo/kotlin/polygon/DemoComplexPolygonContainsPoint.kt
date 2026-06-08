package polygon

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.shapes.polygon.ComplexPolygon2D
import org.openrndr.extra.shapes.polygon.containsPoint
import org.openrndr.extra.shapes.polygon.shape
import org.openrndr.extra.shapes.polygon.signedDistance
import org.openrndr.extra.shapes.polygon.toPolygon
import org.openrndr.extra.shapes.primitives.regularPolygon
import org.openrndr.extra.shapes.primitives.regularStar

fun main() {
    application {
        program {

            val outer = regularStar(5, 100.0, 140.0, drawer.bounds.center).toPolygon()
            val inner = regularPolygon(5, drawer.bounds.center, 50.0).reversed.toPolygon()

            val complex = ComplexPolygon2D(outer, listOf(inner))
            extend {

                drawer.shape(complex.shape)
                if (complex.containsPoint(mouse.position)) {
                    drawer.fill = ColorRGBa.RED
                }
                val distance = complex.signedDistance(mouse.position)

                drawer.circle(mouse.position, distance)
            }
        }
    }
}