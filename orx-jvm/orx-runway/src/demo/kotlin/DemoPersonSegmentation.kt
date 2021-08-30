import org.openrndr.application
import org.openrndr.draw.*
import org.openrndr.extra.runway.*

/**
 * This example requires a `runway/Person-Segmentation` model to be active in Runway.
 */
fun main() = application {
    configure {
        width = 331
        height = 400
    }

    program {
        val image = loadImage("demo-data/images/life-cover.jpg")

        val result: PersonSegmentationResult =
                runwayQuery("http://localhost:8000/query", PersonSegmentationRequest(image.toData(), 0.2))

        val segmentImage = ColorBuffer.fromData(result.image)

        extend {
            drawer.image(segmentImage, 0.0, 0.0)
        }
    }
}