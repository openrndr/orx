import org.openrndr.application
import org.openrndr.extra.processing.PShape
import org.openrndr.extra.processing.toShape

/**
 * Demonstrates how to construct a Processing `PShape` out of an OPENRNDR
 * `Shape` instance, and how to convert a `PShape` back into a `Shape.
 *
 * The program renders a rectangular `Shape` after converting to PShape and back.
 *
 */
fun main() = application {
    program {
        val s = drawer.bounds.offsetEdges(-100.0).shape
        val ps = PShape(s)
        val rs = ps.toShape()
        extend {
            drawer.shape(rs)
        }
    }
}