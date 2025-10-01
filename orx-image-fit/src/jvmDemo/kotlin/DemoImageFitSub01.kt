import org.openrndr.application
import org.openrndr.draw.loadImage
import org.openrndr.extra.imageFit.imageFitSub

import org.openrndr.extra.noise.shapes.uniformSub
import org.openrndr.extra.shapes.primitives.grid
import kotlin.random.Random

/**
 * Demonstrates the `imageFitSub()` method, which allows specifying not only a target `Rectangle`,
 * but also a source `Rectangle`, which is used to set the area of the original image we want to fit
 * into the target.
 *
 * The program also demonstrates the `Rectangle.uniformSub` method, which returns a random sub-rectangle
 * taking into consideration the minimum and maximum width and height arguments.
 *
 * Notice the trick used to generate unique random results changing only once per second by using
 * the current seconds as an integer seed.
 */
fun main() = application {
    configure {
        width = 720
        height = 720
    }
    program {
        val image = loadImage("demo-data/images/image-001.png")
        extend {
            val grid = drawer.bounds.grid(5, 5).flatten()
            val r = Random(seconds.toInt())
            for (cell in grid) {
                drawer.imageFitSub(
                    image,
                    image.bounds.uniformSub(0.25, 0.75, 0.25, 0.75, random = r),
                    cell
                )
            }
        }
    }
}