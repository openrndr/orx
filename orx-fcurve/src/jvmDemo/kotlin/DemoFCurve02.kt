import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.fcurve.fcurve
import org.openrndr.math.Vector2

fun main() {
    application {
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
                drawer.circle(xpos(seconds.mod(5.0)), ypos(seconds.mod(5.0)), 100.0)
                drawer.stroke = ColorRGBa.PINK
                drawer.contours(xposCurve.contours(Vector2(720.0 / 5.0, -1.0), Vector2(0.0, height * 1.0)))
                drawer.contours(yposCurve.contours(Vector2(720.0 / 5.0, -1.0), Vector2(0.0, height * 1.0)))
                drawer.translate(seconds.mod(5.0)*(720.0/5.0), 0.0)
                drawer.lineSegment(0.0, 0.0, 0.0, 720.0)
            }
        }
    }
}