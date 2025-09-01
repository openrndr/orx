import org.openrndr.application
import org.openrndr.extra.camera.Camera2D
import org.openrndr.extra.viewbox.viewBox
import org.openrndr.math.transforms.transform
import org.openrndr.shape.Rectangle

/**
 * Demonstrates how to create a viewBox with an interactive 2D camera and
 * display it multiple times.
 *
 * Instead of calling the viewBox's `.draw()` method multiple times,
 * we call its `.update()` method once, then draw its `.result`
 * repeatedly, in a grid of 4 columns and 4 rows.
 *
 * The camera's initial rotation and scaling are specified as a transformation matrix.
 * To control the camera use the mouse wheel and buttons on the top-left view.
 */
fun main() = application {
    configure {
        width = 720
        height = 720
    }
    program {
        val vbx = viewBox(Rectangle(0.0, 0.0, 200.0, 200.0)) {
            extend(Camera2D()) {
                // Set the initial view for the camera
                view = transform {
                    rotate(30.0)
                    scale(2.0)
                }
            }
            extend {
                drawer.rectangle(20.0, 20.0, 100.0, 100.0)
            }
        }

        extend {
            vbx.update()
            for (j in 0 until 4) {
                for (i in 0 until 4) {
                    drawer.image(vbx.result, j * 200.0, i * 200.0)
                }
            }
        }
    }
}
