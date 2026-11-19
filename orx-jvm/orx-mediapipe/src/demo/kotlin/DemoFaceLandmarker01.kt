import org.openrndr.application
import org.openrndr.extra.mediapipe.MediaPipeModels
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.loadImage
import org.openrndr.extra.mediapipe.FaceLandmarker
import org.openrndr.extra.mediapipe.FaceLandmarks

/**
 * Finds the face mesh in an image of MediaPipe and draws its triangles, the contours of the face and the irises.
 * The models and images are downloaded from [orx-models](https://github.com/openrndr/orx-models) when first used.
 */
fun main() = application {
    configure {
        width = 640
        height = 800
    }
    program {
        val image = loadImage(MediaPipeModels.image("portrait.jpg"))
        val faces = FaceLandmarker().use { it.detect(image) }
        extend {
            drawer.scale(width / image.width.toDouble())
            drawer.image(image)
            for (face in faces) {
                val points = face.landmarks.map { it.xy }
                drawer.stroke = ColorRGBa.WHITE.opacify(0.4)
                drawer.strokeWeight = 1.0
                drawer.lineSegments(FaceLandmarks.TRIANGLES.flatMap { (a, b, c) ->
                    listOf(points[a], points[b], points[b], points[c], points[c], points[a])
                })
                drawer.stroke = ColorRGBa.PINK
                drawer.strokeWeight = 3.0
                drawer.lineSegments(FaceLandmarks.CONTOURS.flatMap { (a, b) -> listOf(points[a], points[b]) })
            }
        }
    }
}
