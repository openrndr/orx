import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.composition.CompositionFonts
import org.openrndr.extra.composition.CompositionInteraction
import org.openrndr.extra.composition.composition
import org.openrndr.extra.svg.animation
import org.openrndr.extra.svg.evaluate
import org.openrndr.extra.svg.loadSVG

/**
 * Demonstrates animated SVG documents, driven by the time of the program and the mouse:
 *
 * - SMIL animation: a spinner turned by animateTransform, an arrow moving back and forth along a path with
 *   animateMotion, colors and rounded corners animated with animate
 * - a CSS animation with @keyframes
 * - buttons with :hover and :active rules, and transitions between their states
 * - animations that begin when the blue circle is clicked, one of them half a second later
 */
fun main() = application {
    configure {
        width = 720
        height = 480
    }
    program {
        CompositionFonts.register("Plex", "demo-data/fonts/IBMPlexMono-Regular.ttf")
        val composition = loadSVG("orx-svg/src/jvmDemo/resources/svg/animated.svg")
        // the pointer puts elements in the :hover and :active states, and begins animations with events
        val interaction = CompositionInteraction(composition).apply { listenTo(mouse) }
        composition.animation?.listenTo(interaction)

        extend {
            composition.evaluate(seconds)
            drawer.clear(ColorRGBa.WHITE)
            drawer.composition(composition)
        }
    }
}
