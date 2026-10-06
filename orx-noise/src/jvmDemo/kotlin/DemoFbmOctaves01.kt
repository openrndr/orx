import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.noise.fbm
import org.openrndr.extra.noise.simplex1D
import org.openrndr.math.Vector2
import org.openrndr.shape.ShapeContour

/**
 * Demonstrates the impact of `octaves` when using `fbm`.
 * The animated graph at the top uses only 1 octave, which makes it the smoothest.
 * The bottom one uses 8 octaves. By default, each additional octave has half the strength of the previous one
 * and double the frequency.
 */
fun main() = application {
    configure {
        width = 720
        height = 720
    }
    program {
        extend {
            drawer.clear(ColorRGBa.WHITE)
            for (octaves in 1..8) {
                val n = simplex1D.fbm(octaves)
                val c = ShapeContour.fromPoints(List(width) { x ->
                    Vector2(x.toDouble(), n(404, x * 0.01 + seconds) * 45.0 + 90.0 * octaves - 45.0)
                }, false)
                drawer.contour(c)
            }
        }
    }
}