import org.openrndr.MouseTracker
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.extra.jumpfill.DirectionalField
import org.openrndr.extra.noise.simplex
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.shape.Rectangle

/**
 * Shows how to use the [DirectionalField] filter.
 * Draws moving white shapes on black background,
 * then applies the DirectionalField filter which returns a [ColorBuffer] in which
 * the red and green components encode the direction to the closest black/white edge.
 *
 * Hold down a mouse button to see the raw animation.
 */
fun main() = application {
    configure {
        width = 1024
        height = 1024
    }

    program {
        val rt = renderTarget(width, height) { colorBuffer() }
        val directionalField = DirectionalField().also {
            it.distanceScale = 0.004
        }

        // Needs to be FLOAT32 so we can have negative values
        val result = colorBuffer(width, height, type = ColorType.FLOAT32)
        val shader = shadeStyle {
            fragmentTransform = """
                x_fill.rgb = vec3(x_fill.rg + 0.5, x_fill.b);
                
                // interesting when distanceScale = 1.0
                //x_fill.rgb = vec3(1.0 / (x_fill.r + x_fill.g));
            """
        }
        val mouseTracker = MouseTracker(mouse)

        extend {
            // Draw moving white shapes on a black background
            drawer.isolatedWithTarget(rt) {
                clear(ColorRGBa.BLACK)
                stroke = null
                fill = ColorRGBa.WHITE
                repeat(10) {
                    val pos = Vector2.simplex(it, seconds * 0.2) *
                            bounds.center + bounds.center
                    val size = (it * it + 5.0) * 2.0

                    isolated {
                        translate(pos)
                        if (it % 2 == 0) {
                            circle(Vector2.ZERO, size)
                        } else {
                            rotate(Vector3.UNIT_Z, pos.x)
                            rectangle(
                                Rectangle.fromCenter(
                                    Vector2.ZERO, size, size * 2
                                )
                            )
                        }
                    }
                }
            }

            directionalField.apply(rt.colorBuffer(0), result)

            drawer.isolated {
                if (mouseTracker.pressedButtons.isEmpty()) {
                    shadeStyle = shader
                    image(result)
                } else {
                    image(rt.colorBuffer(0))
                }
            }
        }
    }
}