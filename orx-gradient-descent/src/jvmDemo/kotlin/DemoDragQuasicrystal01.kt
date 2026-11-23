import org.openrndr.KEY_ARROW_DOWN
import org.openrndr.KEY_ARROW_UP
import org.openrndr.WindowMultisample
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.loadFont
import org.openrndr.draw.shadeStyle
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * A quasicrystal as a density: the sum of N plane waves of the same wavelength in N directions around the circle,
 * of which the bright spots, the atoms, have N-fold symmetry but repeat nowhere when N is not 3, 4 or 6, which give
 * crystals. The arrow keys change N.
 *
 * The parameters are the phases of the waves. A bright spot can be dragged, see [DragParameters.grab]: the phases
 * change as little as they can to move it to the mouse, which shifts the pattern partly and rearranges it partly,
 * a phason. Where the spot is for some phases is found by Newton's method, climbing the density from where it was.
 */
fun main() = application {
    configure {
        width = 720
        height = 720
        multisample = WindowMultisample.SampleCount(4)
    }
    program {
        val font = loadFont("demo-data/fonts/IBMPlexMono-Regular.ttf", 16.0)
        val most = 13
        val wavelength = 42.0
        var n = 5

        /** the wave vectors of [n] waves */
        fun waves(n: Int) = (0 until n).map { j -> Vector2(cos(2 * PI * j / n), sin(2 * PI * j / n)) * (2 * PI / wavelength) }
        var k = waves(n)

        /** the density at [p] for the phases [x], its gradient and its Hessian, as xx, xy and yy */
        fun density(p: Vector2, x: DoubleArray): Triple<Double, Vector2, Vector3> {
            var f = 0.0
            var g = Vector2.ZERO
            var h = Vector3.ZERO
            for (j in 0 until n) {
                val a = k[j].dot(p) + x[j]
                val c = cos(a)
                f += c
                g -= k[j] * sin(a)
                h -= Vector3(k[j].x * k[j].x, k[j].x * k[j].y, k[j].y * k[j].y) * c
            }
            return Triple(f, g, h)
        }

        /** the bright spot near [start] for the phases [x], by Newton's method, or gradient ascent where that does not climb */
        fun spot(start: Vector2, x: DoubleArray): Vector2 {
            var p = start
            repeat(8) {
                val (_, g, h) = density(p, x)
                val det = h.x * h.z - h.y * h.y
                // a maximum where the Hessian is negative definite
                p = if (h.x < 0 && det > 0) {
                    p - Vector2(h.z * g.x - h.y * g.y, -h.y * g.x + h.x * g.y) / det
                } else {
                    p + g * (wavelength * wavelength / (4 * PI * PI * n))
                }
            }
            return p
        }

        val drag = extend(DragParameters(DoubleArray(most), drawer.bounds.center) { emptyList() })
        drag.damping = 1.0
        drag.affects = { IntArray(n) { it } }

        // the spot that is dragged, where it is now
        var dragged: Vector2? = null
        drag.grab = { position ->
            val p = spot(position, drag.parameters)
            // only bright spots are atoms
            if (p.distanceTo(position) < wavelength / 3 && density(p, drag.parameters).first > 0.4 * n) {
                dragged = p
                val place: (DoubleArray) -> Vector2 = { x -> spot(dragged ?: p, x) }
                place
            } else null
        }
        mouse.buttonUp.listen { dragged = null }

        keyboard.keyDown.listen {
            when (it.key) {
                KEY_ARROW_UP -> n = (n + 1).coerceAtMost(most)
                KEY_ARROW_DOWN -> n = (n - 1).coerceAtLeast(3)
            }
            k = waves(n)
        }

        val style = shadeStyle {
            fragmentTransform = """
                float f = 0.0;
                for (int i = 0; i < p_count; ++i) {
                    vec3 w = p_waves[i];
                    f += cos(dot(w.xy, v_worldPosition.xy - p_origin) + w.z);
                }
                float v = clamp(0.5 + 0.5 * f / float(p_count), 0.0, 1.0);
                x_fill.rgb = mix(vec3(0.03, 0.06, 0.12), vec3(1.0, 0.85, 0.6), smoothstep(0.45, 0.95, v));
            """
        }

        extend {
            dragged = dragged?.let { spot(it, drag.parameters) }
            drawer.translate(drag.origin)
            style.parameter("waves", Array(most) { j ->
                if (j < n) Vector3(k[j].x, k[j].y, drag.parameters[j]) else Vector3.ZERO
            })
            style.parameter("count", n)
            // the shader sees the window, the density is around the center
            style.parameter("origin", drag.origin)
            drawer.shadeStyle = style
            drawer.stroke = null
            drawer.rectangle(-360.0, -360.0, 720.0, 720.0)
            drawer.shadeStyle = null

            dragged?.let {
                drawer.fill = null
                drawer.stroke = ColorRGBa.PINK
                drawer.strokeWeight = 2.0
                drawer.circle(it, wavelength / 3)
            }
            drawer.fontMap = font
            drawer.fill = ColorRGBa.BLACK.opacify(0.7)
            drawer.stroke = null
            drawer.rectangle(-360.0, 320.0, 720.0, 40.0)
            drawer.fill = ColorRGBa.WHITE
            drawer.text("$n waves${if (n == 3 || n == 4 || n == 6) ", a crystal" else ", a quasicrystal"} (arrow keys)", -340.0, 340.0)
        }
    }
}
