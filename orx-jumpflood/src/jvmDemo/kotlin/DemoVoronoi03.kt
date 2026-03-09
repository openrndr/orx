import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.extra.color.colormatrix.tint
import org.openrndr.extra.jumpfill.ClusteredField
import org.openrndr.extra.jumpfill.DecodeMode
import org.openrndr.extra.noise.scatter
import org.openrndr.extra.noise.uniformRing
import org.openrndr.math.Vector2
import kotlin.random.Random

/**
 * Demonstrates the use of the [ClusteredField] filter.
 *
 * The program updates a render target on every animation frame, then
 * uses that render target as the input for the [ClusteredField] filter.
 * The result is written into the `flowfield` `ColorBuffer`.
 *
 * To give the user more time to appreciate what the program does,
 * a random seed is updated only once per second.
 *
 * The content drawn into the render target consists of small points with
 * colors between red and black. First, scattered points with a minimum
 * distance between them are calculated in the program window. Then, for each
 * of those, 30 random points inside a ring are drawn.
 *
 * The filter then renders an image calculating the distance for every pixel
 * in the render target to the closest non-empty pixel.
 *
 * Try commenting out the `tint` operation to make the calculated distances more obvious,
 * displayed as a shade of blue.
 *
 * You can also make the `innerRadius` and the `outerRadius` in `.uniformRing()`
 * equal to make the `Voronoi` effect more obvious and less glitchy.
 *
 * Moving the mouse to the right side of the window displays the RenderTarget,
 * otherwise the filtered result is shown.
 */
fun main() = application {
    configure {
        width = 512
        height = 512
    }
    program {
        val rt = renderTarget(width, height, 1.0) {
            colorBuffer(type = ColorType.FLOAT32)
        }
        val flowfield = colorBuffer(width, height, type = ColorType.FLOAT32)
        val cluster = ClusteredField(decodeMode = DecodeMode.DISTANCE, outputDistanceToContours = true)

        cluster.normalizedDistance = true

        extend {
            val rnd = Random(seconds.toInt())
            drawer.isolatedWithTarget(rt) {
                drawer.ortho(rt)
                drawer.clear(ColorRGBa(-1.0, -1.0, -1.0, 0.0))
                val points = drawer.bounds.scatter(20.0, random = rnd)
                drawer.points {
                    for ((index, point) in points.withIndex()) {
                        fill = ColorRGBa((index + 1.0) / points.size, 0.0, 0.0, 1.0)
                        for (i in 0 until 30) {
                            point(point + Vector2.uniformRing(15.0, 25.0, random = rnd) * Vector2(1.0, 1.0))
                        }
                    }
                }
            }
            if(mouse.position.x < width * 0.6) {
                // Display effect
                cluster.apply(rt.colorBuffer(0), flowfield)
                drawer.drawStyle.colorMatrix = tint(ColorRGBa(100.0, 100.0, 0.0))
                drawer.image(flowfield)
            } else {
                // Display render target
                drawer.image(rt.colorBuffer(0))
            }
        }
    }
}