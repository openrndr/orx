import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.isolated
import org.openrndr.draw.loadFont
import org.openrndr.extra.mediapipe.GestureRecognizer
import org.openrndr.extra.mediapipe.HandGesture
import org.openrndr.extra.mediapipe.HandLandmarks
import org.openrndr.ffmpeg.loadVideoDevice

/**
 * Follows the hands in front of the webcam and draws them with the names of their gestures. The webcam is shown
 * mirrored, like a mirror.
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
        val recognizer = GestureRecognizer(tracking = true)
        var gestures = emptyList<HandGesture>()
        webcam.newFrame.listen {
            gestures = recognizer.recognize(it.frame)
        }
        val font = loadFont("demo-data/fonts/IBMPlexMono-Regular.ttf", 32.0)
        extend {
            webcam.draw(drawer, blind = true)
            val frame = webcam.colorBuffer ?: return@extend
            val scale = width / frame.effectiveWidth.toDouble()
            drawer.isolated {
                // mirrored, the frame fills the width of the window
                drawer.translate(width.toDouble(), 0.0)
                drawer.scale(-scale, scale)
                drawer.image(frame)
                for (g in gestures) {
                    val points = g.hand.landmarks.map { it.xy }
                    drawer.stroke = if (g.hand.rightHanded) ColorRGBa.PINK else ColorRGBa.CYAN
                    drawer.strokeWeight = 4.0 / scale
                    drawer.lineSegments(HandLandmarks.CONNECTIONS.flatMap { (a, b) -> listOf(points[a], points[b]) })
                }
            }
            // the names of the gestures above the wrists, not mirrored
            drawer.fontMap = font
            drawer.fill = ColorRGBa.WHITE
            for (g in gestures) {
                val wrist = g.hand.landmarks[0].xy
                drawer.text(g.gesture, width - wrist.x * scale - 60.0, wrist.y * scale + 50.0)
            }
        }
    }
}
