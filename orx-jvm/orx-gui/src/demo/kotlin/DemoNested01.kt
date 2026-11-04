import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.parameters.*
import org.openrndr.shape.Circle
import kotlin.math.max

/**
 * Demonstrates nested parameters. A property annotated with `@NestedParameter` holds an object with
 * annotated parameters of its own; the GUI shows them as a group inside the compartment of the
 * outer object. Click the header of a group to collapse or expand it.
 *
 * The nested groups `fill` and `stroke` both have a `color` parameter, nested parameters are
 * stored and saved per group so they do not clash.
 *
 * `stroke` nests a `style` group of its own. Its property is a `var` of an interface type: press
 * "next style" (or the space bar) to assign a style of another class, and the GUI replaces the
 * controls of the group with those of the new style.
 */
class Fill {
    @ColorParameter("color", order = 0)
    var color = ColorRGBa.PINK

    @DoubleParameter("opacity", 0.0, 1.0, order = 1)
    var opacity = 1.0
}

interface StrokeStyle {
    fun draw(drawer: Drawer, circle: Circle)
}

class Solid : StrokeStyle {
    override fun draw(drawer: Drawer, circle: Circle) {
        drawer.fill = null
        drawer.circle(circle)
    }
}

class Dashed : StrokeStyle {
    @DoubleParameter("length", 1.0, 40.0, order = 0)
    var length = 10.0

    @DoubleParameter("duty", 0.1, 0.9, order = 1)
    var duty = 0.5

    override fun draw(drawer: Drawer, circle: Circle) {
        val n = max(1, (circle.contour.length / length).toInt())
        drawer.contours((0 until n).map { circle.contour.sub(it / n.toDouble(), (it + duty) / n) })
    }
}

class Dotted : StrokeStyle {
    @IntParameter("count", 3, 100, order = 0)
    var count = 24

    @DoubleParameter("size", 1.0, 20.0, order = 1)
    var size = 6.0

    override fun draw(drawer: Drawer, circle: Circle) {
        drawer.fill = drawer.stroke
        drawer.stroke = null
        drawer.circles((0 until count).map { circle.contour.position(it / count.toDouble()) }, size)
    }
}

class Stroke {
    @ColorParameter("color", order = 0)
    var color = ColorRGBa.WHITE

    @DoubleParameter("weight", 0.0, 20.0, order = 1)
    var weight = 4.0

    @NestedParameter("style", order = 2)
    var style: StrokeStyle = Dashed()

    @ActionParameter("next style", order = 3)
    fun nextStyle() {
        style = when (style) {
            is Solid -> Dashed()
            is Dashed -> Dotted()
            else -> Solid()
        }
    }
}

@Description("Circle")
class CircleSettings {
    @DoubleParameter("radius", 10.0, 200.0, order = 0)
    var radius = 120.0

    @NestedParameter("fill", order = 1)
    val fill = Fill()

    @NestedParameter("stroke", order = 2)
    val stroke = Stroke()
}

fun main() = application {
    configure {
        width = 720
        height = 600
    }
    program {
        val gui = GUI()
        gui.compartmentsCollapsedByDefault = false
        val settings = CircleSettings()
        gui.add(settings)

        keyboard.keyDown.listen {
            if (it.name == "spacebar") {
                settings.stroke.nextStyle()
            }
        }

        extend(gui)
        extend {
            drawer.clear(ColorRGBa.GRAY.shade(0.25))
            val circle = Circle(width * 0.6, height * 0.5, settings.radius)
            drawer.fill = settings.fill.color.opacify(settings.fill.opacity)
            drawer.stroke = null
            drawer.circle(circle)
            drawer.stroke = settings.stroke.color
            drawer.strokeWeight = settings.stroke.weight
            settings.stroke.style.draw(drawer, circle)
        }
    }
}
