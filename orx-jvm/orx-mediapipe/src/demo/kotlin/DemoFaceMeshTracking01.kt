import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.isolated
import org.openrndr.draw.loadFont
import org.openrndr.extra.mediapipe.FaceLandmarker
import org.openrndr.extra.mediapipe.FaceLandmarks
import org.openrndr.ffmpeg.loadVideoDevice

/**
 * Follows the face in front of the webcam and draws its mesh, its contours and irises, and bars for some of its
 * blendshapes. The webcam is shown mirrored, like a mirror. The models are downloaded and converted by
 * `uv run orx-jvm/orx-mediapipe/tools/convert_models.py`.
 */
fun main() = application {
    configure {
        width = 1280
        height = 720
    }
    program {
        val webcam = loadVideoDevice(width = 1280, height = 720)
        webcam.play()
        // the face is followed from frame to frame, it is only looked for while it is not followed
        val landmarker = FaceLandmarker(tracking = true)
        var faces = emptyList<FaceLandmarks>()
        var millis = 0.0
        webcam.newFrame.listen {
            val start = System.nanoTime()
            faces = landmarker.detect(it.frame)
            millis = (System.nanoTime() - start) / 1E6
        }
        val font = loadFont("demo-data/fonts/IBMPlexMono-Regular.ttf", 20.0)
        val shown = listOf(
            "eyeBlinkLeft", "eyeBlinkRight", "jawOpen", "mouthSmileLeft", "mouthSmileRight", "browInnerUp", "mouthPucker"
        )
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
                    val points = face.landmarks.map { it.xy }
                    drawer.stroke = ColorRGBa.WHITE.opacify(0.3)
                    drawer.strokeWeight = 1.0 / scale
                    drawer.lineSegments(FaceLandmarks.TRIANGLES.flatMap { (a, b, c) ->
                        listOf(points[a], points[b], points[b], points[c], points[c], points[a])
                    })
                    drawer.stroke = ColorRGBa.PINK
                    drawer.strokeWeight = 3.0 / scale
                    drawer.lineSegments(FaceLandmarks.CONTOURS.flatMap { (a, b) -> listOf(points[a], points[b]) })
                }
            }
            drawer.fontMap = font
            drawer.fill = ColorRGBa.WHITE
            drawer.stroke = null
            drawer.text("${faces.size} faces, ${"%.1f".format(millis)} ms", 20.0, 40.0)
            // the blendshapes of the first face, as bars
            faces.firstOrNull()?.let { face ->
                for ((i, name) in shown.withIndex()) {
                    val y = 80.0 + i * 30.0
                    drawer.fill = ColorRGBa.WHITE
                    drawer.text(name, 20.0, y + 16.0)
                    drawer.fill = ColorRGBa.PINK
                    drawer.rectangle(200.0, y, 200.0 * face.blendshapes.getValue(name), 20.0)
                }
            }
        }
    }
}
