import jsyphon.SyphonServer
import org.openrndr.application
import org.openrndr.color.ColorRGBa

import kotlin.math.*


fun main() {
    // force to use GL driver
    System.setProperty("org.openrndr.gl3.gl_type", "gl")
    application {
        configure {
            width = 1000
            height = 1000
        }

        program {
            extend(SyphonServer("Test"))

            extend {
                drawer.clear(ColorRGBa.PINK)
                drawer.fill = ColorRGBa.WHITE
                drawer.circle(drawer.bounds.center, abs(cos(seconds)) * height * 0.5)
            }
        }
    }
}