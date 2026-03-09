import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.parameters.ColorParameter
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.DoubleParameter
import org.openrndr.extra.parameters.Vector2Parameter
import org.openrndr.math.Vector2

internal class ColoredCircle(
    @Vector2Parameter("position", 0.0, 720.0, 2, 10)
    var position: Vector2,
    @ColorParameter("color", 20)
    var color: ColorRGBa,
    @DoubleParameter("radius", 1.0, 300.0, 2, 30)
    var radius: Double
)

/**
 * Demonstrates how to add and remove object instances from a gui.
 *
 * The instances have three parameters: a 2D position, a color, and a radius.
 *
 * Click an empty area in the canvas to add visible circles.
 * Each circle is also added to the gui, making it possible to adjust its properties.
 *
 * Click an existing circle to remove it from the collection and from the gui.
 */
fun main() = application {
    configure {
        width = 720
        height = 720
    }
    program {
        val coloredCircles = mutableListOf<ColoredCircle>()

        val gui = GUI()
        gui.compartmentsCollapsedByDefault = false
        extend(gui)

        extend {
            // Render the colored circles
            coloredCircles.forEach {
                drawer.fill = it.color
                drawer.circle(it.position, it.radius)
            }
        }

        mouse.buttonDown.listen { ev ->
            // Ignore clicks inside the gui
            if(!ev.propagationCancelled) {
                // Check whether we clicked inside a circle
                val hit = coloredCircles.firstOrNull { ev.position.distanceTo(it.position) < it.radius }
                if(hit != null) {
                    // If we did, remove that circle from the collection and the gui
                    coloredCircles.remove(hit)
                    gui.remove(hit)
                } else {
                    // If we did not, add a new circle at the mouse location
                    val c = ColoredCircle(ev.position, ColorRGBa.PINK, 50.0)
                    coloredCircles.add(c)
                    gui.add(c)
                }
            }
        }
    }
}