import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.isolated
import org.openrndr.draw.loadFont
import org.openrndr.extra.mediapipe.HandLandmarker
import org.openrndr.extra.mediapipe.HandLandmarks
import org.openrndr.ffmpeg.loadVideoDevice

/**
 * Follows the hands in front of the webcam and draws their bones and landmarks, right hands in pink, left hands in
 * cyan. The webcam is shown mirrored, like a mirror; the hands are found in the frames as the webcam sees them, such
 * that left and right are those of the hands.
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
        // the hands are followed from frame to frame, palms are only looked for while fewer than two are followed
        val landmarker = HandLandmarker(tracking = true)
        var hands = emptyList<HandLandmarks>()
        var millis = 0.0
        webcam.newFrame.listen {
            val start = System.nanoTime()
            hands = landmarker.detect(it.frame)
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
                for (hand in hands) {
                    val points = hand.landmarks.map { it.xy }
                    drawer.stroke = if (hand.rightHanded) ColorRGBa.PINK else ColorRGBa.CYAN
                    drawer.strokeWeight = 4.0 / scale
                    drawer.lineSegments(HandLandmarks.CONNECTIONS.flatMap { (a, b) -> listOf(points[a], points[b]) })
                    drawer.stroke = null
                    drawer.fill = ColorRGBa.WHITE
                    drawer.circles(points, 5.0 / scale)
                }
            }
            drawer.fontMap = font
            drawer.fill = ColorRGBa.WHITE
            drawer.text("${hands.size} hands, ${"%.1f".format(millis)} ms", 20.0, 40.0)
        }
    }
}
