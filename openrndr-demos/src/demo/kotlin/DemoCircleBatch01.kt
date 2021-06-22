import org.openrndr.application
import org.openrndr.color.ColorRGBa

/*
This demo just verifies that drawing a single circle still works with revamped circle drawing code
 */
suspend fun main() = application {
    program {
        extend {
            drawer.clear(ColorRGBa.GRAY)
            drawer.fill = ColorRGBa.PINK
            drawer.stroke = ColorRGBa.WHITE
            drawer.strokeWeight = 2.0
            drawer.circle(100.0, 100.0, 50.0)
            drawer.rectangle(100.0, 100.0, 50.0, 50.0)
        }
    }
}