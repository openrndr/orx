import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.color.rgb
import org.openrndr.extensions.SingleScreenshot
import org.openrndr.extra.compositor.*
import org.openrndr.extra.fx.blend.Add
import org.openrndr.extra.fx.blur.ApproximateGaussianBlur
import org.openrndr.extra.fx.color.ColorCorrection
import kotlin.random.Random

/**
 * Compositor demo of `use`, which makes it possible to
 * use the color buffer of a previous layer.
 *
 * This program draws a series of concentric rings, most of them gray,
 * 10% are pink.
 *
 * In a second layer we reuse that image with rings, applying an extreme
 * color correction to make everything black except the pink rings,
 * then apply a strong blur and finally compose it over the original
 * image using blend mode Add.
 *
 * The result is an sharp image of gray rings with glowing pink rings.
 *
 * Note: see also orx-fx Bloom()
 *
 */

// Toggle to see the difference between a simple blur and multilayer bloom
const val effectEnabled = true

suspend fun main() = application {
    configure {
        width = 900
        height = 900
    }

    program {
        // -- this block is for automation purposes only
        if (System.getProperty("takeScreenshot") == "true") {
            extend(SingleScreenshot()) {
                this.outputFile = System.getProperty("screenshotPath")
            }
        }

        val composite = compose {
            val circles = layer {
                draw {
                    drawer.stroke = null
                    val rnd = Random(frameCount / 100 + 1)
                    for (i in 18 downTo 0) {
                        drawer.fill = if (rnd.nextDouble() < 0.1)
                            ColorRGBa.PINK.shade(Random.nextDouble(0.88, 1.0))
                        else
                            rgb(rnd.nextInt(6) / 15.0)

                        drawer.circle(drawer.bounds.center, 50.0 + i * 20)
                    }
                }
                // A. To see how the plain blur looks like
                if (!effectEnabled) {
                    post(ApproximateGaussianBlur()) {
                        sigma = 25.0
                        window = 25
                    }
                }
            }
            // B. This is the bloom effect
            if (effectEnabled) {
                layer {
                    use(circles) // <-- use the previous layer as starting point
                    post(ColorCorrection()) {
                        brightness = -0.3
                        contrast = 0.8
                    }
                    post(ApproximateGaussianBlur()) {
                        sigma = 25.0
                        window = 25
                    }
                    blend(Add())
                }
            }
        }

        extend {
            drawer.clear(rgb(0.2))
            composite.draw(drawer)
        }
    }
}
