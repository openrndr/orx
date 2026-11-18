import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.ColorFormat
import org.openrndr.draw.ColorType
import org.openrndr.draw.colorBuffer
import org.openrndr.draw.isolated
import org.openrndr.draw.loadFont
import org.openrndr.draw.shadeStyle
import org.openrndr.extra.mediapipe.ImageSegmenter
import org.openrndr.extra.mediapipe.Segmentation
import org.openrndr.ffmpeg.loadVideoDevice
import org.openrndr.shape.Rectangle

/**
 * Cuts the people in front of the webcam out of the background, which is drawn over in pink, with the selfie model for
 * images of 16:9. The webcam is shown mirrored, like a mirror. The models are downloaded and converted by
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
        val segmenter = ImageSegmenter("orx-jvm/orx-mediapipe/models/selfie_segmenter_landscape.onnx")
        var segmentation: Segmentation? = null
        var millis = 0.0
        webcam.newFrame.listen {
            val start = System.nanoTime()
            segmentation = segmenter.segment(it.frame)
            millis = (System.nanoTime() - start) / 1E6
        }
        val font = loadFont("demo-data/fonts/IBMPlexMono-Regular.ttf", 24.0)
        val mask = colorBuffer(256, 144, format = ColorFormat.R, type = ColorType.FLOAT32)
        extend {
            webcam.draw(drawer, blind = true)
            val frame = webcam.colorBuffer ?: return@extend
            segmentation?.write("person", mask)
            drawer.isolated {
                // mirrored, the frame fills the width of the window
                val scale = width / frame.effectiveWidth.toDouble()
                drawer.translate(width.toDouble(), 0.0)
                drawer.scale(-scale, scale)
                drawer.image(frame)
                // the background, where the mask is not sure of a person
                drawer.shadeStyle = shadeStyle {
                    fragmentTransform = "x_fill = vec4(p_color.rgb, 1.0 - x_fill.r);"
                    parameter("color", ColorRGBa.PINK)
                }
                drawer.image(mask, Rectangle(0.0, 0.0, frame.effectiveWidth.toDouble(), frame.effectiveHeight.toDouble()))
            }
            drawer.fontMap = font
            drawer.fill = ColorRGBa.WHITE
            drawer.text("${"%.1f".format(millis)} ms", 20.0, 40.0)
        }
    }
}
