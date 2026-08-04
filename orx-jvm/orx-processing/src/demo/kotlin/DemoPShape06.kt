import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.processing.PShape
import org.openrndr.extra.processing.toShape
import org.openrndr.shape.Circle
import org.openrndr.shape.Shape

/**
 * Demonstrates that a `Shape` can be converted to a
 * `PShape` and back to a `Shape` while maintaining
 * its looks.
 */
fun main() = application {
    program {
        val a = Shape(
            listOf(
                Circle(0.0, 0.0, 80.0).contour,
                Circle(0.0, 0.0, 40.0).contour.reversed
            )
        )
        val b = PShape(a).toShape()
        extend {
            drawer.clear(ColorRGBa.PINK)
            drawer.translate(drawer.bounds.center)
            drawer.shape(a)
            drawer.translate(a.bounds.width * 0.4, 0.0)
            drawer.shape(b)
        }
    }
}