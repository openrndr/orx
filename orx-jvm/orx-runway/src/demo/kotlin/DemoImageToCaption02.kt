import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.loadFont
import org.openrndr.draw.loadImage
import org.openrndr.extra.runway.Im2txtRequest
import org.openrndr.extra.runway.Im2txtResult
import org.openrndr.extra.runway.runwayQuery


/**
 * This demonstrates an image to text network. It generates caption texts from a single file
 * This example requires a `runway/im2txt` model to be active in Runway.
 */


suspend fun main() = application {
    configure {
        width = 768
        height = 576
    }

    program {
        val image = loadImage("demo-data/images/pm5544.png")

        val font = loadFont("demo-data/fonts/IBMPlexMono-Regular.ttf", 24.0)
        extend {
            drawer.fontMap = font
            val response: Im2txtResult =
                    runwayQuery("http://localhost:8000/query", Im2txtRequest(image.toDataUrl()))
            drawer.image(image)

            drawer.fontMap = font
            drawer.fill = ColorRGBa.PINK
            drawer.text(response.caption, 40.0, height - 40.0)
            Thread.sleep(4000)
        }
    }
}