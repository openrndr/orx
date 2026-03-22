import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.color.spaces.toOKHSVa
import org.openrndr.extra.color.statistics.deltaE76
import org.openrndr.math.Polar

/**
 * Demonstrates the use of the `.deltaE76()` method to
 * compute the CIE76 color difference (ΔE*76) between two colors.
 *
 * The method calculates the Euclidean distance between the two colors in the
 * LAB color space and returns it as a Double. If either of the colors is not
 * in LAB format, it is converted to LAB before computation.
 *
 * The `startColor` is a fully saturated color with an animated hue that
 * increases 36 degrees per second.
 *
 * The program renders 36 evenly spaced hues starting at `startColor`,
 * in steps of 10 degrees.
 *
 * For each hue, 100 steps are rendered as small circles using polar coordinates.
 * An `endColor` is calculated for each step. The step number controls the color's
 * saturation.
 *
 * The result is an animated effect featuring rotating, growing, and shrinking
 * lines rendered with colored gradients.
 */
fun main() = application {
    configure {
        width = 720
        height = 720
    }
    program {
        extend {
            drawer.clear(ColorRGBa.BLACK)
            drawer.fill = null
            drawer.stroke = ColorRGBa.WHITE.opacify(0.2)
            for (i in 10 until 270 step 10) {
                drawer.circle(drawer.bounds.center, i.toDouble())
            }

            drawer.stroke = null

            val startColor = ColorRGBa.RED.toOKHSVa().shiftHue(seconds * 36.0).toRGBa()
            drawer.circles {
                for (j in 99 downTo 0) {
                    for (i in 0 until 360 step 10) {
                        val endColor = startColor.toOKHSVa().shiftHue(i.toDouble()).saturate(j / 99.0).toRGBa()
                        val distance = endColor.deltaE76(startColor)
                        val p = Polar(seconds * 36.0 + i.toDouble(), distance).cartesian + drawer.bounds.center
                        fill = endColor
                        circle(p, 2.0)
                    }
                }
            }
        }
    }
}
