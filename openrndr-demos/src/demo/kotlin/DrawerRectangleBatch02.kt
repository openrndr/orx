// Stored rectangle batches
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.rectangleBatch

fun main() = application {
    program {
        val batch = drawer.rectangleBatch {
            fill = ColorRGBa.PINK
            stroke = ColorRGBa.WHITE
            for (i in 0 until 1000) {
                strokeWeight = Math.random() * 5.0 + 1.0
                val rwidth = Math.random() * 40.0 + 4.0
                val rheight = Math.random() * 40.0 + 4.0
                val x = Math.random() * width
                val y = Math.random() * height
                rectangle(x, y, rwidth, rheight, Math.random() * 360.0)
            }
        }

        extend {
            drawer.clear(ColorRGBa.GRAY)
            drawer.fill = ColorRGBa.PINK
            drawer.stroke = ColorRGBa.WHITE
            drawer.strokeWeight = 2.0
            drawer.rectangles(batch)
        }
    }
}