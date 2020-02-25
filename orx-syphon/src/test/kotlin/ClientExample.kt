import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.syphon.SyphonClient


fun main() = application {
    configure {
        width = 1000
        height = 800
    }

    program {
        val syphonClient = SyphonClient()

        extend(syphonClient)
        extend {
            drawer.background(ColorRGBa.BLACK)
            drawer.image(syphonClient.buffer)
        }
    }
}