import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.isolated
import org.openrndr.draw.loadFont
import org.openrndr.draw.loadImage
import org.openrndr.extra.mediapipe.GestureRecognizer
import org.openrndr.extra.mediapipe.HandLandmarks

/**
 * Recognizes the gestures of the hands in images of MediaPipe and draws the hands with the names of their gestures. The
 * models and the images are downloaded and converted by `uv run orx-jvm/orx-mediapipe/tools/convert_models.py`.
 */
fun main() = application {
    configure {
        width = 1200
        height = 300
    }
    program {
        val images = listOf("thumb_up", "pointing_up", "victory", "fist").map {
            loadImage("orx-jvm/orx-mediapipe/models/images/$it.jpg")
        }
        val gestures = GestureRecognizer().use { recognizer -> images.map { recognizer.recognize(it) } }
        val font = loadFont("demo-data/fonts/IBMPlexMono-Regular.ttf", 32.0)
        extend {
            for ((i, image) in images.withIndex()) {
                drawer.isolated {
                    // side by side, as high as the window
                    val scale = height / image.height.toDouble()
                    drawer.translate(i * width / images.size.toDouble(), 0.0)
                    drawer.scale(scale)
                    drawer.image(image)
                    for (g in gestures[i]) {
                        val points = g.hand.landmarks.map { it.xy }
                        drawer.stroke = ColorRGBa.PINK
                        drawer.strokeWeight = 3.0 / scale
                        drawer.lineSegments(HandLandmarks.CONNECTIONS.flatMap { (a, b) -> listOf(points[a], points[b]) })
                        drawer.fontMap = font
                        drawer.fill = ColorRGBa.WHITE
                        drawer.stroke = null
                        drawer.text("${g.gesture} ${"%.2f".format(g.score)}", 10.0, 40.0)
                    }
                }
            }
        }
    }
}
