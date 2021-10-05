import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.ColorBuffer
import org.openrndr.draw.loadFont
import org.openrndr.extra.runway.AttnGANRequest
import org.openrndr.extra.runway.AttnGANResult
import org.openrndr.extra.runway.runwayQuery
import java.io.File

/**
 * This demonstrates a text to image network. It generates images from single words.
 * This example requires a `runway/AttnGAN` model to be active in Runway.
 */

fun main() = application {
    configure {
        width = 768
        height = 576
    }

    program {
        val runwayHost = "http://localhost:8000/query"
        val words = File("demo-data/words/words.txt").readText().split("\n")
        val font = loadFont("demo-data/fonts/IBMPlexMono-Regular.ttf", 72.0)
        extend {
            val text = words.random()
            val result: AttnGANResult = runwayQuery(runwayHost, AttnGANRequest(text))
            val image = ColorBuffer.fromUrl(result.result)
            drawer.fontMap = font
            drawer.image(image, (width - image.width) / 2.0, (height - image.height) / 2.0)
            drawer.fill = ColorRGBa.PINK
            drawer.text(text, 40.0, height / 2.0)
            image.destroy()
        }
    }
}