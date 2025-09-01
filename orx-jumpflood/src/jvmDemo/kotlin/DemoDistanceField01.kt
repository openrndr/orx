import org.openrndr.MouseTracker
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.extra.jumpfill.DistanceField
import org.openrndr.extra.noise.simplex
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.shape.Rectangle

/**
 * Shows how to use the [DistanceField] filter.
 *
 * Draws moving white shapes on black background,
 * then applies the DistanceField filter which returns a [ColorBuffer] in which
 * the red component encodes the distance to the closest black/white edge.
 *
 * The value of the green component is negative when on the black background
 * and positive when inside white shapes. The sign is used in the [shadeStyle] to choose
 * between two colors.
 *
 * The inverse of the distance is used to obtain a non-linear brightness.
 *
 * Hold down a mouse button to see the raw animation.
 */
fun main() = application {
    configure {
        width = 720
        height = 720
    }

    program {
        val rt = renderTarget(width, height) { colorBuffer() }
        val distanceField = DistanceField()

        // Needs to be FLOAT32 so we can have negative values
        val result = colorBuffer(width, height, type = ColorType.FLOAT32)
        val shader = shadeStyle {
            fragmentTransform = """
                float distance = abs(x_fill.r);
                float bri = 1.0 / (1.0 + 0.03 * distance);
                
                // wavy effect
                // bri *= (1.0 + 0.2 * sin(distance * 0.2));
                
                x_fill.rgb = bri * (x_fill.g > 0.0 ? vec3(1.0, 0.0, 0.0) : vec3(0.0, 1.0, 1.0));
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

            distanceField.apply(rt.colorBuffer(0), result)

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