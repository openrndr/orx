import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.composition.ClipMode
import org.openrndr.extra.composition.composition
import org.openrndr.extra.composition.drawComposition

/**
 * Demonstrates how to draw a Composition and how to use
 * `ClipMode.REVERSE_DIFFERENCE` to clip shapes.
 *
 * The first shape clips part of the second one away,
 * producing a shape that seems to be behind the first one.
 *
 * Without clipping, the second circle would cover part of the first one.
 */
fun main() = application {
    program {
        val composition = drawComposition {
            fill = null
            circle(width / 2.0, height / 2.0, 100.0)

            fill = ColorRGBa.BLACK
            clipMode = ClipMode.REVERSE_DIFFERENCE
            circle(width / 2.0 + 50.0, height / 2.0, 100.0)
        }

        extend {
            drawer.clear(ColorRGBa.PINK)
            drawer.composition(composition)
        }
    }
}
