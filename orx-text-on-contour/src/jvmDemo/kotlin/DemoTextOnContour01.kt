import org.openrndr.application
import org.openrndr.draw.loadFont
import org.openrndr.extra.shapes.rectify.rectified
import org.openrndr.extra.textoncontour.textOnContour
import org.openrndr.shape.Circle

/**
 * Demo Functionality includes:
 * - Loading and applying a specific font (`IBMPlexMono-Regular`) with a size of 32.0.
 * - Creating a circular contour at the center of the screen with a radius of 200.0.
 * - Rendering text along the rectified circle's contour.
 * - Offsetting text positions, enabling repeated text rendering along the same contour.
 */
fun main() = application {
    configure {
        width = 720
        height = 720
    }
    program {
        extend {
            drawer.fontMap = loadFont("demo-data/fonts/IBMPlexMono-Regular.ttf", 32.0)
            val c = Circle(drawer.bounds.center, 200.0).contour.rectified()
            drawer.textOnContour("The wheels of the bus go round and round.", c)
            drawer.textOnContour("The wheels of the bus go round and round.", c, c.contour.length / 2.0)
        }
    }
}