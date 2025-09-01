// Test negative widths & heights for drawing aligned rectangles.
// Also draw a circle with negative radius.
// All shapes should appear with a white 4-pixel border

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.shape.Rectangle

fun main() = application {
    program {
        val margin = 5.0
        val squareSize = 100.0

        extend {
            drawer.clear(ColorRGBa.GRAY)
            drawer.fill = ColorRGBa.PINK
            drawer.stroke = ColorRGBa.WHITE
            drawer.strokeWeight = 4.0

            // Top Left
            drawer.rectangle(margin, margin, squareSize, squareSize)
            // Top Right
            drawer.rectangle(width - margin, margin, -squareSize, squareSize)
            // Bottom Right
            drawer.rectangle(width - margin, height - margin, -squareSize, -squareSize)
            // Bottom Left
            drawer.rectangle(margin, height - margin, squareSize, -squareSize)

            // Circle with negative radius
            drawer.circle(drawer.bounds.center, -squareSize * 2)

            // Rectangles with the bottom right corner centered in the window
            drawer.rectangles(List(10) {
                Rectangle(
                    drawer.bounds.center,
                    -squareSize * 2 + it * 10,
                    -squareSize * 2 + it * 10
                )
            })
        }
    }
}