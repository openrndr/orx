import org.openrndr.application
import org.openrndr.color.ColorRGBa

import org.openrndr.extra.gcode.LayerMode
import org.openrndr.extra.gcode.Plot

import org.openrndr.extra.gcode.basicGrblSetup

import org.openrndr.math.Vector2

fun main() = application {
    configure {
        width = 800
        height = 800
    }

    program {


        // A4 Portrait
        extend(Plot(dimensions = Vector2(210.0, 297.0))) {
            generator = basicGrblSetup()

            // Export each layer to separate file
            layerMode = LayerMode.MULTI_FILE

            // Set output files to be exported to tmp
            // "g" to export g-code.
            folder = "/tmp"

            draw {
                // Rectangle in default layer
                rectangle(docBounds.offsetEdges(-10.0))
            }

            layer("circles") {
                // Stroke changes do not affect g-code
                stroke = ColorRGBa.PINK
                strokeWeight = .5
                (10..90 step 10).forEach { r ->
                    circle(docBounds.center, r.toDouble())
                }
            }
        }
    }
}