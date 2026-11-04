import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.ColorType
import org.openrndr.extra.compositor.*
import org.openrndr.extra.fx.blur.HashBlur
import org.openrndr.extra.shadestyles.presets.TextureScalar
import org.openrndr.extra.fx.patterns.Checkers
import kotlin.math.cos

/**
 * Demonstrates how to reuse a layer in the Compositor by using `aside { }`.
 *
 * The `aside` block can make use of `draw`, `mask` and `post`. In this demo
 * only the latter is used to apply a full-window animated `Checkers` effect.
 * The `aside` is not displayed by default.
 *
 * Next, a white, centered circle is drawn.
 *
 * Finally, a `HashBlur` post-processing effect is applied. Its `radiusScale` preset
 * reads the red component of the aside (containing the animated checkers in this case),
 * which multiplies the blur radius per pixel.
 */
fun main() = application {
    configure {
        width = 720
        height = 720
    }
    program {
        val c = compose {
            layer {
                val a = aside(colorType = ColorType.FLOAT32) {
                    post(Checkers()) {
                        this.size = cos(seconds + 2.0) * 0.5 + 0.5
                    }
                }
                draw {
                    drawer.clear(ColorRGBa.GRAY.shade(0.5))
                    drawer.circle(width / 2.0, height / 2.0, 100.0)
                }
                post(HashBlur()) {
                    radiusScale = TextureScalar(a.result)
                    time = seconds
                    radius = 25.0
                }
            }
        }
        extend {
            c.draw(drawer)
        }
    }
}
