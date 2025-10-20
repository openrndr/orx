import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.FontImageMap
import org.openrndr.draw.TextSettingMode
import org.openrndr.draw.isolated
import org.openrndr.draw.loadFont
import org.openrndr.draw.shadeStyle
import org.openrndr.extra.textwriter.TextWriter
import org.openrndr.extra.textwriter.writer
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import kotlin.math.cos

/**
 * This demo implements a drawing program utilizing custom text rendering with a wave-like animation effect.
 * It allows for manipulating text position and scaling over time.
 *
 * Key elements of the program:
 * - A centered rectangle on the drawing canvas.
 * - Text rendering with properties such as horizontal alignment, vertical alignment, and tracking,
 *   dynamically changing over time.
 * - Custom text animation implementing wave-like movement and scaling.
 */
fun main() = application {
    configure {
        width = 720
        height = 720
    }
    program {
        extend {
            val r = Rectangle.fromCenter(drawer.bounds.center, 600.0, 600.0)
            drawer.isolated {
                drawer.fill = null
                drawer.stroke = ColorRGBa.WHITE
                drawer.rectangle(r)
            }
            drawer.fontMap = loadFont("demo-data/fonts/IBMPlexMono-Regular.ttf", 24.0)


            fun TextWriter.wavyWrite(text: String) {
                text(text, visible = false)
                drawer.shadeStyle = shadeStyle {
                    fragmentTransform = """
                            float o = x_fill.r;
                            x_fill = u_fill;
                            x_fill.a *= o;
                        """.trimIndent()
                }

                drawer.image((drawer.fontMap as FontImageMap).texture, glyphOutput.rectangles.mapIndexed { index, it ->
                    Pair(
                        it.first, it.second
                            .movedBy(Vector2(0.0, 20.0 * cos(index * 0.5 + seconds * 10.0)))
                            .scaledBy(cos(index * 0.1 + seconds * 0.5) * 0.5 + 1.0)
                    )
                })
            }
            writer {
                drawer.drawStyle.textSetting = TextSettingMode.SUBPIXEL
                style.horizontalAlign = cos(seconds) * 0.5 + 0.5
                style.verticalAlign = 0.5
                style.tracking = (cos(seconds * 0.1) * 0.5 + 0.5) * 20.0
                box = r.offsetEdges(-10.0)
                wavyWrite("hello world\nthis is a test\ncentered")
            }
        }
    }
}
