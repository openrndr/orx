import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.ColorFormat
import org.openrndr.draw.ColorType
import org.openrndr.draw.colorBuffer
import org.openrndr.draw.loadImage
import org.openrndr.draw.shadeStyle
import org.openrndr.extra.mediapipe.ImageSegmenter

/**
 * Finds the hair in an image of MediaPipe and colors it. The model and the image are downloaded and converted by
 * `uv run orx-jvm/orx-mediapipe/tools/convert_models.py`.
 */
fun main() = application {
    configure {
        width = 640
        height = 800
    }
    program {
        val image = loadImage("orx-jvm/orx-mediapipe/models/images/portrait.jpg")
        val segmentation = ImageSegmenter("orx-jvm/orx-mediapipe/models/hair_segmenter.onnx").use { it.segment(image) }
        val hair = colorBuffer(segmentation.width, segmentation.height, format = ColorFormat.R, type = ColorType.FLOAT32)
        segmentation.write("hair", hair)
        extend {
            drawer.scale(width / image.width.toDouble())
            drawer.image(image)
            // the hair in pink, as opaque as the mask is sure
            drawer.shadeStyle = shadeStyle {
                fragmentTransform = "x_fill = vec4(p_color.rgb, x_fill.r * 0.7);"
                parameter("color", ColorRGBa.PINK)
            }
            drawer.image(hair, image.bounds)
        }
    }
}
