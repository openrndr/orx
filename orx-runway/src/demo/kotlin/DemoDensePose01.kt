import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.extra.runway.*
import org.openrndr.resourceUrl

/**
 * This demonstrates the body estimation model of DensePose
 * This example requires a `runway/DensePose` model active in Runway.
 */
fun main() = application {
    configure {
        width = 512
        height = 512
    }

    program {
        val rt = renderTarget(512, 512) {
            colorBuffer()
        }
        val startImage = loadImage(resourceUrl("/data/images/peopleCity01.jpg"))

        drawer.isolatedWithTarget(rt) {
            drawer.ortho(rt)
            drawer.background(ColorRGBa.BLACK)
            drawer.image(startImage, (rt.width - startImage.width)/2.0, (rt.height - startImage.height) / 2.0)
        }

        extend {
            val result: DensePoseResult =
                    runwayQuery("http://localhost:8000/query", DensePoseQuery(rt.colorBuffer(0).toData()))
            val image = ColorBuffer.fromData(result.output)

            drawer.image(image, 0.0, 0.0, 512.0, 512.0)
            image.destroy()
        }
    }
}