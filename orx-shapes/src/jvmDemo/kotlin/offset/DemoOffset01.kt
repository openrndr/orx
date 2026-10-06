package offset

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.shapes.primitives.regularPolygonRounded
import org.openrndr.shape.Circle
import org.openrndr.shape.Rectangle
import org.openrndr.shape.SegmentJoin
import kotlin.math.cos

/**
 * Demonstrates the use of ShapeContour.offset() with different `SegmentJoin` settings.
 * The offset distance is animated over time using the cosine of time.
 */
fun main() = application {
    program {
        val c = Rectangle(100.0, 100.0, width - 200.0, height - 200.0).contour //.reversed
        //val c = Circle(drawer.bounds.center, 150.0).contour
        //val c = regularPolygonRounded(4, 0.2, drawer.bounds.center, 150.0, 15.0)

        extend {
            drawer.fill = null
            drawer.stroke = ColorRGBa.PINK.opacify(0.7)
            drawer.contour(c)
            drawer.contour(c.offset(cos(seconds + 0.5) * 40.0, SegmentJoin.BEVEL))
            drawer.contour(c.offset(cos(seconds + 0.5) * 80.0, SegmentJoin.MITER))
            drawer.contour(c.offset(cos(seconds + 0.5) * 120.0, SegmentJoin.ROUND))
        }
    }
}