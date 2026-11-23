import org.openrndr.WindowMultisample
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.math.Vector2
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin

/**
 * A loxodromic Möbius transformation: it moves every point away from one fixed point, the source, and towards the
 * other, the sink, while turning around them, by a complex multiplier k. In coordinates where the source is 0 and
 * the sink infinity it is multiplication by k, so chains of circles, each the image of the one before, spiral out of
 * the source into the sink, along loxodromes.
 *
 * The parameters are the two fixed points, the logarithm of the size of k, the angle of k and the size of the seed
 * circles. The fixed points can be dragged, the centers of the first images of the seed change k, the rim of a seed
 * circle its size, see [DragParameters] and [DragParameters.affects].
 */
fun main() = application {
    configure {
        width = 720
        height = 720
        multisample = WindowMultisample.SampleCount(4)
    }
    program {
        // complex numbers as vectors
        fun mul(a: Vector2, b: Vector2) = Vector2(a.x * b.x - a.y * b.y, a.x * b.y + a.y * b.x)
        fun div(a: Vector2, b: Vector2) = Vector2(a.x * b.x + a.y * b.y, a.y * b.x - a.x * b.y) / b.squaredLength
        val one = Vector2(1.0, 0.0)

        /** the transformation of the parameters [x], as maps between the plane and the coordinates of the spirals */
        class Loxodromic(x: DoubleArray) {
            val source = Vector2(x[0], x[1])
            val sink = Vector2(x[2], x[3])
            val logSize = x[4]
            val angle = x[5]
            val seed = x[6]

            /** from coordinates where the source is 0 and the sink infinity to the plane */
            fun plane(w: Vector2) = div(source - mul(sink, w), one - w)

            /** k to the power [t]: the transformation [t] times, also in between */
            fun k(t: Double) = Vector2(cos(t * angle), sin(t * angle)) * exp(t * logSize)

            /** the image of the circle of [center] and [radius] in the coordinates of the spirals, null when it is not a circle in view */
            fun circle(center: Vector2, radius: Double): Pair<Vector2, Double>? {
                // a circle around 1 goes through infinity, its image is the outside of a circle
                if (center.distanceTo(one) <= radius) return null
                val (p, q, r) = (0 until 3).map { plane(center + Vector2(cos(it * 2 * PI / 3), sin(it * 2 * PI / 3)) * radius) }
                val d = 2 * (p.x * (q.y - r.y) + q.x * (r.y - p.y) + r.x * (p.y - q.y))
                val c = Vector2(
                    (p.squaredLength * (q.y - r.y) + q.squaredLength * (r.y - p.y) + r.squaredLength * (p.y - q.y)) / d,
                    (p.squaredLength * (r.x - q.x) + q.squaredLength * (p.x - r.x) + r.squaredLength * (q.x - p.x)) / d
                )
                val radiusInPlane = c.distanceTo(p)
                return if (radiusInPlane.isFinite() && radiusInPlane in 0.3..3000.0) c to radiusInPlane else null
            }
        }

        // three spirals, seeded on the circle of points as far from the source as from the sink
        val arms = (0 until 3).map { -Vector2(cos(it * 2 * PI / 3), sin(it * 2 * PI / 3)) }
        val colors = listOf(ColorRGBa.PINK, ColorRGBa.fromHex("#5ab0ff"), ColorRGBa.fromHex("#f5d76e"))

        val drag = extend(DragParameters(doubleArrayOf(-160.0, 20.0, 170.0, -20.0, 0.22, 0.45, 0.22), drawer.bounds.center) { x ->
            val l = Loxodromic(x)
            listOf(l.source, l.sink) + (1..3).map { l.plane(mul(arms[0], l.k(it.toDouble()))) } +
                l.plane(arms[0] * (1 + l.seed))
        })
        drag.affects = {
            when (it) {
                0 -> intArrayOf(0, 1)
                1 -> intArrayOf(2, 3)
                in 2..4 -> intArrayOf(4, 5)
                else -> intArrayOf(6)
            }
        }
        drag.damping = 1.0

        extend {
            drawer.translate(drag.origin)
            val l = Loxodromic(drag.parameters)
            for ((arm, w0) in arms.withIndex()) {
                // the loxodrome through the seed
                drawer.stroke = colors[arm].opacify(0.35)
                drawer.lineStrip((-400..400).map { l.plane(mul(w0, l.k(it / 20.0))) }.filter { it.length < 2000.0 })
                // the seed and its images, the transformation applied to it n times
                drawer.fill = colors[arm].opacify(0.25)
                drawer.stroke = colors[arm]
                val circles = (-60..60).mapNotNull { n ->
                    val k = l.k(n.toDouble())
                    l.circle(mul(w0, k), l.seed * k.length)
                }
                drawer.circles(circles.map { it.first }, circles.map { it.second })
            }
            drawer.stroke = null
            drawer.fill = ColorRGBa.WHITE
            drawer.circles(listOf(l.source, l.sink), 6.0)
            drag.drawPoints(drawer)
        }
    }
}
