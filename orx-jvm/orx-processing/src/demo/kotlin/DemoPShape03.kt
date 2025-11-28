import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.processing.PShape
import org.openrndr.extra.processing.toShape
import org.openrndr.extra.shapes.operators.roundCorners
import org.openrndr.shape.Shape

/**
 * Demonstrates how to convert a `Shape` with multiple `ShapeContour`s
 * (an outer contour and two holes) into a Processing `PShape`,
 * then converts it back to a `Shape`.
 *
 * The program renders both the original `Shape` and
 * the resulting `Shape` with translucency and a slight offset
 * so they can be visually compared.
 */
fun main() = application {
    program {
        val outlineRect = drawer.bounds.offsetEdges(-100.0)
        val s = Shape(
            listOf(
                outlineRect.contour.roundCorners(60.0),
                outlineRect.sub(0.25..0.45, 0.25..0.75).contour.reversed.roundCorners(30.0),
                outlineRect.sub(0.55..0.75, 0.25..0.75).contour.reversed.roundCorners(30.0),
            )
        )
        val ps = PShape(s)
        val rs = ps.toShape()
        extend {
            drawer.fill = ColorRGBa.PINK.opacify(0.5)
            drawer.shape(rs)

            drawer.translate(15.0, 15.0)
            drawer.shape(s)
        }
    }
}