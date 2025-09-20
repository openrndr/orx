package hobbycurve

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.shapes.hobbycurve.hobbyCurve
import org.openrndr.extra.shapes.primitives.regularStar

/**
 * This demo shows how the [org.openrndr.shape.ShapeContour]'s method `hobbyCurve()` can be used
 * to round contours with linear segments.
 */
fun main() = application {
    configure {
        width = 720
        height = 720
    }
    program {
        val star = regularStar(5, 100.0, 300.0, drawer.bounds.center)
        val hobby = star.hobbyCurve()
        extend {
            drawer.fill = ColorRGBa.PINK
            drawer.contour(hobby)

            drawer.fill = null
            drawer.stroke = ColorRGBa.WHITE.opacify(0.5)
            drawer.contour(star)
        }
    }
}