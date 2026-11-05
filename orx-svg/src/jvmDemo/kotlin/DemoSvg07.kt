import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.composition.composition
import org.openrndr.extra.svg.loadSVG
import kotlin.math.sin

/**
 * Demonstrates SVG patterns: content that is drawn in a tile which repeats.
 *
 * - top row: a checkerboard in user space that continues across a rectangle and a circle, and four by four tiles
 *   over the bounds of an ellipse
 * - middle row: content in bounds units, a view box fitted into the tile and stripes turned by a pattern transform
 * - bottom row: a pattern with gradients in it, a pattern that inherits its content and is scaled, and a pattern
 *   stroke
 *
 * The tiles are drawn to textures at the scale they are shown at. Press space to zoom in and out: the tiles are drawn
 * again when the scale changes enough, so they stay sharp.
 */
fun main() = application {
    configure {
        width = 720
        height = 480
    }
    program {
        val composition = loadSVG("orx-svg/src/jvmDemo/resources/svg/patterns.svg")
        var zooming = false
        keyboard.keyDown.listen {
            if (it.name == "spacebar") zooming = !zooming
        }
        extend {
            drawer.clear(ColorRGBa.WHITE)
            if (zooming) {
                val zoom = 1.0 + 1.5 * (0.5 - 0.5 * sin(seconds * 0.8 + Math.PI / 2.0))
                drawer.translate(width / 2.0, height / 2.0)
                drawer.scale(zoom)
                drawer.translate(-width / 2.0, -height / 2.0)
            }
            drawer.composition(composition)
        }
    }
}
