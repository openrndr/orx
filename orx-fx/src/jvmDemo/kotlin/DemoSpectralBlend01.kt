import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.createEquivalent
import org.openrndr.drawImage
import org.openrndr.extra.fx.blend.BlendSpectral
import org.openrndr.extra.fx.blur.BoxBlur
import org.openrndr.extra.fx.patterns.Checkers
import org.openrndr.math.Vector2
import kotlin.math.sin

/**
 * Demonstration of how to use the [BlendSpectral] filter to combine two images, using
 * this pigment-simulation color mixing approach.
 *
 * The program:
 * - generates two images
 * - blurs one of them
 * - creates and draws a checkers-pattern as the background
 * - mixes and draws both images
 *
 * The `fill` factor, which controls how the top and the bottom colors are mixed, is animated.
 *
 * The `clip` parameter is also animated and toggles every 6 seconds.
 */
fun main() = application {
    configure {
        width = 800
        height = 800
    }
    program {
        val a = drawImage(width, height) {
            drawer.stroke = null
            drawer.fill = ColorRGBa.BLUE
            drawer.circle(drawer.bounds.center - Vector2(100.0, 0.0), drawer.width * 0.25)
        }
        val b = drawImage(width, height) {
            drawer.clear(ColorRGBa.TRANSPARENT)
            drawer.stroke = ColorRGBa.RED
            drawer.strokeWeight = 10.0
            drawer.fill = ColorRGBa.YELLOW.opacify(1.0)
            drawer.circle(drawer.bounds.center + Vector2(100.0, 0.0), drawer.width * 0.25)
        }
        BoxBlur().apply { window = 10 }.apply(b, b)
        val checked = a.createEquivalent()
        Checkers().apply(emptyArray(), checked)

        val mixed = a.createEquivalent()
        val blendSpectral = BlendSpectral()
        extend {
            drawer.image(checked)
            blendSpectral.fill = sin(seconds) * 0.5 + 0.5
            blendSpectral.clip = seconds.mod(12.0) > 6.0
            blendSpectral.apply(a, b, mixed)
            drawer.image(mixed)
        }
    }
}
