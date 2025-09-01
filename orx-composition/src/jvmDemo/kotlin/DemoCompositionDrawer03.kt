import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.composition.ClipMode
import org.openrndr.extra.composition.composition
import org.openrndr.extra.composition.drawComposition
import org.openrndr.extra.svg.saveToFile
import java.io.File

/**
 * Draws a composition using 3 circles and `ClipMode.REVERSE_DIFFERENCE`.
 *
 * A println() demonstrates that the result contains 3 shapes:
 * a complete circle, a moon-like shape, and a shape with two small black areas.
 *
 * One way to verify this is by saving the design as an SVG file and opening
 * it in vector editing software.
 *
 */
fun main() = application {
    program {
        val composition = drawComposition {
            fill = null
            clipMode = ClipMode.REVERSE_DIFFERENCE

            circle(width / 2.0 - 50.0, height / 2.0, 100.0)
            circle(width / 2.0 + 50.0, height / 2.0, 100.0)

            fill = ColorRGBa.BLACK
            circle(width / 2.0, height / 2.0, 100.0)
        }

        println(composition.findShapes().size)

        // save svg to a File
        //composition.saveToFile(File("/path/to/design.svg"))

        extend {
            drawer.clear(ColorRGBa.PINK)
            drawer.composition(composition)
        }
    }
}
