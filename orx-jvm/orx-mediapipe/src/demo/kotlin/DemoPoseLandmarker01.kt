import org.openrndr.application
import org.openrndr.extra.mediapipe.MediaPipeModels
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.loadImage
import org.openrndr.extra.mediapipe.PoseLandmarker
import org.openrndr.extra.mediapipe.PoseLandmarks

/**
 * Finds the body in an image of MediaPipe and draws its landmarks and the connections between them.
 * The models and images are downloaded from [orx-models](https://github.com/openrndr/orx-models) when first used.
 */
fun main() = application {
    configure {
        width = 1000
        height = 667
    }
    program {
        val image = loadImage(MediaPipeModels.image("pose.jpg"))
        val poses = PoseLandmarker().use { it.detect(image) }
        extend {
            drawer.image(image)
            for (pose in poses) {
                val points = pose.landmarks.map { it.xy }
                drawer.stroke = ColorRGBa.PINK
                drawer.strokeWeight = 4.0
                drawer.lineSegments(PoseLandmarks.CONNECTIONS.flatMap { (a, b) -> listOf(points[a], points[b]) })
                drawer.stroke = null
                drawer.fill = ColorRGBa.WHITE
                drawer.circles(points, 6.0)
            }
        }
    }
}
