package primitives

import org.openrndr.application
import org.openrndr.extra.noise.shapes.uniformSub
import org.openrndr.extra.shapes.primitives.alignToVertically
import org.openrndr.extra.shapes.primitives.distributeHorizontally
import org.openrndr.shape.Rectangle
import kotlin.math.cos
import kotlin.random.Random

/** This function creates an interactive graphical application that displays a dynamic visual composition
 * of rectangles, which are generated and manipulated based on time and random parameters. The application
 * follows these steps:
 *
 * 1. Initializes a random generator seeded with the elapsed seconds since the start of the program.
 * 2. Creates a sequence of rectangles using the `uniformSub` function to generate random sub-rectangles
 *    within the bounding rectangle of the canvas.
 * 3. Distributes the generated rectangles horizontally within the canvas using the `distributeHorizontally` method.
 * 4. Aligns the rectangles vertically according to their position in relation to the bounding rectangle
 *    and a dynamic anchor point derived from the cosine of elapsed time.
 * 5. Renders the rectangles on the canvas in the output window.
 */
fun main() {
    application {
        program {
            extend {
                val random = Random(seconds.toInt())
                val rs = (0 until 7).map { drawer.bounds.uniformSub(minWidth = 0.01, maxWidth = 0.1, random = random) }
                    .distributeHorizontally(drawer.bounds)
                    .alignToVertically(drawer.bounds, cos(seconds) * 0.5 + 0.5)

                drawer.rectangles(rs)
            }
        }
    }
}