import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.BufferMultisample
import org.openrndr.extra.compositor.blend
import org.openrndr.extra.compositor.compose
import org.openrndr.extra.compositor.draw
import org.openrndr.extra.compositor.layer
import org.openrndr.extra.fx.blend.Normal
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle

/**
 * Demonstration of using [BufferMultisample] on a per layer basis.
 * Try changing which layer has multisampling applied and observe the results.
 */
fun main() = application {
    configure {
        width = 720
        height = 720
    }

    program {
        val layers = compose {
            layer(multisample = BufferMultisample.SampleCount(4)) {
                draw {
                    drawer.translate(drawer.bounds.center)
                    drawer.rotate(seconds + 5)
                    drawer.fill = ColorRGBa.PINK
                    drawer.rectangle(Rectangle.fromCenter(Vector2.ZERO, 200.0))
                }

                layer() {
                    blend(Normal()) {
                        clip = true
                    }
                    draw {
                        drawer.rotate((seconds + 5) * -2)
                        drawer.fill = ColorRGBa.WHITE
                        drawer.rectangle(Rectangle.fromCenter(Vector2.ZERO, 200.0))
                    }
                }
            }
        }

        extend {
            drawer.clear(ColorRGBa.WHITE)
            layers.draw(drawer)
        }
    }
}