// Drawing points using a stored batch

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.pointBatch

fun main() = application {
    program {
        val storedBatch = drawer.pointBatch {
            for (y in 10 until height step 20) {
                for (x in 10 until width step 20) {
                    fill = ColorRGBa.PINK.shade(Math.random())
                    point(x * 1.0, y * 1.0)
                }
            }
        }
        extend {
            drawer.clear(ColorRGBa.GRAY)
            drawer.points(storedBatch)
        }
    }
}