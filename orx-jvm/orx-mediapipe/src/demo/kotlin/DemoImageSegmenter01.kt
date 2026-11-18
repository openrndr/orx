import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.ColorFormat
import org.openrndr.draw.ColorType
import org.openrndr.draw.colorBuffer
import org.openrndr.draw.loadImage
import org.openrndr.draw.shadeStyle
import org.openrndr.extra.mediapipe.ImageSegmenter

/**
 * Segments an image of MediaPipe into hair, skin of the face and of the body, clothes and others, and draws each in a
 * color over the image. The models and the image are downloaded and converted by
 * `uv run orx-jvm/orx-mediapipe/tools/convert_models.py`.
 */
fun main() = application {
    configure {
        width = 640
        height = 800
    }
    program {
        val image = loadImage("orx-jvm/orx-mediapipe/models/images/portrait.jpg")
        val colors = mapOf(
            "hair" to ColorRGBa.YELLOW, "body-skin" to ColorRGBa.CYAN, "face-skin" to ColorRGBa.PINK,
            "clothes" to ColorRGBa.GREEN, "others" to ColorRGBa.RED
        )
        // the masks of the categories, in color buffers that are stretched over the image
        val masks = ImageSegmenter().use { segmenter ->
            val segmentation = segmenter.segment(image)
            colors.keys.associateWith { category ->
                colorBuffer(segmentation.width, segmentation.height, format = ColorFormat.R, type = ColorType.FLOAT32).also {
                    segmentation.write(category, it)
                }
            }
        }
        extend {
            drawer.scale(width / image.width.toDouble())
            drawer.image(image)
            for ((category, mask) in masks) {
                // the color of the category, as opaque as the mask is sure
                drawer.shadeStyle = shadeStyle {
                    fragmentTransform = "x_fill = vec4(p_color.rgb, x_fill.r * 0.6);"
                    parameter("color", colors.getValue(category))
                }
                drawer.image(mask, image.bounds)
            }
        }
    }
}
