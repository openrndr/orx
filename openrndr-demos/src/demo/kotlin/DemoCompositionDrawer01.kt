import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.math.Vector2
import org.openrndr.shape.drawComposition
import org.openrndr.svg.svgNamespaceInkscape
import org.openrndr.svg.toSVG
import org.openrndr.svg.writeSVG

fun main() {
    application {
        program {
            extend {
                drawer.clear(ColorRGBa.WHITE)

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
                    layer.id = "Layer_2"
                    layer.attributes["inkscape:label"] = "Layer 1"
                    layer.attributes["inkscape:groupmode"] = "layer"
                }
                // draw the composition to the screen
                drawer.composition(composition)

                // print the svg to the console
                println(composition.toSVG(namespaces = listOf(svgNamespaceInkscape)))
            }
        }
    }
}