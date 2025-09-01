import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.isolated
import org.openrndr.extra.color.spaces.OKHSV
import org.openrndr.extra.color.tools.mixHue
import org.openrndr.extra.color.tools.withHue

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
