import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.isolated
import org.openrndr.draw.loadFont
import org.openrndr.extra.mediapipe.PoseLandmarker
import org.openrndr.extra.mediapipe.PoseLandmarks
import org.openrndr.ffmpeg.loadVideoDevice

/**
 * Follows the body in front of the webcam and draws its landmarks and the connections between them, those that are
 * likely to be visible. The webcam is shown mirrored, like a mirror.
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
        // the body is followed from frame to frame, it is only looked for while it is not followed
        val landmarker = PoseLandmarker(tracking = true)
        var poses = emptyList<PoseLandmarks>()
        var millis = 0.0
        webcam.newFrame.listen {
            val start = System.nanoTime()
            poses = landmarker.detect(it.frame)
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
                for (pose in poses) {
                    val points = pose.landmarks.map { it.xy }
                    val visible = pose.visibility.map { it > 0.5 }
                    drawer.stroke = ColorRGBa.PINK
                    drawer.strokeWeight = 4.0 / scale
                    drawer.lineSegments(PoseLandmarks.CONNECTIONS.filter { (a, b) -> visible[a] && visible[b] }
                        .flatMap { (a, b) -> listOf(points[a], points[b]) })
                    drawer.stroke = null
                    drawer.fill = ColorRGBa.WHITE
                    drawer.circles(points.filterIndexed { i, _ -> visible[i] }, 6.0 / scale)
                }
            }
            drawer.fontMap = font
            drawer.fill = ColorRGBa.WHITE
            drawer.text("${poses.size} bodies, ${"%.1f".format(millis)} ms", 20.0, 40.0)
        }
    }
}
