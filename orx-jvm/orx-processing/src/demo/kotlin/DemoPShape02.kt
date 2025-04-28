import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.processing.PShape
import org.openrndr.extra.processing.toShape
import org.openrndr.extra.shapes.primitives.regularStarRounded

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
        val rc = ps.toShape()
        extend {
            drawer.fill = ColorRGBa.PINK.opacify(0.5)
            drawer.shape(rc)
            drawer.translate(15.0, 15.0)
            drawer.contour(c)
        }
    }
}