package bezierpatch

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.shapes.bezierpatches.bezierPatch
import org.openrndr.shape.Circle

fun main() {
    application {
        program {
            extend {
                drawer.clear(ColorRGBa.PINK)
                val bp = bezierPatch(
                    Circle(width/2.0, height/2.0, 200.0).contour
                ).withColors(
                    listOf(
                    listOf(ColorRGBa.PINK, ColorRGBa.RED, ColorRGBa.BLACK, ColorRGBa.BLUE),
                        listOf(ColorRGBa.RED, ColorRGBa.BLACK, ColorRGBa.BLUE, ColorRGBa.GREEN),
                        listOf(ColorRGBa.PINK, ColorRGBa.RED, ColorRGBa.WHITE, ColorRGBa.GREEN),
                        listOf(ColorRGBa.BLACK, ColorRGBa.WHITE, ColorRGBa.BLACK, ColorRGBa.BLUE),
                    )
                )

                drawer.bezierPatch(bp)

                drawer.fill = null
                drawer.contour(bp.contour)
                for (i in 0 until 10) {
                    drawer.contour(bp.horizontal(i/9.0))
                }
                for (i in 0 until 10) {
                    drawer.contour(bp.vertical(i/9.0))
                }
            }
        }
    }
}