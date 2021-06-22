import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.ColorBuffer
import org.openrndr.draw.loadFont
import org.openrndr.extra.runway.AttnGANRequest
import org.openrndr.extra.runway.AttnGANResult
import org.openrndr.extra.runway.runwayQuery
import java.io.File

/**
 * This demonstrates a text to image network. It generates images from simple sentences.
 * This example requires a `runway/AttnGAN` model to be active in Runway.
 */

suspend fun main() = application {
    configure {
        width = 768
        height = 576
    }

    program {
        val runwayHost = "http://localhost:8000/query"
        val nouns = File("demo-data/words/nouns.txt").readText().split("\n")
        val prepositions = File("demo-data/words/prepositions.txt").readText().split("\n")
        val adjectives = File("demo-data/words/adjectives.txt").readText().split("\n")
        val font = loadFont("demo-data/fonts/IBMPlexMono-Regular.ttf", 36.0)

        extend {
            val text = "a ${adjectives.random()} ${nouns.random()} ${prepositions.random()} a ${adjectives.random()} ${nouns.random()}"
            val result: AttnGANResult = runwayQuery(runwayHost, AttnGANRequest(text))
            val image = ColorBuffer.fromUrl(result.result)
            drawer.fontMap = font
            drawer.image(image, (width - image.width) / 2.0, (height - image.height) / 2.0)
            drawer.fill = ColorRGBa.PINK
            drawer.text(text, 40.0, height - 40.0)
            image.destroy()
        }
    }
}