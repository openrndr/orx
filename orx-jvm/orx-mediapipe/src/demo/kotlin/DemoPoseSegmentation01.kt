import org.openrndr.application
import org.openrndr.extra.mediapipe.MediaPipeModels
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.ColorFormat
import org.openrndr.draw.ColorType
import org.openrndr.draw.colorBuffer
import org.openrndr.draw.isolated
import org.openrndr.draw.loadImage
import org.openrndr.draw.shadeStyle
import org.openrndr.extra.mediapipe.PoseLandmarker

/**
 * Finds the body in an image of MediaPipe and draws its mask, in the square around the body, over the image.
 * The models and images are downloaded from [orx-models](https://github.com/openrndr/orx-models) when first used.
 */
fun main() = application {
    configure {
        width = 1000
        height = 667
    }
    program {
        val image = loadImage(MediaPipeModels.image("pose.jpg"))
        val pose = PoseLandmarker().use { it.detect(image).single() }
        val mask = colorBuffer(pose.mask.width, pose.mask.height, format = ColorFormat.R, type = ColorType.FLOAT32)
        pose.mask.write(mask)
        extend {
            drawer.image(image)
            drawer.isolated {
                drawer.translate(pose.mask.center)
                drawer.rotate(Math.toDegrees(pose.mask.rotation))
                drawer.shadeStyle = shadeStyle {
                    fragmentTransform = "x_fill = vec4(p_color.rgb, x_fill.r * 0.6);"
                    parameter("color", ColorRGBa.PINK)
                }
                val s = pose.mask.size
                drawer.image(mask, -s / 2.0, -s / 2.0, s, s)
            }
        }
    }
}
