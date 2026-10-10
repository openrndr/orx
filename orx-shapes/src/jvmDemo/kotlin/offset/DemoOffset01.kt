package offset

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.shapes.primitives.regularStar
import org.openrndr.extra.shapes.primitives.regularStarRounded
import org.openrndr.shape.SegmentJoin
import kotlin.math.cos

/**
 * Demonstrates the use of ShapeContour.offset() with different `SegmentJoin` settings.
 * The offset distance is animated over time using the cosine of time.
 */
fun main() = application {
    program {
        val c = regularStarRounded(5, 60.0, 150.0, 0.5, 0.5, drawer.bounds.center)

        extend {
            val distance = 10.0

            drawer.fill = null
            drawer.stroke = ColorRGBa.PINK.opacify(0.7)
            drawer.contour(c)

            drawer.fill = ColorRGBa.PINK.opacify(0.5)
            drawer.stroke = ColorRGBa.WHITE
            val offset = c.offset(distance, SegmentJoin.MITER)
            drawer.shape(offset)


        }
    }
}
