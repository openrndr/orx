import difference.differencing
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.delegatemagic.aggregation.aggregating
import org.openrndr.extra.delegatemagic.aggregation.maxMag
import org.openrndr.extra.delegatemagic.tracking.tracking
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.parameters.DoubleParameter
import org.openrndr.math.Vector2

/**
 * Demonstrates the use of the `differencing`, `tracking` and `aggregating` delegates.
 *
 * All three are used to track changes to a variable. Try changing the `radius` parameter
 * in the GUI.
 *
 * The `difference` between the previous value and the current one will be displayed as a red line
 * that starts in the center of the screen and grows right when the value increases, or left
 * when the value decreases. As soon as `radius` stops changing, the line becomes invisible
 * due to having a length of zero.
 *
 * `tracking` is used to keep track of recent values. The number of samples kept is passed
 * in its constructor. `tracking` can't be used directly, but it can be passed to an `aggregating`
 * delegate. In this case it is used to find the maximum value among the last 50 samples
 * and rendered as a blue line.
 *
 * Note that new values keep being added to `differenceHistory` and old values discarded.
 * Therefore, a large increase or decrease in `radius` followed lack of change will be visualized
 * as a blue line for a short while, until the large value gets replaced by newer values.
 * How long the large value is visible depends on the `length` parameter passed to `tracking`.
 */
fun main() = application {
    program {
        val gui = GUI()

        val state = object {
            @DoubleParameter("radius", 0.0, 200.0)
            var radius = 100.0

            val difference by differencing(::radius)
            val differenceHistory by tracking(::difference, 50)
            val differenceMax by aggregating(::differenceHistory) {
                it.maxMag()
            }
        }

        gui.add(state, "state")

        extend(gui)
        extend {
            drawer.circle(drawer.bounds.center, state.radius)

            drawer.strokeWeight = 2.0

            drawer.stroke = ColorRGBa.RED
            drawer.lineSegment(drawer.bounds.center, drawer.bounds.center + Vector2(state.difference, 0.0))

            drawer.translate(0.0, 4.0)

            drawer.stroke = ColorRGBa.BLUE
            drawer.lineSegment(drawer.bounds.center, drawer.bounds.center + Vector2(state.differenceMax, 0.0))
        }
    }
}
