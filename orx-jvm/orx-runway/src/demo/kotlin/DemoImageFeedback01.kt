import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.ColorBuffer
import org.openrndr.draw.loadFont
import org.openrndr.extra.runway.*
import org.openrndr.ffmpeg.ScreenRecorder
import org.openrndr.resourceUrl
import java.io.File
import java.net.URL

/**
 * This demonstrates a feedback loop
 * This example requires:
 * a `runway/im2txt` model active in Runway on port 8000
 * a `runway/AttnGAN` model active in Runway on port 8001
 */

fun main() = application {
    configure {
        width = 768
        height = 576
    }

    program {

        val attnHost = "http://localhost:8001/query"
        val im2txtHost = "http://localhost:8000/query"

        val nouns = File("demo-data/words/nouns.txt").readText().split("\n")
        val prepositions = File("demo-data/words/prepositions.txt").readText().split("\n")
        val adjectives = File("demo-data/words/adjectives.txt").readText().split("\n")

        val font = loadFont("demo-data/fonts/IBMPlexMono-Regular.ttf", 36.0)

        var text = "a ${adjectives.random()} ${nouns.random()} ${prepositions.random()} a ${adjectives.random()} ${nouns.random()}"

        extend(ScreenRecorder()) {
            frameRate = 1
        }

        extend {
            val result: AttnGANResult = runwayQuery(attnHost, AttnGANRequest(text))
            val image = ColorBuffer.fromUrl(result.result)
            drawer.fontMap = font
            drawer.image(image, (width - image.width)/2.0, (height - image.height)/2.0)

            val result2:Im2txtResult = runwayQuery(im2txtHost, Im2txtRequest(image.toDataUrl()))
            text = result2.caption

            drawer.fill = ColorRGBa.PINK
            drawer.text(text, 40.0, height - 40.0)
            image.destroy()
        }
    }
}