import org.openrndr.application
import org.openrndr.extra.mediapipe.MediaPipeModels
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.loadImage
import org.openrndr.extra.mediapipe.FaceLandmarks
import org.openrndr.extra.mediapipe.HandLandmarks
import org.openrndr.extra.mediapipe.HolisticLandmarker
import org.openrndr.extra.mediapipe.HolisticLandmarks
import org.openrndr.extra.mediapipe.PoseLandmarks

/**
 * Finds a person in an image of MediaPipe, the body, the face and the hands, and draws them.
 * The models and images are downloaded from [orx-models](https://github.com/openrndr/orx-models) when first used.
 */
fun main() = application {
    configure {
        width = 638
        height = 1000
    }
    program {
        val image = loadImage(MediaPipeModels.image("male_full_height_hands.jpg"))
        val person = HolisticLandmarker().use { it.detect(image) }
        extend {
            drawer.image(image)
            person?.let { drawer.drawPerson(it) }
        }
    }
}

/** draws the body, the contours of the face and the hands of a person */
fun Drawer.drawPerson(person: HolisticLandmarks, lineWidth: Double = 2.0) {
    fun connect(points: List<org.openrndr.math.Vector2>, pairs: List<Pair<Int, Int>>, color: ColorRGBa) {
        stroke = color
        strokeWeight = lineWidth
        lineSegments(pairs.flatMap { (a, b) -> listOf(points[a], points[b]) })
    }
    connect(person.pose.landmarks.map { it.xy }, PoseLandmarks.CONNECTIONS, ColorRGBa.WHITE)
    person.face?.let { connect(it.landmarks.map { p -> p.xy }, FaceLandmarks.CONTOURS, ColorRGBa.PINK) }
    person.leftHand?.let { connect(it.landmarks.map { p -> p.xy }, HandLandmarks.CONNECTIONS, ColorRGBa.CYAN) }
    person.rightHand?.let { connect(it.landmarks.map { p -> p.xy }, HandLandmarks.CONNECTIONS, ColorRGBa.YELLOW) }
}
