import org.openrndr.MouseButton
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.composition.composition
import org.openrndr.extra.composition.draw
import org.openrndr.extra.composition.drawComposition
import org.openrndr.math.Polar
import org.openrndr.math.Vector2
import kotlin.math.sin

/**
 * Demonstrates how to add content to and how to clear an existing Composition.
 *
 * A number of circles are added when the program starts.
 * Dragging the mouse button adds more circles.
 * Right-clicking the mouse clears the Composition.
 */
fun main() = application {
    program {
        val composition = drawComposition { }

        // initial Composition content
        repeat(360) {
            composition.draw {
                fill = ColorRGBa.WHITE
                val r = sin(it / 90.0) * 30 + 40
                circle(
                    drawer.bounds.center + Polar(it * 5.0, r * 2).cartesian,
                    r
                )
            }
        }

        extend {
            drawer.clear(ColorRGBa.PINK)
            drawer.composition(composition)
        }

        // To avoid drawing too many circles when dragging the mouse,
        // we require a minimum separation between them
        var lastPosition = Vector2.INFINITY
        val minSeparation = 10.0

        mouse.dragged.listen {
            if(it.position.distanceTo(lastPosition) > minSeparation) {
                composition.draw {
                    fill = ColorRGBa.WHITE
                    // the drag speed affects the radius
                    circle(it.position, 5.0 + it.dragDisplacement.length * 5.0)
                }
                lastPosition = it.position
            }
        }

        mouse.buttonDown.listen {
            if (it.button == MouseButton.RIGHT) {
                composition.clear()
            }
        }
    }
}
