import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.loadImage
import org.openrndr.extra.mediapipe.HandLandmarker
import org.openrndr.extra.mediapipe.HandLandmarks

/**
 * Finds the hands in an image of MediaPipe and draws their bones and landmarks. The models and the image are downloaded
 * and converted by `uv run orx-jvm/orx-mediapipe/tools/convert_models.py`.
 */
fun main() = application {
    configure {
        width = 720
        height = 382
    }
    program {
        val image = loadImage("orx-jvm/orx-mediapipe/models/images/right_hands.jpg")
        val hands = HandLandmarker().use { it.detect(image) }
        extend {
            drawer.image(image)
            for (hand in hands) {
                val points = hand.landmarks.map { it.xy }
                drawer.stroke = if (hand.rightHanded) ColorRGBa.PINK else ColorRGBa.CYAN
                drawer.strokeWeight = 3.0
                drawer.lineSegments(HandLandmarks.CONNECTIONS.flatMap { (a, b) -> listOf(points[a], points[b]) })
                drawer.stroke = null
                drawer.fill = ColorRGBa.WHITE
                drawer.circles(points, 4.0)
            }
        }
    }
}
