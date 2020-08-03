import org.openrndr.application
import org.openrndr.draw.*
import org.openrndr.extra.runway.*

/**
 * This example requires a `runway/U-2-Net` model to be active in Runway.
 */
fun main() = application {
    configure {
        width = 305
        height = 400
    }

    program {
        val image = loadImage("demo-data/images/vw-beetle.jpg")

        val result: U2NetResult =
                runwayQuery("http://localhost:8000/query", U2NetRequest(image.toData()))

        val segmentImage = ColorBuffer.fromData(result.image)

        extend {
            drawer.image(segmentImage, 0.0, 0.0)
        }
    }
}