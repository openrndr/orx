import org.openrndr.application
import org.openrndr.draw.ColorFormat
import org.openrndr.draw.ColorType
import org.openrndr.draw.colorBuffer
import org.openrndr.draw.isolated
import org.openrndr.extra.mediapipe.FaceStylizer
import org.openrndr.extra.mediapipe.StylizedFace
import org.openrndr.extra.mediapipe.toRgbaImage
import org.openrndr.extra.mediapipe.write
import org.openrndr.ffmpeg.loadVideoDevice
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread

/**
 * Stylizes the face in front of the webcam and draws it in place of the face; 1, 2 and 3 choose a color sketch, color
 * ink or an oil painting. The stylizer takes more than a frame, it stylizes the latest frame of the webcam in a thread
 * of its own. The webcam is shown mirrored, like a mirror. The models are downloaded and converted by
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
        val styles = listOf("color_sketch", "color_ink", "oil_painting").map {
            FaceStylizer("orx-jvm/orx-mediapipe/models/face_stylizer_$it.onnx")
        }
        var style = 0
        val busy = AtomicBoolean(false)
        val latest = AtomicReference<StylizedFace?>(null)
        webcam.newFrame.listen {
            // the pixels are read here, the face is stylized in a thread, when the last one is done
            if (busy.compareAndSet(false, true)) {
                val pixels = it.frame.toRgbaImage()
                val stylizer = styles[style]
                thread {
                    latest.set(stylizer.stylize(pixels))
                    busy.set(false)
                }
            }
        }
        keyboard.character.listen {
            if (it.character in '1'..'3') style = it.character - '1'
        }
        val stylized = colorBuffer(256, 256, format = ColorFormat.RGBa, type = ColorType.UINT8)
        extend {
            webcam.draw(drawer, blind = true)
            val frame = webcam.colorBuffer ?: return@extend
            drawer.isolated {
                // mirrored, the frame fills the width of the window
                val scale = width / frame.effectiveWidth.toDouble()
                drawer.translate(width.toDouble(), 0.0)
                drawer.scale(-scale, scale)
                drawer.image(frame)
                latest.get()?.let { face ->
                    face.image.write(stylized)
                    drawStylizedFace(face, stylized)
                }
            }
        }
    }
}
