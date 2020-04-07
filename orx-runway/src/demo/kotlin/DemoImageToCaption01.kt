import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.colorBuffer
import org.openrndr.draw.loadFont
import org.openrndr.extra.fx.transform.FlipVertically
import org.openrndr.extra.runway.*
import org.openrndr.ffmpeg.ScreenRecorder
import org.openrndr.ffmpeg.VideoPlayerFFMPEG
import org.openrndr.math.Vector2


/**
 * This demonstrates an image to text network. It generates caption texts from a camera image
 * This example requires a `runway/im2txt` model to be active in Runway.
 */


fun main() = application {
    configure {
        width = 768
        height = 576
    }

    program {
        val image = colorBuffer(640, 360)
        val camera = VideoPlayerFFMPEG.fromDevice(imageWidth = 640, imageHeight = 360)
        camera.play()
        val flip = FlipVertically()

        val font = loadFont("data/fonts/IBMPlexMono-Regular.ttf", 24.0)
        camera.newFrame.listen {
            flip.apply(it.frame, image)
        }
        extend(ScreenRecorder()) {
            frameRate = 1
        }
        extend {
            camera.draw(drawer)
            drawer.fontMap = font
            try {
                val response: Im2txtResult =
                        runwayQuery("http://localhost:8000/query", Im2txtRequest(image.toDataUrl()))

                drawer.fontMap = font
                drawer.fill = ColorRGBa.PINK
                drawer.text(response.caption, 40.0, height - 40.0)

            } catch (e: IllegalStateException) {
                e.printStackTrace()
            }
        }
    }
}