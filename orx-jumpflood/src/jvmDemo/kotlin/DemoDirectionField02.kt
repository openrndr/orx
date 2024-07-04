import org.openrndr.MouseTracker
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.drawImage
import org.openrndr.extra.jumpfill.DirectionalField
import org.openrndr.extra.noise.scatter
import org.openrndr.extra.noise.simplex
import org.openrndr.math.IntVector2
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.math.clamp
import org.openrndr.shape.Rectangle
import kotlin.math.abs

/**
 * Create directional distance field and demonstrate signed distance
 */
fun main() = application {
    configure {
        width = 1024
        height = 1024
    }

    program {
        val input = drawImage(width, height, contentScale = 1.0) {
            val points = drawer.bounds.scatter(100.0)
            drawer.circles(points, 50.0)
        }

        val filter = DirectionalField()
        val ddf = input.createEquivalent(type = ColorType.FLOAT32)

        filter.signedMagnitude = true
        filter.unitDirection = true
        filter.apply(input, ddf)

        ddf.shadow.download()
        extend {
            val p = (mouse.position * ddf.contentScale).toInt().clamp(IntVector2.ZERO, IntVector2(width-1, height-1))
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