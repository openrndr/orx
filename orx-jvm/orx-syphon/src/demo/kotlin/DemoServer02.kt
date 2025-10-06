import jsyphon.SyphonServer
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.isolatedWithTarget
import org.openrndr.draw.renderTarget
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/* This demo uses an alternate RenderTarget to send frames to Syphon (instead of the entire screen).
* */
fun main() {
    // force to use GL driver
    System.setProperty("org.openrndr.gl3.gl_type", "gl")
    application {
        configure {
            width = 1000
            height = 1000
        }

        program {
            val rt = renderTarget(100, 100) {
                colorBuffer()
            }

            // You can give the server a different name
            extend(SyphonServer("Test", rt))

            extend {
                /**
                 * This is what will be sent to Syphon, and drawn in a small corner of the screen
                 */
                drawer.isolatedWithTarget(rt) {
                    drawer.clear(ColorRGBa(sin(seconds), cos(seconds / 2.0), 0.5, 1.0))
                }

                drawer.clear(ColorRGBa.PINK)
                drawer.fill = ColorRGBa.WHITE
                drawer.circle(drawer.bounds.center, abs(cos(seconds)) * height * 0.5)
                drawer.image(rt.colorBuffer(0))
            }
        }
    }
}