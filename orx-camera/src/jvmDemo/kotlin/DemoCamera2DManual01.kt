import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.isolatedWithTarget
import org.openrndr.draw.loadFont
import org.openrndr.draw.renderTarget
import org.openrndr.extra.camera.Camera2D
import org.openrndr.extra.camera.Camera2DManual
import org.openrndr.math.Vector2
import org.openrndr.math.transforms.transform
import org.openrndr.shape.Rectangle
import kotlin.math.sin

/**
 * Demonstrate the use of `Camera2DManual` for manual camera control.
 *
 * The application is configured with a 720x720 window size. Within the program, a custom camera (`Camera2DManual`)
 * is initialized and used to create isolated drawing scopes. The `isolated` method is utilized to overlay different
 * drawing operations while maintaining individual camera states, ensuring proper transformations for specific elements.
 *
 * A pink circle is drawn at the center of the canvas with varying radii using isolated and non-isolated camera states.
 * The outermost and innermost circles are affected by the `Camera2DManual` isolated scope, while the middle circle
 * is outside of the camera's isolated scope, creating a layered visual effect.
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
