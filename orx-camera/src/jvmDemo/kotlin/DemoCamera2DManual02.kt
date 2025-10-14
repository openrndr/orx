import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.camera.Camera2DManual
import org.openrndr.extra.noise.shapes.uniform
import org.openrndr.extra.noise.uniform
import org.openrndr.math.Vector2
import org.openrndr.math.transforms.transform
import org.openrndr.shape.Rectangle
import org.openrndr.shape.contains

/**
 * Demonstrate the use of `Camera2DManual` to independently translate, scale and rotate one contour
 * in a collection.
 *
 * When the mouse is clicked, the active contour is transformed using the camera view matrix,
 * then the camera is reset to its default state and whatever shape is under the mouse becomes
 * the new active contour.
 *
 * As the mouse is dragged or its wheel scrolled, the camera is updated, affecting
 * how the active contour is rendered.
 */
fun main() = application {
    configure {
        width = 720
        height = 720
    }

    program {
        val camera = Camera2DManual()
        // Create a mutable list of rectangular contours with random transformations
        // applied to them. This is the initial state for the contours.
        val contours = MutableList(16) {
            Rectangle.fromCenter(Vector2.ZERO, 85.0).contour.transform(
                transform {
                    translate(drawer.bounds.uniform())
                    scale(Double.uniform(0.5, 2.0))
                    rotate(Double.uniform(0.0, 360.0))
                }
            )
        }

        var activeContour = -1
        extend {
            // Draw all contours. The active contour is drawn in pink and
            // affected by the camera's transformations.
            contours.forEachIndexed { i, c ->
                if (i == activeContour) {
                    camera.isolated {
                        drawer.fill = ColorRGBa.PINK
                        drawer.contour(c)
                    }
                } else {
                    drawer.fill = ColorRGBa.GRAY
                    drawer.contour(c)
                }
            }
        }
        mouse.buttonDown.listen {
            // Apply the camera view matrix to the active contour
            if (activeContour >= 0) contours[activeContour] = contours[activeContour].transform(camera.view)

            // Reset the camera to its default state
            camera.defaults()
            camera.rotationCenter = it.position

            // Make the contour under the mouse the active contour
            activeContour = contours.indexOfLast { mouse.position in it }
        }
    }
}
