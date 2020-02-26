import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.syphon.SyphonServer
import kotlin.math.*


fun main() = application {
    configure {
        width = 1000
        height = 1000
    }

    program {
        extend(SyphonServer("Test"))

        extend {
            drawer.background(ColorRGBa.PINK)
            drawer.fill = ColorRGBa.WHITE
            drawer.circle(drawer.bounds.center, abs(cos(seconds)) * height * 0.5)
        }
    }
}