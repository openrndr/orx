import org.openrndr.application
import org.openrndr.draw.ColorBuffer
import org.openrndr.draw.colorBuffer
import org.openrndr.extra.fx.transform.FlipVertically
import org.openrndr.extra.runway.DeOldifyRequest
import org.openrndr.extra.runway.DeOldifyResponse
import org.openrndr.extra.runway.runwayQuery
import org.openrndr.ffmpeg.ScreenRecorder
import org.openrndr.ffmpeg.VideoPlayerConfiguration
import org.openrndr.ffmpeg.VideoPlayerFFMPEG

/**
 * This demonstrates the Deoldify model, which colors grayscale images
 * This example requires a `reiinakano/DeOldify` model active in Runway.
 */

fun main() = application {
    configure {
        width = 1280
        height = 480
    }

    program {
        val image = colorBuffer(640, 480)
        val vc = VideoPlayerConfiguration().apply {
            allowFrameSkipping = false
        }
        // -- you will have to supply your own video here
        val video = VideoPlayerFFMPEG.fromFile("data/videos/night_of_the_living_dead_512kb.mp4", configuration = vc)
        video.play()
        val flip = FlipVertically()

        video.newFrame.listen {
            flip.apply(it.frame, image)
        }
        extend(ScreenRecorder()) {
            frameRate = 30
        }
        extend {
            video.draw(drawer, 0.0, 0.0, 640.0, 480.0)
            val response: DeOldifyResponse =
                    runwayQuery("http://localhost:8000/query", DeOldifyRequest(image.toDataUrl()))

            val image = ColorBuffer.fromUrl(response.image)

            drawer.image(image, 640.0, 0.0)
            image.destroy()
        }
    }
}