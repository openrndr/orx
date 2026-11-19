import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.isolated
import org.openrndr.draw.loadFont
import org.openrndr.extra.mediapipe.HolisticLandmarker
import org.openrndr.extra.mediapipe.HolisticLandmarks
import org.openrndr.ffmpeg.loadVideoDevice

/**
 * Follows the person in front of the webcam, the body, the face and the hands, and draws them; the left hand of the
 * person in cyan, the right hand in yellow. The webcam is shown mirrored, like a mirror.
 * The models and images are downloaded from [orx-models](https://github.com/openrndr/orx-models) when first used.
 */
fun main() = application {
    configure {
        width = 1280
        height = 720
    }
    program {
        val webcam = loadVideoDevice(width = 1280, height = 720)
        webcam.play()
        val holistic = HolisticLandmarker(blendshapesPath = null, tracking = true)
        var person: HolisticLandmarks? = null
        var millis = 0.0
        webcam.newFrame.listen {
            val start = System.nanoTime()
            person = holistic.detect(it.frame)
            millis = (System.nanoTime() - start) / 1E6
        }
        val font = loadFont("demo-data/fonts/IBMPlexMono-Regular.ttf", 24.0)
        extend {
            webcam.draw(drawer, blind = true)
            val frame = webcam.colorBuffer ?: return@extend
            drawer.isolated {
                // mirrored, the frame fills the width of the window
                val scale = width / frame.effectiveWidth.toDouble()
                drawer.translate(width.toDouble(), 0.0)
                drawer.scale(-scale, scale)
                drawer.image(frame)
                person?.let { drawer.drawPerson(it, 3.0 / scale) }
            }
            drawer.fontMap = font
            drawer.fill = ColorRGBa.WHITE
            drawer.text("${"%.1f".format(millis)} ms", 20.0, 40.0)
        }
    }
}
