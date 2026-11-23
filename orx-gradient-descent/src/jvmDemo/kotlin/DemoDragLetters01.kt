import org.openrndr.WindowMultisample
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.loadFont
import org.openrndr.math.Vector2
import org.openrndr.shape.ShapeContour

/**
 * Parametric letters: the word HILT of five parameters that all its letters share, the width of a letter, the
 * height of the capitals, the weight of the stems, the height of the bars and the slant, all in pixels. Every corner
 * of every letter can be dragged, see [DragParameters], which changes the parameters that change least to bring it to
 * the mouse: dragging the inside of a stem makes the letters bolder, the top of a letter taller, a corner sideways
 * wider or more slanted, and the whole word follows.
 */
fun main() = application {
    configure {
        width = 720
        height = 720
        multisample = WindowMultisample.SampleCount(4)
    }
    program {
        val font = loadFont("demo-data/fonts/IBMPlexMono-Regular.ttf", 16.0)

        /** the outlines of the letters, of the parameters, from the left on the baseline, with y up */
        fun outlines(w: Double, h: Double, s: Double, bar: Double, slant: Double): List<List<Vector2>> {
            val b0 = bar - s / 2
            val b1 = bar + s / 2
            val letterH = listOf(
                0.0 to 0.0, s to 0.0, s to b0, w - s to b0, w - s to 0.0, w to 0.0,
                w to h, w - s to h, w - s to b1, s to b1, s to h, 0.0 to h
            )
            val letterI = listOf(0.0 to 0.0, s to 0.0, s to h, 0.0 to h)
            val letterL = listOf(0.0 to 0.0, 0.8 * w to 0.0, 0.8 * w to s, s to s, s to h, 0.0 to h)
            val c = w / 2
            val letterT = listOf(
                c - s / 2 to 0.0, c + s / 2 to 0.0, c + s / 2 to h - s, w to h - s,
                w to h, 0.0 to h, 0.0 to h - s, c - s / 2 to h - s
            )
            val letters = listOf(letterH to w, letterI to s, letterL to 0.8 * w, letterT to w)
            val gap = 0.4 * s + 20.0
            // the word starts at a fixed place on a fixed baseline, slanted around it: centered, every corner would
            // move with every parameter
            var x = -300.0
            return letters.map { (letter, width) ->
                val left = x
                x += width + gap
                // on the screen y is down
                letter.map { (px, py) -> Vector2(left + px + slant * py / h, 120.0 - py) }
            }
        }

        /** the parameters kept such that the letters stay letters */
        fun outlines(x: DoubleArray): List<List<Vector2>> {
            val s = x[2].coerceIn(4.0, 120.0)
            val w = x[0].coerceAtLeast(2 * s + 10.0)
            val h = x[1].coerceAtLeast(2 * s + 10.0)
            return outlines(w, h, s, x[3].coerceIn(s, h - s), x[4])
        }

        val drag = extend(DragParameters(doubleArrayOf(140.0, 200.0, 30.0, 100.0, 0.0), drawer.bounds.center) { x ->
            outlines(x).flatten()
        })
        drag.damping = 0.05
        extend {
            drawer.translate(drag.origin)
            drawer.fill = ColorRGBa.WHITE
            drawer.stroke = null
            drawer.contours(outlines(drag.parameters).map { ShapeContour.fromPoints(it, closed = true) })
            drag.drawPoints(drawer, ColorRGBa.PINK.opacify(0.8), ColorRGBa.PINK)

            val (w, h, s, bar, slant) = drag.parameters
            drawer.fontMap = font
            drawer.fill = ColorRGBa.WHITE.opacify(0.7)
            drawer.text(
                "width ${w.toInt()}  height ${h.toInt()}  weight ${s.toInt()}  bar ${bar.toInt()}  slant ${slant.toInt()}",
                -340.0, 340.0
            )
        }
    }
}
