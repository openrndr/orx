import org.openrndr.application
import org.openrndr.extra.mediapipe.MediaPipeModels
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.loadImage
import org.openrndr.extra.mediapipe.FaceDetector

/**
 * Finds the face in an image of MediaPipe and draws its box and keypoints.
 * The models and images are downloaded from [orx-models](https://github.com/openrndr/orx-models) when first used.
 */
fun main() = application {
    configure {
        width = 640
        height = 800
    }
    program {
        val image = loadImage(MediaPipeModels.image("portrait.jpg"))
        val faces = FaceDetector().use { it.detect(image) }
        extend {
            drawer.scale(width / image.width.toDouble())
            drawer.image(image)
            drawer.fill = null
            drawer.stroke = ColorRGBa.PINK
            drawer.strokeWeight = 4.0
            for (face in faces) {
                drawer.rectangle(face.bounds)
                drawer.fill = ColorRGBa.PINK
                drawer.stroke = null
                drawer.circles(face.keypoints, 8.0)
            }
        }
    }
}
