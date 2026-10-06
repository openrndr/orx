import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.color.presets.DODGER_BLUE
import org.openrndr.extra.color.spaces.OKHSV
import org.openrndr.extra.color.tools.shiftHue
import org.openrndr.extra.marchingsquares.findContours
import org.openrndr.extra.marchingsquares.findContoursAdaptive
import org.openrndr.extra.marchingsquares.findContoursAdaptiveMarchingSquares
import org.openrndr.extra.shapes.primitives.grid
import org.openrndr.math.Vector2
import org.openrndr.shape.Shape
import kotlin.math.cos

/**
 * Demonstrates three different marching squares methods:
 * - [findContours](https://orx.openrndr.org/orx-marching-squares/org.openrndr.extra.marchingsquares/find-contours.html) (dodger-blue, top-left)
 * - [findContoursAdaptive](https://orx.openrndr.org/orx-marching-squares/org.openrndr.extra.marchingsquares/find-contours-adaptive.html) (pink-red, top-right)
 * - [findContoursAdaptiveMarchingSquares](https://orx.openrndr.org/orx-marching-squares/org.openrndr.extra.marchingsquares/find-contours-adaptive-marching-squares.html) (acid-green, bottom-left)
 *
 * One can appreciate that the animated effect of the adaptive methods is smoother than what the hplain
 * [findContours] provides.
 */
fun main() = application {
    configure {
        width = 720
        height = 720
    }
    program {
        extend {
            val grid = drawer.bounds.grid(2, 2, gutterX = 10.0, gutterY = 10.0, marginX = 10.0, marginY = 10.0).flatten()

            fun f(v: Vector2) = cos(seconds + (v.distanceTo(drawer.bounds.center) * 0.1))

            val contours = findContours(::f, grid[0], 16.0)
            val shape = Shape(contours)

            val contoursAdaptive = findContoursAdaptive(::f, grid[1], 6)
            val shapeAdaptive = Shape(contoursAdaptive)

            val contoursAMC = findContoursAdaptiveMarchingSquares(::f, grid[2], 6)
            val shapeAMC = Shape(contoursAMC)

            drawer.fill = ColorRGBa.DODGER_BLUE
            drawer.shape(shape)

            drawer.fill = ColorRGBa.DODGER_BLUE.shiftHue<OKHSV>(120.0)
            drawer.shape(shapeAdaptive)

            drawer.fill = ColorRGBa.DODGER_BLUE.shiftHue<OKHSV>(240.0)
            drawer.shape(shapeAMC)
        }
    }
}
