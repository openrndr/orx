import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.circleBatch

/*
This program demonstrates creating "pre-baked" batches of circles. Batches can have varying fill, stroke and
strokeWeight settings.

Batches are (currently) static but stored in GPU memory. Batches are fast to draw.
 */

fun main() = application {
    program {

        val batch = drawer.circleBatch {
            this.fill = ColorRGBa.PINK
            for (i in 0 until 100) {
                this.strokeWeight = Math.random() * 5.0
                this.circle(Math.random() * width, Math.random() * height, 50.0 * Math.random()  + 50.0 )
            }
        }

        extend {
            drawer.clear(ColorRGBa.GRAY)
            drawer.circles(batch)
        }
    }
}