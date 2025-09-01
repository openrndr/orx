import difference.differencing
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.delegatemagic.aggregation.aggregating
import org.openrndr.extra.delegatemagic.aggregation.maxMag
import org.openrndr.extra.delegatemagic.tracking.tracking
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.parameters.DoubleParameter
import org.openrndr.math.Vector2

fun main() = application {
    program {
        val gui = GUI()

        val state = object {
            @DoubleParameter("radius", 0.0, 200.0)
            var radius = 100.0

            val difference by differencing(::radius)
            val differenceHistory by tracking(::difference)
            val differenceMax by aggregating(::differenceHistory) {
                it.maxMag()
            }
        }

        gui.add(state, "state")

        extend(gui)
        extend {
            drawer.circle(drawer.bounds.center, state.radius)
            drawer.stroke = ColorRGBa.GREEN
            drawer.lineSegment(drawer.bounds.center, drawer.bounds.center + Vector2(state.difference, 0.0))
            drawer.translate(0.0, 4.0)
            drawer.stroke = ColorRGBa.BLUE
            drawer.lineSegment(drawer.bounds.center, drawer.bounds.center + Vector2(state.differenceMax, 0.0))
        }
    }
}
