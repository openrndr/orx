import org.openrndr.application
import org.openrndr.extra.processing.PShape
import org.openrndr.extra.processing.toShape

fun main() = application {
    program {
        val c = drawer.bounds.offsetEdges(-100.0).shape
        val ps = PShape(c)
        val rc = ps.toShape()
        extend {
            drawer.shape(rc)
        }
    }
}