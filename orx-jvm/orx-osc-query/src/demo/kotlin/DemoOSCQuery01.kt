import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.color.rgb
import org.openrndr.extra.oscquery.OSCQuery
import org.openrndr.extra.parameters.*
import org.openrndr.math.Vector2
import org.openrndr.shape.Circle

/**
 * Demonstrates how to control an OPENRNDR program over
 * [OSCQuery](https://github.com/Vidvox/OSCQueryProposal).
 *
 * A `settings` object is created using the same annotations used by `orx-gui`. Passing it to
 * [OSCQuery.add] exposes its parameters both as a discoverable JSON namespace over HTTP and as
 * OSC addresses that receive value updates.
 *
 * With the program running, point an OSCQuery client (or a browser) at
 * `http://<this-machine>:9000/` to inspect the namespace. This example publishes:
 *  - `/Settings/radius`  (float)
 *  - `/Settings/x`       (float)
 *  - `/Settings/y`       (float)
 *  - `/Settings/sides`   (int)
 *  - `/Settings/fill`    (color)
 *  - `/Settings/randomize` (trigger)
 *
 * Sending an OSC message to any of those addresses (UDP, port 9000) updates the sketch live.
 */
fun main() = application {
    configure {
        width = 800
        height = 800
    }
    program {
        var strokeColor = ColorRGBa.WHITE
        val settings = @Description("Settings") object {
            @DoubleParameter("radius", 0.0, 300.0, order = 10)
            var radius = 100.0

            @DoubleParameter("x", -300.0, 300.0, order = 20)
            var x = 0.0

            @DoubleParameter("y", -300.0, 300.0, order = 30)
            var y = 0.0

            @DoubleParameter("rotation", 0.0, 360.0, order = 35)
            var rotation = 0.0

            @IntParameter("sides", 3, 12, order = 40)
            var sides = 6

            @ColorParameter("fill", order = 50)
            var color = ColorRGBa.WHITE

            @ActionParameter("randomize", order = 60)
            fun randomize() {
                strokeColor = rgb(Math.random(), Math.random(), Math.random())
                println("randomize triggered over OSCQuery")
            }
        }

        val oscQuery = OSCQuery()
        oscQuery.add(settings)

        extend {
            val center = drawer.bounds.center + Vector2(settings.x, settings.y)
            val contour = Circle(Vector2.ZERO, settings.radius).contour.sampleEquidistant(settings.sides)
            drawer.fill = settings.color
            drawer.stroke = strokeColor
            drawer.strokeWeight = 8.0
            drawer.rotate(settings.rotation)
            drawer.translate(center)
            drawer.contour(contour)
        }
    }
}
