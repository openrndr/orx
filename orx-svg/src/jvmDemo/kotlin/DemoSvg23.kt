import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.composition.CompositionFonts
import org.openrndr.extra.composition.CompositionInteraction
import org.openrndr.extra.composition.Length
import org.openrndr.extra.composition.composition
import org.openrndr.extra.composition.findShapes
import org.openrndr.extra.composition.strokeWeight
import org.openrndr.extra.svg.animation
import org.openrndr.extra.svg.evaluate
import org.openrndr.extra.svg.loadSVG

/**
 * Demonstrates transitions started by the program changing styles:
 *
 * - clicking a tile toggles its class `on`, the rule for `.tile.on` changes its fill, transform and stroke width
 * - pressing a key toggles the class `alt` of the rows, a descendant rule changes the fill of all their tiles
 * - every second `setProperty` sets the dash array of some tiles, like an inline style; the transitions in the style
 *   sheet do not cover it, so it changes at once
 * - pressing the space bar sets the stroke weight of the nodes directly, it is transitioned like the class changes
 */
fun main() = application {
    configure {
        width = 720
        height = 480
    }
    program {
        CompositionFonts.register("Plex", "demo-data/fonts/IBMPlexMono-Regular.ttf")
        val composition = loadSVG("orx-svg/src/jvmDemo/resources/svg/transitions.svg")
        val animation = composition.animation!!
        val interaction = CompositionInteraction(composition).apply { listenTo(mouse) }
        interaction.clicked.listen { animation.toggleClass(it.node, "on") }

        val rows = composition.findShapes().mapNotNull { it.parent }.distinct()
        var thick = false
        keyboard.keyDown.listen {
            if (it.name == "space") {
                // a change made directly to the styles of the nodes
                thick = !thick
                for (tile in composition.findShapes()) tile.style.strokeWeight = Length.Pixels(if (thick) 10.0 else 2.0)
            } else {
                rows.forEach { row -> animation.toggleClass(row, "alt") }
            }
        }

        var second = -1
        extend {
            if (seconds.toInt() != second) {
                second = seconds.toInt()
                val tiles = composition.findShapes()
                tiles.forEachIndexed { i, tile ->
                    animation.setProperty(tile, "stroke-dasharray", if ((i + second) % 3 == 0) "8 6" else null)
                }
            }
            composition.evaluate(seconds)
            drawer.clear(ColorRGBa.WHITE)
            drawer.composition(composition)
        }
    }
}
