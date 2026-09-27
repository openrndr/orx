import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.marchingsquares.findContours
import org.openrndr.math.Vector2

/**
 * A simple demonstration of using the `findContours` method provided by `orx-marching-squares`.
 *
 * `findContours` lets one generate contours by providing a mathematical function to be
 * sampled within the provided area and with the given cell size. Contours are generated
 * between the areas in which the function returns positive and negative values.
 *
 * In this example, the `f` function returns the distance of a point to the center of the window minus 200.0.
 * Therefore, sampled locations which are less than 200 pixels away from the center return
 * negative values and all others return positive values, effectively generating a circle of radius 200.0.
 *
 * Try increasing the cell size to see how the precision of the circle reduces.
 *
 * The circular contour created in this program has over 90 segments. The number of segments depends on the cell
 * size, and the resulting radius.
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
            fun f(v: Vector2) = v.distanceTo(drawer.bounds.center) - 200.0
            val contours = findContours(::f, drawer.bounds, 16.0)
            drawer.fill = null
            drawer.contours(contours)
        }
    }
}
