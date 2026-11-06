import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.composition.CompositionFonts
import org.openrndr.extra.composition.CompositionInteraction
import org.openrndr.extra.composition.Paint
import org.openrndr.extra.composition.ShapeNode
import org.openrndr.extra.composition.TextNode
import org.openrndr.extra.composition.composition
import org.openrndr.extra.composition.findAll
import org.openrndr.extra.composition.findGroups
import org.openrndr.extra.composition.stroke
import org.openrndr.extra.svg.loadSVG

/**
 * Demonstrates interaction with a composition, and text along paths and in vertical lines:
 *
 * - shapes are hit like in SVG: on their fill, with its fill rule, and on their stroke; the shape under the mouse is
 *   outlined, clicking a shape shows its id, clicking the link shows where it leads
 * - text on the right side of a path, and along a path given in the path attribute of textPath
 * - vertical text, turned sideways and upright
 */
fun main() = application {
    configure {
        width = 720
        height = 480
    }
    program {
        CompositionFonts.register("Plex", "demo-data/fonts/IBMPlexMono-Regular.ttf")
        val composition = loadSVG("orx-svg/src/jvmDemo/resources/svg/interactive.svg")
        val status = composition.root.findGroups().first { it.id == "status" }
            .findAll { it is TextNode }.single() as TextNode

        val interaction = CompositionInteraction(composition)
        interaction.listenTo(mouse)
        // the shape under the mouse gets a white outline
        val strokes = mutableMapOf<ShapeNode, Paint>()
        interaction.entered.listen { event ->
            (event.node as? ShapeNode)?.let { strokes[it] = it.style.stroke; it.stroke = ColorRGBa.WHITE }
        }
        interaction.exited.listen { event ->
            (event.node as? ShapeNode)?.let { shape -> strokes.remove(shape)?.let { shape.style.stroke = it } }
        }
        interaction.clicked.listen { event ->
            status.text = event.link?.let { "a link to $it" } ?: "clicked ${event.node.id} at ${event.point.x.toInt()}, ${event.point.y.toInt()}"
        }

        extend {
            drawer.clear(ColorRGBa.WHITE)
            drawer.composition(composition)
        }
    }
}
