import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.color.spaces.ColorXSLUVa
import org.openrndr.math.Polar
import org.openrndr.shape.contour

/**
 * Visualize the XSLUV color space by drawing a recursively subdivided set of arcs.
 *
 * The provided `Arc` class provides a `contour` getter, which creates a "thick" arc with
 * its thickness defined by the `height` argument. This is created by two arcs and two
 * connecting lines.
 *
 * The mouse x coordinate controls the saturation, while the y coordinate controls the luminosity.
 * The two if-statements check whether the program is taking a screenshot (this happens when
 * it runs on GitHub actions) to set fixed saturation and luminosity values.
 */
fun main() = application {
    configure {
        width = 720
        height = 720
    }

    class Arc(val start: Double, val radius: Double, val length: Double, val height: Double) {
        /**
         * Splits the Arc in two equal parts with half the length of the original
         */
        fun split(offset: Double = 0.0): List<Arc> {
            val hl = length / 2.0
            return listOf(
                Arc(start, radius + offset, hl, height),
                Arc(start + hl, radius + offset, hl, height)
            )
        }

        /**
         * Return the contour of an arc with `height` thickness, by drawing an arc using `radius`,
         * then a line connecting to a returning arc using `radius + height`, and a final line to
         * close the contour.
         */
        val contour
            get() = contour {
                moveTo(Polar(start, radius).cartesian)
                arcTo(radius, radius, length, false, true, Polar(start + length, radius).cartesian)
                lineTo(Polar(start + length, radius + height).cartesian)
                arcTo(radius + height, radius + height, length, false, false, Polar(start, radius + height).cartesian)
                lineTo(anchor)
                close()
            }
    }

    /**
     * Create a list of `Arc` by recursively calling the `split` function until `depth` reaches 0.
     */
    fun List<Arc>.split(depth: Int): List<Arc> = if (depth == 0) {
        this
    } else {
        this + flatMap { it.split(it.height) }.split(depth - 1)
    }

    program {
        val arcs = (0..4).map {
            Arc(it * 90.0 - 45.0, 50.0, 90.0, 50.0)
        }.split(5)

        extend {
            drawer.clear(ColorRGBa.GRAY)
            drawer.stroke = ColorRGBa.BLACK
            drawer.strokeWeight = 1.0
            drawer.translate(drawer.bounds.center)
            val l = if (System.getProperty("takeScreenshot") == "true") 0.7 else mouse.position.y / height
            val s = if (System.getProperty("takeScreenshot") == "true") 1.0 else mouse.position.x / width
            for (arc in arcs) {
                val xsluv = ColorXSLUVa(arc.start + arc.length / 2.0, s, l, 1.0)
                drawer.fill = xsluv.toRGBa()
                drawer.contour(arc.contour)
            }
        }
    }
}
