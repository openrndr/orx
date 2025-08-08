import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.camera.Camera2DManual

/**
 * Demonstrate the use of `Camera2DManual` for manual camera control.
 *
 * The application is configured with a 720x720 window size. Within the program, a custom camera (`Camera2DManual`)
 * is initialized and used to create isolated drawing scopes. The `isolated` method is used to overlay different
 * drawing operations while maintaining individual camera states, ensuring proper transformations for specific elements.
 *
 * Three circles are drawn on the canvas: a small pink one, a medium white one and a large pink one.
 * Only the pink ones are affected by the interactive `Camera2DManual`, while the middle white circle is outside
 * the camera's isolated scope.
 */
fun main() = application {
    configure {
        width = 720
        height = 720
    }

    program {
        val camera = Camera2DManual()
        extend {
            camera.isolated {
                drawer.fill = ColorRGBa.PINK
                drawer.circle(drawer.bounds.center, 300.0)
            }

            drawer.circle(drawer.bounds.center, 200.0)

            camera.isolated {
                drawer.fill = ColorRGBa.PINK
                drawer.circle(drawer.bounds.center, 100.0)
            }
        }
    }
}
