import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.ColorType
import org.openrndr.draw.createEquivalent
import org.openrndr.drawImage
import org.openrndr.extra.color.colormatrix.constant
import org.openrndr.extra.color.colormatrix.tint
import org.openrndr.extra.jumpfill.DirectionalField
import org.openrndr.extra.noise.scatter
import org.openrndr.math.IntVector2
import org.openrndr.math.Vector3
import org.openrndr.math.clamp

/**
 * Demonstrates how use the `DirectionalField` effect to create
 * a `ColorBuffer` in which the RGB components encode direction and distance to the closest
 * edge of every pixel in an input `ColorBuffer`.
 *
 * The program draws scattered white circles on a `ColorBuffer`, then applies the `DistanceField()`
 * effect and renders the static result on every animation frame.
 *
 * Additionally, it uses the shadow (CPU version of the texture) to query the distance field texture
 * at current mouse position. The resulting blue color component is used as the radius of a circle
 * centered at the mouse position. The red and green components are used to draw a line to the
 * black/white edge closest to the mouse pointer.
 */
fun main() = application {
    configure {
        width = 720
        height = 720
    }

    program {
        val input = drawImage(width, height, contentScale = 1.0) {
            val points = drawer.bounds.scatter(100.0)
            drawer.circles(points, 50.0)
        }

        val directionalField = DirectionalField()
        val ddf = input.createEquivalent(type = ColorType.FLOAT32)

        directionalField.signedMagnitude = true
        directionalField.unitDirection = true
        directionalField.apply(input, ddf)

        ddf.shadow.download()
        extend {
            val p = (mouse.position * ddf.contentScale).toInt().clamp(
                IntVector2.ZERO,
                IntVector2(width - 1, height - 1)
            )
            val c = ddf.shadow[p.x, p.y]
            val sdf3 = Vector3(c.r, c.g, c.b)

            drawer.drawStyle.colorMatrix = constant(ColorRGBa.WHITE.shade(0.5)) * tint(ColorRGBa.WHITE.shade(0.5))

            drawer.image(ddf)
            drawer.fill = null
            drawer.stroke = ColorRGBa.WHITE

            drawer.circle(mouse.position, sdf3.z / ddf.contentScale)
            drawer.lineSegment(mouse.position, mouse.position + sdf3.xy * sdf3.z)
        }
    }
}