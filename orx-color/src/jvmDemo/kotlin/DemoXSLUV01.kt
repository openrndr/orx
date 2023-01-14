// Visualize XSLUV color space by drawing a recursively subdivided arcs

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extensions.SingleScreenshot
import org.openrndr.extra.color.spaces.ColorXSLUVa
import org.openrndr.extra.color.spaces.toHSLUVa
import org.openrndr.math.Polar
import org.openrndr.shape.contour

fun main() {
    class Arc(val start: Double, val radius: Double, val length: Double, val height: Double) {
        fun split(offset: Double = 0.0): List<Arc> {
            val hl = length / 2.0
            return listOf(Arc(start, radius + offset, hl, height), Arc(start + hl, radius + offset, hl, height))
        }

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

    fun List<Arc>.split(depth: Int): List<Arc> = if (depth == 0) {
        this
    } else {
        this + flatMap { it.split(it.height) }.split(depth - 1)
    }

    application {
        configure {
            width = 720
            height = 720
        }

        program {
            val arcs = (0..4).map { Arc(it * 90.0 - 45.0, 50.0, 90.0, 50.0) }.split(5)

            extend {
                drawer.clear(ColorRGBa.GRAY)
                val color = ColorRGBa.RED
                val hc = color.toHSLUVa()
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
}