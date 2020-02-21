import jsyphon.SyphonClient
import org.openrndr.application
import org.openrndr.color.ColorRGBa



fun main() = application {
    configure {
        width = 1000
        height = 800
    }

    program {
        val syphonClient = SyphonClient()

        extend(syphonClient)
        extend {
            drawer.clear(ColorRGBa.BLACK)
            drawer.image(syphonClient.buffer)
        }
    }
}