import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.marchingsquares.findContours
import org.openrndr.math.Vector2
import kotlin.math.PI
import kotlin.math.cos

/**
 * This Marching Square demonstration shows the effect of wrapping a distance function
 * within a cosine (or sine). These mathematical functions return values that periodically
 * alternate between negative and positive, creating nested contours as the distance increases.
 *
 * The `/ 100.0) * 2 * PI` part of the formula is only a scaling factor, more or less
 * equivalent to 0.06. Increasing or decreasing this value will change how close the generated
 * parallel curves are to each other.
 *
 */
fun main() = application {
    configure {
        width = 720
        height = 720
    }
    program {
        extend {
            drawer.clear(ColorRGBa.BLACK)
            drawer.stroke = ColorRGBa.PINK
            fun f(v: Vector2) = cos((v.distanceTo(drawer.bounds.center) / 100.0) * 2 * PI)
            val contours = findContours(::f, drawer.bounds.offsetEdges(-24.0), 16.0)
            drawer.fill = null
            drawer.contours(contours)
        }
    }
}
