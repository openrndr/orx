// Dynamic rectangle batches

import org.openrndr.application
import org.openrndr.color.ColorRGBa

fun main() = application {
    program {
        extend {
            drawer.clear(ColorRGBa.GRAY)
            drawer.fill = ColorRGBa.PINK
            drawer.stroke = ColorRGBa.WHITE
            drawer.strokeWeight = 2.0
            drawer.rectangles {
                for (y in 0 until height/20) {
                    for (x in 0 until width/20) {
                        rectangle(x * 20.0, y * 20.0, 10.0, 15.0, (x + y) * 10.0 + seconds*90.0)

                    }
                }
            }
        }
    }
}