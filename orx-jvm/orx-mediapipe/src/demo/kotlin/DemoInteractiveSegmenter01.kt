import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.ColorFormat
import org.openrndr.draw.ColorType
import org.openrndr.draw.colorBuffer
import org.openrndr.draw.isolated
import org.openrndr.draw.loadImage
import org.openrndr.draw.shadeStyle
import org.openrndr.extra.mediapipe.InteractiveSegmenter
import org.openrndr.extra.mediapipe.toRgbaImage
import org.openrndr.math.Vector2

/**
 * Selects the object under the mouse with MagicTouch: a click selects the object at the point, a drag the object along
 * the scribble.
 * The models and images are downloaded from [orx-models](https://github.com/openrndr/orx-models) when first used.
 */
fun main() = application {
    configure {
        width = 768
        height = 768
    }
    program {
        val image = loadImage("demo-data/images/peopleCity01.jpg")
        val pixels = image.toRgbaImage()
        val segmenter = InteractiveSegmenter()
        val mask = colorBuffer(512, 512, format = ColorFormat.R, type = ColorType.FLOAT32)
        var selected = false
        val scale = width / image.width.toDouble()
        val scribble = mutableListOf<Vector2>()

        fun select(points: List<Vector2>) {
            val segmentation = if (points.size == 1) segmenter.segment(pixels, points[0]) else segmenter.segment(pixels, points)
            segmentation.write("object", mask)
            selected = true
        }
        mouse.buttonDown.listen {
            scribble.clear()
            scribble.add(it.position / scale)
        }
        mouse.dragged.listen {
            scribble.add(it.position / scale)
        }
        mouse.buttonUp.listen {
            if (scribble.isNotEmpty()) select(scribble.toList())
        }
        extend {
            drawer.isolated {
                drawer.scale(scale)
                drawer.image(image)
                if (selected) {
                    // the selected object in pink, as opaque as the mask is sure
                    drawer.shadeStyle = shadeStyle {
                        fragmentTransform = "x_fill = vec4(p_color.rgb, x_fill.r * 0.6);"
                        parameter("color", ColorRGBa.PINK)
                    }
                    drawer.image(mask, image.bounds)
                    drawer.shadeStyle = null
                }
                drawer.stroke = ColorRGBa.WHITE
                drawer.strokeWeight = 2.0
                if (scribble.size > 1) drawer.lineStrip(scribble)
                drawer.fill = ColorRGBa.WHITE
                drawer.stroke = ColorRGBa.BLACK
                scribble.firstOrNull()?.let { drawer.circle(it, 4.0) }
            }
        }
    }
}
