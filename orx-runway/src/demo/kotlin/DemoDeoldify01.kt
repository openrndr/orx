import org.openrndr.application
import org.openrndr.draw.ColorBuffer
import org.openrndr.draw.colorBuffer
import org.openrndr.extra.fx.transform.FlipVertically
import org.openrndr.extra.runway.DeOldifyRequest
import org.openrndr.extra.runway.DeOldifyResponse
import org.openrndr.extra.runway.runwayQuery
import org.openrndr.ffmpeg.ScreenRecorder
import org.openrndr.ffmpeg.VideoPlayerFFMPEG

/**
 * This demonstrates the Deoldify model, which colors grayscale images
 * This example requires a `reiinakano/DeOldify` model active in Runway.
 */

fun main() = application {
    configure {
        width = 1280
        height = 360
    }

    program {
        val image = colorBuffer(640, 360)
        val camera = VideoPlayerFFMPEG.fromDevice(imageWidth = 640, imageHeight = 360)
        camera.play()
        val flip = FlipVertically()

        camera.newFrame.listen {
            flip.apply(it.frame, image)
        }

        extend {
            camera.draw(drawer)
            val response: DeOldifyResponse =
                    runwayQuery("http://localhost:8000/query", DeOldifyRequest(image.toDataUrl()))

            val image = ColorBuffer.fromUrl(response.image)
            drawer.image(image, 640.0, 0.0)
            image.destroy()
        }
    }
}