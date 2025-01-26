import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.loadFont
import org.openrndr.extra.easing.Easing
import org.openrndr.extra.shapes.primitives.grid
import org.openrndr.math.Vector2
import org.openrndr.math.map

/**
 * # Visualizes Easing types as a graph and as motion.
 *
 * [grid] is used to layout graphs on rows and columns.
 *
 */
fun main() = application {
    configure {
        width = 1280
        height = 1080
    }
    program {
        val font = loadFont("demo-data/fonts/IBMPlexMono-Regular.ttf", 20.0)

        // grid `columns * rows` must be >= Easing.values().size
        val grid = drawer.bounds.grid(
            3, 11, 10.0, 10.0, 10.0, 10.0
        ).flatten()

        // make pairs of (easing function, grid rectangle)
        val pairs = Easing.entries.toTypedArray() zip grid

        extend {
            // ~4 seconds animation loop
            val animT = (frameCount % 240) / 60.0

            pairs.forEach { (easing, gridRect) ->

                // background rectangle
                drawer.stroke = null
                drawer.fill = ColorRGBa.WHITE.opacify(0.3)
                drawer.rectangle(gridRect)

                // graph
                drawer.stroke = ColorRGBa.PINK
                val points = List(40) {
                    val curveT = it / 39.0
                    gridRect.position(
                        curveT, easing.function(curveT, 1.0, -1.0, 1.0)
                    )
                }
                drawer.lineStrip(points)

                // label
                drawer.fill = ColorRGBa.WHITE
                drawer.stroke = null
                drawer.fontMap = font
                drawer.text(
                    easing.name,
                    // text position rounded for crisp font rendering
                    gridRect.position(0.02, 0.25).toInt().vector2
                )

                // animation
                drawer.fill = ColorRGBa.WHITE.opacify(
                    when {                         // 4-stage opacity
                        animT > 3.0 -> 0.0         // invisible
                        animT > 2.0 -> 3.0 - animT // fade-out
                        animT < 1.0 -> animT       // fade-in
                        else -> 1.0                // visible
                    }
                )
                // move only while visible (when loop time in 1.0..2.0)
                val t = animT.map(1.0, 2.0, 0.0, 1.0, true)
                val xy = Vector2(1.0, easing.function(t, 1.0, -1.0, 1.0))
                drawer.circle(gridRect.position(xy), 5.0)
            }
        }
    }
}
