import org.openrndr.application
import org.openrndr.draw.loadImage

suspend fun main() {
    application {
        program {
            val image = loadImage("demo-data/images/image-001.dds")
            println(image.format)
            println(image.type)
            extend {
                drawer.image(image)
            }
        }
    }
}