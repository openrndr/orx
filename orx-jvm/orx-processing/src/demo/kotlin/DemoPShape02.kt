import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.processing.PShape
import org.openrndr.extra.processing.toShape
import org.openrndr.extra.shapes.primitives.regularStarRounded

/**
 * Demonstrates how to convert a `ShapeContour` into a Processing
 * `PShape`, then converts the `PShape` to a `Shape`.
 *
 * The program renders both the original `ShapeContour` and
 * the resulting `Shape` after being a `PShape`.
 *
 * Both elements are rendered with translucency and a slight offset
 * so they can be visually compared.
 */
fun main() = application {
    program {
        val c = regularStarRounded(
            points = 5,
            innerRadius = 100.0,
            outerRadius = 200.0,
            innerFactor = 0.25,
            outerFactor = 0.75,
            center = drawer.bounds.center
        )
        val ps = PShape(c)
        val rs = ps.toShape()
        extend {
            drawer.fill = ColorRGBa.PINK.opacify(0.5)
            drawer.shape(rs)

            drawer.translate(15.0, 15.0)
            drawer.contour(c)
        }
    }
}