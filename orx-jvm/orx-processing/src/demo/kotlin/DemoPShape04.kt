import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.processing.PShape
import org.openrndr.extra.processing.toShape
import org.openrndr.shape.contour

/**
 * Demonstrates how to convert a `ShapeContour` into a Processing
 * `PShape`, then converts the `PShape` to a `Shape`.
 *
 * The program creates a `ShapeContour` with quadratic, cubic, and straight segments.
 *
 * Both elements are rendered with translucency and a slight offset
 * so they can be visually compared.
 */
fun main() = application {
    program {
        val c = contour {
            moveTo(100.0, 100.0)
            curveTo(200.0, 200.0, 540.0, 100.0)
            curveTo(100.0, 400.0, 500.0, 300.0, 320.0, 400.0)
            close()
        }
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