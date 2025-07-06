package primitives

import org.openrndr.application
import org.openrndr.extra.noise.shapes.uniformSub
import org.openrndr.extra.shapes.primitives.alignToHorizontally
import org.openrndr.extra.shapes.primitives.alignToVertically
import org.openrndr.extra.shapes.primitives.distributeHorizontally
import org.openrndr.extra.shapes.primitives.fitHorizontally
import org.openrndr.extra.shapes.primitives.fitVertically
import org.openrndr.shape.Rectangle
import kotlin.math.cos
import kotlin.random.Random

fun main() = application {
    configure {
        width = 720
        height = 720
    }
    program {
        extend {
            val rs = (0 until 7).map { drawer.bounds.uniformSub(minWidth = 0.01, maxWidth = 0.1, random = Random(it)) }
                .fitHorizontally(drawer.bounds, gutter = 30.0 * mouse.position.y / height)
                .alignToVertically(drawer.bounds, cos(seconds) * 0.5 + 0.5)

            drawer.rectangles(rs)


            val rsh = (0 until 7).map { drawer.bounds.uniformSub(minHeight = 0.01, maxHeight = 0.1, random = Random(it)) }
                .fitVertically(drawer.bounds, gutter = 30.0 * mouse.position.y / height)
                .alignToHorizontally(drawer.bounds, cos(seconds) * 0.5 + 0.5)

            drawer.rectangles(rsh)
        }
    }
}
