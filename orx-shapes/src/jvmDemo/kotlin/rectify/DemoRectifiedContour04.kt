package rectify
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.shapes.rectify.rectified
import org.openrndr.shape.Circle
import org.openrndr.shape.Segment2D

fun main() = application {
    configure {
        width = 720
        height = 720
    }
    program {
        val c = Circle(drawer.bounds.center, 50.0).contour
        val rc = c.rectified()
        val normals = List(200) {
            val t = it / 200.0
            val p = rc.position(t)
            val n = rc.normal(t)
            Segment2D(p, p + n * 200.0)
        }
        extend {
            drawer.clear(ColorRGBa.WHITE)
            drawer.segments(normals)
        }
    }
}