import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.shape.Circle

/**
 * a simple demo that tests heavy stroke weights on tiny geometry
 *
 * This was made to assist in resolving https://github.com/openrndr/openrndr/issues/164
 */
fun main() = application {
    program {
        val c = Circle(200.0, 200.0, 10.0).contour
        extend {
            drawer.strokeWeight = mouse.position.y
            drawer.stroke = ColorRGBa.PINK
            drawer.contour(c)
        }
    }
}