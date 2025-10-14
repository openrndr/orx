import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.isolatedWithTarget
import org.openrndr.draw.loadFont
import org.openrndr.draw.renderTarget
import org.openrndr.extra.camera.Camera2D
import org.openrndr.math.Vector2
import org.openrndr.math.transforms.transform
import org.openrndr.shape.Rectangle
import kotlin.math.sin

/**
 * #### Camera2D demo with static elements
 *
 * An approach for having certain elements not affected by the camera.
 * See DemoCamera2DManual01.kt for a new and simpler approach
 */
fun main() = application {
    program {
        // Create a renderTarget where to draw things. It will be controlled by the camera.
        val rt = renderTarget(width, height) {
            colorBuffer()
            depthBuffer()
        }
        // Create a camera and apply an initial transformation
        // so the origin is no longer in the top-left corner.
        val cam = Camera2D()
        cam.view *= transform {
            translate(width * 0.5, height * 1.0)
            rotate(45.0)
            scale(2.0)
        }

        // Add mouse listeners to the camera
        cam.setup(this)

        val font = loadFont("demo-data/fonts/IBMPlexMono-Regular.ttf", 50.0)

        extend {
            // Draw onto the renderTarget
            drawer.isolatedWithTarget(rt) {
                // Calling ortho required if the size differs from the window size
                ortho(rt)
                // Apply the current camera transformation
                view = cam.view
                // Clear render target
                clear(ColorRGBa.TRANSPARENT)
                // Draw the things affected by the camera (here a rectangle at the origin)
                rectangle(Rectangle.fromCenter(Vector2.ZERO, 200.0, 100.0 + sin(seconds) * 20.0))
            }

            drawer.clear(ColorRGBa.PINK)

            // Draw the renderTarget with the camera applied to it
            drawer.image(rt.colorBuffer(0))

            // Draw things not affected by the camera
            drawer.fontMap = font
            drawer.fill = ColorRGBa.PINK.shade(0.5)
            drawer.text("click and drag mouse", 50.0, 400.0)
            drawer.text("use mouse wheel", 50.0, 450.0)
        }
    }
}
