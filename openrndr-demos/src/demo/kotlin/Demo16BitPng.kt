
import org.openrndr.application
import org.openrndr.draw.loadImage

suspend fun main() {
    application {
        program {
            val image16 = loadImage("demo-data/images/16-bit.png")
            val image8 = loadImage("demo-data/images/image-001.png")
            extend {
                drawer.image(image16)
                drawer.image(image8, 320.0, 0.0)
            }
        }
    }
}