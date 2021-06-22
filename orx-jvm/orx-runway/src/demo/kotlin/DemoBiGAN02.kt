import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.extra.runway.*

/**
 * This demonstrates an image feedback effect. It starts from a single image.
 * The BigBiGAN generates a new image from the input, this program feeds the
 * generated image back into the model (with an additional distortion).
 * This example requires a `runway/BigBiGAN` model to be active in Runway.
 */
suspend fun main() = application {
    configure {
        width = 512
        height = 512
    }

    program {
        val rt = renderTarget(256, 256) {
            colorBuffer()
        }
        val startImage = loadImage("demo-data/images/portrait.jpg")

        drawer.isolatedWithTarget(rt) {
            drawer.ortho(rt)
            drawer.clear(ColorRGBa.BLACK)
            drawer.image(startImage, (rt.width - startImage.width) / 2.0, (rt.height - startImage.height) / 2.0)
        }

        extend {
            val result: BigBiGANResult =
                    runwayQuery("http://localhost:8000/query", BigBiGANQuery(rt.colorBuffer(0).toData()))

            val image = ColorBuffer.fromData(result.outputImage)
            drawer.image(image, 0.0, 0.0, 512.0, 512.0)

            drawer.isolatedWithTarget(rt) {
                drawer.ortho(rt)
                drawer.translate(image.width / 2.0, image.height / 2.0)
                drawer.rotate(10.0)
                drawer.translate(-image.width / 2.0, -image.height / 2.0)
                drawer.drawStyle.colorMatrix = tint(ColorRGBa.WHITE.opacify(0.5))
                drawer.image(image)
            }
            image.destroy()
        }
    }
}

