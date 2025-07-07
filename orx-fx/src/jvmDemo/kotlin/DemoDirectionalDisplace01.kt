import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.color.rgb
import org.openrndr.draw.ColorType
import org.openrndr.draw.colorBuffer
import org.openrndr.draw.loadImage
import org.openrndr.drawImage
import org.openrndr.extra.fx.distort.DirectionalDisplace
import org.openrndr.extra.noise.simplex
import org.openrndr.extra.shapes.primitives.grid
import kotlin.math.cos

/**
 * Demonstrate how to use [DirectionalDisplace].
 *
 * The direction map is populated using `drawImage` instead of
 * pixel by pixel. A grid of circles is drawn, each circle with a
 * color based on simplex noise. The R and G channels of the colors
 * control the direction of the sampling. By animating the sampling
 * distance the result oscillates between no-effect and a noticeable one.
 */
fun main() = application {
    program {
        val displace = DirectionalDisplace()

        val displaced = colorBuffer(width, height)
        val direction = drawImage(width, height, type = ColorType.FLOAT32) {
            clear(ColorRGBa.BLACK)
            bounds.grid(32, 24).flatten().forEach {
                fill = rgb(
                    simplex(133, it.center * 0.004),
                    simplex(197, it.center * 0.004),
                    0.0
                )
                stroke = null

                //rectangle(it)
                circle(it.center, 8.0)
            }
        }
        val image = loadImage("demo-data/images/image-001.png")
        extend {
            displace.distance = 100.0 + 100.0 * cos(seconds)
            displace.wrapX = true
            displace.wrapX = true
            displace.apply(arrayOf(image, direction), displaced)
            drawer.image(displaced)
        }
    }
}