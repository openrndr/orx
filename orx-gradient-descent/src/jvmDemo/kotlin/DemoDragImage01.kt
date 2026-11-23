import org.openrndr.WindowMultisample
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.loadImage
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle

/**
 * An image of three parameters, its center and its scale: dragging the center moves it, dragging its corner scales
 * it around its center, see [DragParameters]. [DragParameters.affects] makes the center change the position only and
 * the corner the scale only.
 */
fun main() = application {
    configure {
        width = 720
        height = 720
        multisample = WindowMultisample.SampleCount(4)
    }
    program {
        val image = loadImage("demo-data/images/image-001.png")
        val size = Vector2(image.width.toDouble(), image.height.toDouble())

        val drag = extend(DragParameters(doubleArrayOf(0.0, 0.0, 300.0 / size.x), drawer.bounds.center) { (x, y, scale) ->
            val center = Vector2(x, y)
            listOf(center, center + size * (scale / 2))
        })
        drag.affects = { if (it == 0) intArrayOf(0, 1) else intArrayOf(2) }
        extend {
            drawer.translate(drag.origin)
            val (x, y, scale) = drag.parameters
            val bounds = Rectangle.fromCenter(Vector2(x, y), size.x * scale, size.y * scale)
            drawer.image(image, bounds.corner, bounds.width, bounds.height)
            drawer.fill = null
            drawer.stroke = ColorRGBa.WHITE.opacify(0.5)
            drawer.rectangle(bounds)
            drag.drawPoints(drawer)
        }
    }
}
