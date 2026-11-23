import org.openrndr.KEY_ARROW_DOWN
import org.openrndr.KEY_ARROW_LEFT
import org.openrndr.KEY_ARROW_RIGHT
import org.openrndr.KEY_ARROW_UP
import org.openrndr.WindowMultisample
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.loadFont
import org.openrndr.extra.gradientdescent.minimize
import org.openrndr.math.Vector2
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.sin

/**
 * A Doyle spiral: circles of which every one touches six others, centered at a^m b^n for two complex numbers a and b,
 * each of a radius r times the distance of its center to the center of the spiral. That the circles touch is three
 * equations, of the circles at 1, a and b, and that the spiral closes after p steps of a and q of b is a fourth,
 * a^p = b^q: these are solved for a, b and r with gradient descent, [minimize], from a hexagonal grid of circles.
 * The arrow keys change p and q, which solves the spiral of them.
 *
 * The spiral is the same when turned and scaled by a or b, which dragging shows: any place can be grabbed, which turns
 * and zooms the view such that it follows the mouse, see [DragParameters.grab], in and out without end.
 */
fun main() = application {
    configure {
        width = 720
        height = 720
        multisample = WindowMultisample.SampleCount(4)
    }
    program {
        val font = loadFont("demo-data/fonts/IBMPlexMono-Regular.ttf", 16.0)

        // complex numbers as vectors
        fun mul(a: Vector2, b: Vector2) = Vector2(a.x * b.x - a.y * b.y, a.x * b.y + a.y * b.x)
        fun div(a: Vector2, b: Vector2) = Vector2(a.x * b.x + a.y * b.y, a.y * b.x - a.x * b.y) / b.squaredLength
        fun exp(z: Vector2) = Vector2(cos(z.y), sin(z.y)) * exp(z.x)

        /** a, b and r of the Doyle spiral of [p] and [q], from the logarithms of a and b, or null when there is none */
        fun doyle(p: Int, q: Int): Triple<Vector2, Vector2, Double>? {
            // a hexagonal grid in the logarithms of the centers, of which p steps of a and q of b go once around
            val hexagon = Vector2(cos(PI / 3), sin(PI / 3))
            val around = Vector2(p.toDouble(), 0.0) - hexagon * q.toDouble()
            val length = 2 * PI / around.length
            val direction = PI / 2 - atan2(around.y, around.x)
            val logA = Vector2(cos(direction), sin(direction)) * length
            val logB = mul(logA, hexagon)

            /** how far the circles at 1, a and b are from touching, and the spiral from closing, squared */
            fun residual(x: DoubleArray): Double {
                val a = exp(Vector2(x[0], x[1]))
                val b = exp(Vector2(x[2], x[3]))
                val r = x[4]
                val one = Vector2(1.0, 0.0)
                val touch = listOf(
                    a.distanceTo(one) - r * (a.length + 1),
                    b.distanceTo(one) - r * (b.length + 1),
                    a.distanceTo(b) - r * (a.length + b.length)
                )
                // a^p = b^q, in logarithms: the sizes are equal and the angles once around apart
                val close = listOf(p * x[0] - q * x[2], p * x[1] - q * x[3] - 2 * PI)
                return (touch + close).sumOf { it * it }
            }

            val solution = minimize(doubleArrayOf(logA.x, logA.y, logB.x, logB.y, length / 2)) { residual(it) }
            if (solution.value > 1E-12 || solution.solution[4] <= 0.0) return null
            val (ax, ay, bx, by, r) = solution.solution
            return Triple(Vector2(ax, ay), Vector2(bx, by), r)
        }

        var p = 5
        var q = 8
        var spiral = doyle(p, q)
        keyboard.keyDown.listen {
            when (it.key) {
                KEY_ARROW_UP -> p++
                KEY_ARROW_DOWN -> p = (p - 1).coerceAtLeast(1)
                KEY_ARROW_RIGHT -> q++
                KEY_ARROW_LEFT -> q = (q - 1).coerceAtLeast(1)
            }
            spiral = doyle(p, q)
        }

        // the view: the logarithm of how much it is scaled and how far it is turned
        val drag = extend(DragParameters(doubleArrayOf(ln(200.0), 0.0), drawer.bounds.center) { emptyList() })

        /** the view of [z], of the parameters [x] */
        fun view(z: Vector2, x: DoubleArray) = mul(exp(Vector2(x[0], x[1])), z)

        // any place is grabbed: the place in the spiral that the view shows at the mouse
        drag.grab = { position ->
            val point = div(position, exp(Vector2(drag.parameters[0], drag.parameters[1])))
            val place: (DoubleArray) -> Vector2 = { x -> view(point, x) }
            if (point.length > 1E-9) place else null
        }

        extend {
            drawer.translate(drag.origin)
            val s = spiral
            if (s != null) {
                val (logA, logB, r) = s
                // every circle once: m from 0 to p, since p steps of a and q of b go once around
                for (m in 0 until p) {
                    val centers = mutableListOf<Vector2>()
                    val radii = mutableListOf<Double>()
                    for (n in -400..400) {
                        val z = view(exp(logA * m.toDouble() + logB * n.toDouble()), drag.parameters)
                        val radius = r * z.length
                        if (radius < 0.4 || z.length - radius > 520.0) continue
                        centers.add(z)
                        radii.add(radius)
                    }
                    drawer.stroke = ColorRGBa.BLACK
                    drawer.fill = ColorRGBa.fromHex(if (m % 2 == 0) "#f0a030" else "#5ab0ff")
                    drawer.circles(centers, radii)
                }
            }
            drawer.fontMap = font
            drawer.fill = ColorRGBa.WHITE
            drawer.text(if (s == null) "($p, $q): no Doyle spiral" else "($p, $q)  r = ${"%.4f".format(s.third)}", -340.0, 340.0)
        }
    }
}
