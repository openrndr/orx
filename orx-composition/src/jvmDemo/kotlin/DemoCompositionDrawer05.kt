import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.composition.composition
import org.openrndr.extra.composition.drawComposition
import org.openrndr.math.Vector2
import org.openrndr.extra.svg.saveToFile
import java.io.File

/**
 * Demonstrates how to
 *
 * - Create a Composition with a group
 * - Add XML attributes so the group appears as a layer in Inkscape
 */
fun main() = application {
    program {
        val composition = drawComposition {
            val layer = group {
                fill = ColorRGBa.PINK
                stroke = ColorRGBa.BLACK
                strokeWeight = 10.0
                circle(Vector2(width / 2.0, height / 2.0), 100.0)
                circle(Vector2(200.0, 200.0), 50.0)
            }
            // Demonstrate how to set custom attributes on the `GroupNode`
            // These are stored in the SVG file.

            layer.id = "Layer_2"
            layer.attributes["inkscape:label"] = "Layer 1"
            layer.attributes["inkscape:groupmode"] = "layer"
        }

        // save svg to a File
        //composition.saveToFile(File("/path/to/design.svg"))

        extend {
            drawer.clear(ColorRGBa.WHITE)

            // draw the composition to the screen
            drawer.composition(composition)
        }
    }
}
