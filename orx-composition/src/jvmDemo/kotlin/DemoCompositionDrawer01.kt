import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.composition.composition
import org.openrndr.extra.composition.drawComposition
import org.openrndr.extra.svg.saveToFile
import org.openrndr.extra.svg.toSVG
import org.openrndr.math.Vector2
import java.io.File

fun main() {
    application {
        program {
            val composition = drawComposition {
                val layer = group {
                    fill = ColorRGBa.PINK
                    stroke = ColorRGBa.BLACK
                    strokeWeight = 10.0
                    circle(Vector2(width / 2.0, height / 2.0), 100.0)
                    circle(Vector2(200.0, 200.0), 50.0)
                }
                // demonstrating how to set custom attributes on the CompositionNode
                // these are stored in SVG
                //layer.id = "Layer_2"
                //layer.attributes["inkscape:label"] = "Layer 1"
                //layer.attributes["inkscape:groupmode"] = "layer"
            }

            // print the svg to the console
            println(composition.toSVG())

            // save svg to a File
            //composition.saveToFile(File("/path/to/design.svg"))

            extend {
                drawer.clear(ColorRGBa.WHITE)

                // draw the composition to the screen
                drawer.composition(composition)
            }
        }
    }
}