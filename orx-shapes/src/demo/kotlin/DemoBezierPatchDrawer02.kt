import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.loadFont
import org.openrndr.extensions.SingleScreenshot
import org.openrndr.extra.color.spaces.toOKLABa
import org.openrndr.extra.shapes.bezierPatch
import org.openrndr.extra.shapes.drawers.bezierPatch
import org.openrndr.shape.Circle

fun main() {
    application {
        configure {
            width = 720
            height = 720
        }
        program {
            extend {
                drawer.clear(ColorRGBa.BLACK)
                val bp2 = bezierPatch(
                    Circle(width/2.0 - 180.0, height/2.0, 170.0).contour
                ).withColors(
                    listOf(
                    listOf(ColorRGBa.PINK, ColorRGBa.PINK, ColorRGBa.PINK, ColorRGBa.PINK),
                        listOf(ColorRGBa.RED, ColorRGBa.RED, ColorRGBa.RED, ColorRGBa.RED),
                        listOf(ColorRGBa.BLUE, ColorRGBa.BLUE, ColorRGBa.BLUE, ColorRGBa.BLUE),
                        listOf(ColorRGBa.WHITE, ColorRGBa.WHITE, ColorRGBa.WHITE, ColorRGBa.WHITE),
                    )
                )
                drawer.bezierPatch(bp2)
                val bp3 = bezierPatch(
                    Circle(width/2.0 + 180.0, height/2.0, 170.0).contour
                ).withColors(
                    listOf(
                        listOf(ColorRGBa.PINK.toOKLABa(), ColorRGBa.PINK.toOKLABa(), ColorRGBa.PINK.toOKLABa(), ColorRGBa.PINK.toOKLABa()),
                        listOf(ColorRGBa.RED.toOKLABa(), ColorRGBa.RED.toOKLABa(), ColorRGBa.RED.toOKLABa(), ColorRGBa.RED.toOKLABa()),
                        listOf(ColorRGBa.BLUE.toOKLABa(), ColorRGBa.BLUE.toOKLABa(), ColorRGBa.BLUE.toOKLABa(), ColorRGBa.BLUE.toOKLABa()),
                        listOf(ColorRGBa.WHITE.toOKLABa(), ColorRGBa.WHITE.toOKLABa(), ColorRGBa.WHITE.toOKLABa(), ColorRGBa.WHITE.toOKLABa()),
                    )
                )
                drawer.bezierPatch(bp3)

                drawer.fill = ColorRGBa.WHITE
                drawer.fontMap = loadFont("demo-data/fonts/IBMPlexMono-Regular.ttf", 16.0)
                drawer.text("RGB", width/2.0 - 180.0, height/2.0 + 200.0)
                drawer.text("OKLab", width/2.0 + 180.0, height/2.0 + 200.0)
            }
        }
    }
}