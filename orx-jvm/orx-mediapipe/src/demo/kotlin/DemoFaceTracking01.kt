import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.isolated
import org.openrndr.draw.loadFont
import org.openrndr.extra.mediapipe.FaceDetection
import org.openrndr.extra.mediapipe.FaceDetector
import org.openrndr.ffmpeg.loadVideoDevice

/**
 * Finds the faces in front of the webcam in every frame and draws their boxes and keypoints: the eyes, the tip of the
 * nose, the mouth and the ears. The webcam is shown mirrored, like a mirror.
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
        val detector = FaceDetector()
        var faces = emptyList<FaceDetection>()
        var millis = 0.0
        webcam.newFrame.listen {
            val start = System.nanoTime()
            faces = detector.detect(it.frame)
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
                for (face in faces) {
                    drawer.fill = null
                    drawer.stroke = ColorRGBa.PINK
                    drawer.strokeWeight = 4.0 / scale
                    drawer.rectangle(face.bounds)
                    drawer.stroke = null
                    drawer.fill = ColorRGBa.WHITE
                    drawer.circles(face.keypoints, 6.0 / scale)
                }
            }
            drawer.fontMap = font
            drawer.fill = ColorRGBa.WHITE
            drawer.text("${faces.size} faces, ${"%.1f".format(millis)} ms", 20.0, 40.0)
        }
    }
}
