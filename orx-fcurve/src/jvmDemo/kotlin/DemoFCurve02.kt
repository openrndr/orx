import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.fcurve.fcurve
import org.openrndr.math.Vector2

/**
 * Does everything that `DemoFCurve01.kt` does and
 * additionally renders both `FCurve` instances
 * as contours and visualizing the current
 * time as a vertical line.
 */
fun main() = application {
    configure {
        width = 720
        height = 720
    }
    program {
        val xposCurve = fcurve("M0 Q4,360,5,720")
        val xpos = xposCurve.sampler()
        val yposCurve = fcurve("M360 h5")
        val ypos = yposCurve.sampler()

        extend {
            val t = seconds.mod(5.0)
            drawer.circle(xpos(t), ypos(t), 100.0)

            // visualize both `FCurve` instances as contours
            drawer.stroke = ColorRGBa.PINK
            drawer.contours(xposCurve.contours(Vector2(720.0 / 5.0, -1.0), Vector2(0.0, height * 1.0)))
            drawer.contours(yposCurve.contours(Vector2(720.0 / 5.0, -1.0), Vector2(0.0, height * 1.0)))

            // show time advancing linearly as a vertical line,
            // crossing the window in 5 seconds
            drawer.translate(t * (720.0 / 5.0), 0.0)
            drawer.lineSegment(0.0, 0.0, 0.0, 720.0)
        }
    }
}
