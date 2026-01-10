import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.gcode.BasicGrblGenerator
import org.openrndr.extra.gcode.LayerMode
import org.openrndr.extra.gcode.Plot
import org.openrndr.math.Vector2

/**
 * A minimal example of how to use the [Plot].
 *
 * The Plot is set up to A4 Portrait paper, to generate grbl compatible g-code, to write each layer to a separate file
 * and to export the g-code to `/tmp`.
 *
 * The default layer can be drawn to with the `draw` block.
 * Additional named layers can be created with `layer` block.
 *
 * The application window shows a preview of the plot.
 * In this case a black rectrangle 1cm from the paper edges and 9 circles with radii from 10mm to 90mm.
 *
 * Pressing `g` will write two files to the `/tmp` directory.
 * Note that setting the stroke will not affect the generated g-code.
 * It could be a hint to what pen color is used to draw each layer.
 *
 * This does not use the olive orx to keep the example minimal. But using a program with live reloading,
 * you can quickly preview your plot.
 */
fun main() = application {
    configure {
        width = 800
        height = 800
    }

    program {
        extend(Plot(dimensions = Vector2(210.0, 297.0))) {
            generator = BasicGrblGenerator()

            // Export each layer to a separate file
            layerMode = LayerMode.MULTI_FILE

            // Set output files to be exported to /tmp
            // Press "g" t export g-code.
            folder = "/tmp"

            draw {
                // Rectangle in the default layer
                rectangle(docBounds.offsetEdges(-10.0))
            }

            layer("circles") {
                // Stroke changes do not affect the generated g-code
                stroke = ColorRGBa.PINK
                strokeWeight = .5
                (10..90 step 10).forEach { r ->
                    circle(docBounds.center, r.toDouble())
                }
            }
        }
    }
}