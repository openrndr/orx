import org.openrndr.application
import org.openrndr.draw.ColorBuffer
import org.openrndr.extra.runway.StyleGANRequest
import org.openrndr.extra.runway.StyleGANResponse
import org.openrndr.extra.runway.runwayQuery
/**
 * This demonstrates an image synthesizer.
 * StyleGAN accepts a 512 dimensional vector from which it generates images.
 * This example requires a `runway/StyleGAN` model to be active in Runway.
 * This also works with `eryksalvaggio/Ascinte_Seated`
 */
fun main() = application {
    configure {
        width = 512
        height = 512
    }

    program {
        val latentSpaceVector = MutableList(512) { Math.random() }

        extend {
            val result: StyleGANResponse =
                runwayQuery("http://localhost:8000/query", StyleGANRequest(latentSpaceVector, 0.2))

            val image = ColorBuffer.fromUrl(result.image)

            drawer.image(image, 0.0, 0.0, 512.0, 512.0)

            for (i in latentSpaceVector.indices) {
                latentSpaceVector[i] += (Math.random() - 0.5) * 0.1
            }

            image.destroy()
        }
    }
}