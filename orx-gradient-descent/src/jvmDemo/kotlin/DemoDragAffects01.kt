import org.openrndr.WindowMultisample
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.math.Vector2

/**
 * Three lines of the same kind, of 12 parameters, the ends of the lines, of which [DragParameters.affects] limits
 * what dragging changes: the ends of the white line move freely; of the pink line only the start moves, its end
 * stays put; the blue line's start only moves sideways and its end only up and down.
 */
fun main() = application {
    configure {
        width = 720
        height = 720
        multisample = WindowMultisample.SampleCount(4)
    }
    program {
        val colors = listOf(ColorRGBa.WHITE, ColorRGBa.PINK, ColorRGBa.fromHex("#5ab0ff"))
        // for every line its start x and y, then its end x and y
        val parameters = doubleArrayOf(
            -200.0, -150.0, 200.0, -150.0,
            -200.0, 0.0, 200.0, 0.0,
            -200.0, 150.0, 200.0, 150.0
        )
        val drag = extend(DragParameters(parameters, drawer.bounds.center) { x ->
            (0 until 6).map { Vector2(x[2 * it], x[2 * it + 1]) }
        })
        drag.affects = { point ->
            when (point) {
                // the pink line: its start, not its end
                2 -> intArrayOf(4, 5)
                3 -> intArrayOf()
                // the blue line: the x of its start, the y of its end
                4 -> intArrayOf(8)
                5 -> intArrayOf(11)
                else -> null
            }
        }
        extend {
            drawer.translate(drag.origin)
            val points = drag.points(drag.parameters)
            drawer.strokeWeight = 3.0
            for (line in 0 until 3) {
                drawer.stroke = colors[line]
                drawer.lineSegment(points[2 * line], points[2 * line + 1])
            }
            drawer.strokeWeight = 1.0
            drag.drawPoints(drawer)
        }
    }
}
