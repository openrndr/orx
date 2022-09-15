import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.loadFont
import org.openrndr.extra.camera.Camera2D

/**
 * # Camera2D demo
 *
 * click and drag the mouse for panning, use the mouse wheel for zooming
 */
fun main() = application {
    program {
        val font = loadFont("demo-data/fonts/IBMPlexMono-Regular.ttf", 72.0)

        extend(Camera2D())
        extend {
            drawer.circle(drawer.bounds.center, 300.0)

            drawer.fontMap = font
            drawer.fill = ColorRGBa.PINK
            drawer.text("click and drag mouse", 50.0, 400.0)
            drawer.text("use mouse wheel", 50.0, 500.0)
        }
    }
}