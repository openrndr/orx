package polygon

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.shapes.polygon.signedDistance
import org.openrndr.extra.shapes.polygon.toPolygon
import org.openrndr.extra.shapes.primitives.regularStar
import org.openrndr.shape.ShapeContour

fun main() {
    application {
        program {
            extend {
                drawer.clear(ColorRGBa.PINK)
                val polygon = regularStar(7, 50.0, 100.0, drawer.bounds.center).contour.toPolygon()
                val distance = polygon.signedDistance(mouse.position)

                drawer.contour(ShapeContour.fromPoints(polygon.points, true))

                if (distance < 0.0) {
                    drawer.fill = ColorRGBa.GREEN
                }
                drawer.circle(mouse.position, distance)
            }
        }
    }
}