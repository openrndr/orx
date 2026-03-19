import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.isolated
import org.openrndr.extra.color.spaces.OKHSV
import org.openrndr.extra.color.tools.mixHue
import org.openrndr.extra.color.tools.withHue

/**
 * Demonstrates the use of the `ColorRGBa` methods `.withHue()` and `.mixHue()`.
 *
 * `.withHue()` returns a new color by changing the hue of a source color,
 * while maintaining other properties like saturation and value.
 *
 * `.mixHue()` returns a new color where the hue of a source color
 * is interpolated towards a target color by the specified amount.
 *
 * In this demo, the target hue depends on the current time in seconds,
 * resulting in an animated effect.
 */
fun main() = application {
    configure {
        width = 720
        height = 720
    }
    program {
        extend {
            val seedColor = ColorRGBa.PINK
            val targetHue = seconds * 100.0

            val rows = 10
            val columns = 12

            val cellWidth = width / columns.toDouble()
            val cellHeight = height / rows.toDouble()

            drawer.stroke = null
            for (j in 0 until 10) {
                drawer.isolated {
                    for (i in 0 until columns) {
                        drawer.fill = seedColor
                            .withHue<OKHSV>(i * 360.0 / columns)
                            .mixHue<OKHSV>(targetHue, j / (rows.toDouble() - 1.0))
                        drawer.rectangle(0.0, 0.0, cellWidth, cellHeight)
                        drawer.translate(cellWidth, 0.0)
                    }
                }
                drawer.translate(0.0, cellHeight)
            }
        }
    }
}
