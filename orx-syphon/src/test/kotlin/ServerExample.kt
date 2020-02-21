import org.openrndr.application
import org.openrndr.color.ColorRGBa
import kotlin.math.sin


fun main() = application {
    configure {
        width = 1000
        height = 1000
    }

    program {
        extend(SyphonServer("Test"))

        extend {
            drawer.background(ColorRGBa.RED)
            drawer.circle(width/2.0, height/2.0, sin(seconds) * width / 2.0)
        }
    }
}