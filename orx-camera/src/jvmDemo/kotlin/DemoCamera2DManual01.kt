import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.camera.Camera2DManual

/**
 * Demonstrates how to use `Camera2DManual` to have
 * some elements affected by an interactive 2D camera combined with
 * other elements not affected by it.
 *
 * In this example both PINK circles can be dragged, scaled and rotated
 * while the white circle in the middle is static.
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
