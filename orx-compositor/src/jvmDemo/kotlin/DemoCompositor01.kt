import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.color.rgb
import org.openrndr.draw.Drawer
import org.openrndr.extra.compositor.compose
import org.openrndr.extra.compositor.draw
import org.openrndr.extra.compositor.layer
import org.openrndr.extra.compositor.post
import org.openrndr.extra.fx.blur.ApproximateGaussianBlur
import org.openrndr.math.Vector3
import kotlin.random.Random

/**
 * Compositor demo showing 3 layers of moving items
 * with a different amount of blur in each layer,
 * simulating depth of field
 */
fun main() = application {
    configure {
        width = 720
        height = 720
    }

    program {
        data class Item(var pos: Vector3, val color: ColorRGBa) {
            fun draw(drawer: Drawer) {
                pos -= Vector3(pos.z * 3.0, 0.0, 0.0)
                if (pos.x < -260.0) {
                    pos = Vector3(width + 260.0, pos.y, pos.z)
                }
                drawer.fill = null
                drawer.stroke = color
                drawer.strokeWeight = 2.0 + 30.0 * pos.z * pos.z
                drawer.circle(pos.xy, 10.0 + 250.0 * pos.z * pos.z)
            }
        }

        val items = List(50) {
            val pos = Vector3(Random.nextDouble() * width,
                    Random.nextDouble(0.3, 0.7) * height,
                    Random.nextDouble())
            Item(pos, ColorRGBa.PINK.shade(Random.nextDouble(0.2, 0.9)))
        }.sortedBy { it.pos.z }

        val composite = compose {
            layer {
                draw {
                    drawer.stroke = null
                    items.filter { it.pos.z < 0.33 }.forEach {
                        it.draw(drawer)
                    }
                }
                post(ApproximateGaussianBlur()) {
                    window = 25
                    sigma = 5.00
                }
            }

            layer {
                draw {
                    drawer.stroke = null
                    items.filter { it.pos.z in 0.33..0.66 }.forEach {
                        it.draw(drawer)
                    }
                }
            }

            layer {
                draw {
                    drawer.stroke = null
                    items.filter { it.pos.z > 0.66 }.forEach {
                        it.draw(drawer)
                    }
                }
                post(ApproximateGaussianBlur()) {
                    window = 25
                    sigma = 5.00
                }
            }
        }

        extend {
            drawer.clear(rgb(0.2))
            composite.draw(drawer)
        }
    }
}
