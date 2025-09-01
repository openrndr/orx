import org.openrndr.application
import org.openrndr.draw.loadImage
import org.openrndr.extra.imageFit.imageFitSub

import org.openrndr.extra.noise.shapes.uniformSub
import org.openrndr.extra.shapes.primitives.grid
import kotlin.random.Random

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