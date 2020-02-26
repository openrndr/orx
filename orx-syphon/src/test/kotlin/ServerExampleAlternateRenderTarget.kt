import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.isolatedWithTarget
import org.openrndr.draw.renderTarget
import org.openrndr.extra.syphon.SyphonServer
import kotlin.math.*


fun main() = application {
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
                drawer.background(ColorRGBa(sin(seconds), cos(seconds / 2.0), 0.5, 1.0))
            }

            drawer.background(ColorRGBa.PINK)
            drawer.fill = ColorRGBa.WHITE
            drawer.circle(drawer.bounds.center, abs(cos(seconds)) * height * 0.5)
            drawer.image(rt.colorBuffer(0))
        }
    }
}