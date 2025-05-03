import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.color.rgb
import org.openrndr.draw.ColorType
import org.openrndr.draw.colorBuffer
import org.openrndr.draw.loadImage
import org.openrndr.drawImage
import org.openrndr.extra.fx.distort.DirectionalDisplace
import org.openrndr.math.Polar

/**
 * Demonstrate how to use [DirectionalDisplace].
 *
 * The program draws 12 overlapping translucent circles on the
 * `direction` color buffer to produce new color combinations
 * on the overlapping areas. Those colors specify where the
 * `DirectionalDisplace` effect will sample pixels from.
 *
 */

fun main() = application {
    program {
        val db = DirectionalDisplace()

        val blurred = colorBuffer(width, height)
        val direction = drawImage(width, height, type = ColorType.FLOAT32) {
            clear(ColorRGBa.BLACK)
            for(x in 0 until 6) {
                val offset = Polar(x * 60.0).cartesian
                fill = rgb(offset.y, offset.x, 0.0, 0.3)
                stroke = null

                val pos = bounds.center - offset * 110.0
                circle(pos, 120.0)
                circle(pos, 80.0)
            }
        }
        val image = loadImage("demo-data/images/image-001.png")
        extend {
            db.distance = 250.0
            db.apply(arrayOf(image, direction), blurred)
            drawer.image(blurred)
            //drawer.image(direction)
        }
    }
}