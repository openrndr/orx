import jsyphon.SyphonClient
import org.openrndr.application
import org.openrndr.color.ColorRGBa

fun main() {
    System.setProperty("org.openrndr.gl3.gl_type", "gl")
    application {
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
}