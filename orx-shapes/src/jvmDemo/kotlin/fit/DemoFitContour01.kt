package fit

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.shapes.fit.fitCubicBeziers
import org.openrndr.math.Vector2
import org.openrndr.shape.ShapeContour

fun main() = application {
    configure {
        width = 720
        height = 720
    }

    program {

        val points = mutableListOf<Vector2>()

        mouse.dragged.listen {

            if (points.isEmpty() || points.last().distanceTo(it.position) > 10.0) {
                points.add(mouse.position)
            }
        }
        extend {

            if (points.size > 2) {
                val segments = fitCubicBeziers(points, minPointsToSplit = 5)

                val c = ShapeContour.fromSegments(segments, false)

                drawer.fill = null
                drawer.stroke = ColorRGBa.PINK
                drawer.drawStyle.lineJoin = org.openrndr.draw.LineJoin.ROUND
                drawer.contour(c)

                drawer.circles(c.segments.map { it.start }, 5.0)
            }
            drawer.fill = null
            drawer.stroke = ColorRGBa.PINK

            //drawer.circles(points, 5.0)
            drawer.circle(mouse.position, 5.0)


        }
    }
}
