import org.openrndr.application
import org.openrndr.draw.BufferMultisample
import org.openrndr.draw.DrawPrimitive
import org.openrndr.extensions.Screenshots
import org.openrndr.extra.camera.Camera2D
import org.openrndr.extra.camera.Orbital
import org.openrndr.extra.fx.Post
import org.openrndr.extra.fx.blur.ApproximateGaussianBlur
import org.openrndr.extra.meshgenerators.boxMesh
import org.openrndr.extra.shapes.primitives.grid
import org.openrndr.extra.viewbox.viewBox
import org.openrndr.shape.Rectangle

/**
 * Demonstrates how to draw multiple view boxes. The first two feature
 * interactive 2D cameras, the third one uses an Orbital 3D camera.
 * All three can be controlled with the mouse wheel and buttons.
 *
 * The `shouldDraw` viewBox variable is used to avoid re-rendering the view
 * unnecessarily when the camera has not changed.
 *
 */
fun main() = application {
    configure {
        width = 720
        height = 720
    }
    program {

        val grid = drawer.bounds.grid(2, 1).flatten()

        val viewBoxes = grid.map {
            viewBox(it) {
                var radius = 100.0
                gestures.pinchUpdated.listen {
                    radius *= it.scale
                }
                extend {
                    drawer.circle(drawer.bounds.center, radius)
                }
            }
        }

        extend {
            for (viewBox in viewBoxes) {
                viewBox.draw()
            }
        }

    }
}
