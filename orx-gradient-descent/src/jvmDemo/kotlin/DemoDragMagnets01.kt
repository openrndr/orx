import org.openrndr.WindowMultisample
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.LineCap
import org.openrndr.draw.isolated
import org.openrndr.math.Vector2
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Magnetic field lines of three bar magnets, each two poles of opposite strength of which the field falls off with the
 * distance, as it does in two dimensions; the lines are followed from around every north pole until they reach a
 * south pole or leave the window. Compasses at fixed places point along the field.
 *
 * The parameters are where the magnets are and how they are turned. The center of a magnet moves it, its north pole
 * turns it, see [DragParameters.affects]; the needle of a compass can be dragged too, which moves and turns the magnets,
 * as little as they can, such that the field at the compass points to the mouse.
 */
fun main() = application {
    configure {
        width = 720
        height = 720
        multisample = WindowMultisample.SampleCount(4)
    }
    program {
        val magnets = 3
        val length = 110.0
        val compasses = listOf(Vector2(-230.0, 200.0), Vector2(230.0, -220.0), Vector2(0.0, -250.0), Vector2(240.0, 230.0))

        /** the poles of the magnets of the parameters [x]: where they are and how strong, north positive */
        fun poles(x: DoubleArray): List<Pair<Vector2, Double>> = (0 until magnets).flatMap { m ->
            val center = Vector2(x[3 * m], x[3 * m + 1])
            val half = Vector2(cos(x[3 * m + 2]), sin(x[3 * m + 2])) * (length / 2)
            listOf(center + half to 1.0, center - half to -1.0)
        }

        /** the field at [p], away from north poles and towards south poles */
        fun field(p: Vector2, poles: List<Pair<Vector2, Double>>): Vector2 {
            var b = Vector2.ZERO
            for ((c, q) in poles) {
                val d = p - c
                b += d * (q / d.squaredLength)
            }
            return b
        }

        /** the field line from [start], in steps along the field until it reaches a south pole or leaves the window */
        fun line(start: Vector2, poles: List<Pair<Vector2, Double>>): List<Vector2> {
            val step = 4.0
            fun direction(p: Vector2) = field(p, poles).normalized
            val points = mutableListOf(start)
            var p = start
            repeat(700) {
                // a step of Runge and Kutta, of the fourth order
                val k1 = direction(p)
                val k2 = direction(p + k1 * (step / 2))
                val k3 = direction(p + k2 * (step / 2))
                val k4 = direction(p + k3 * step)
                p += (k1 + k2 * 2.0 + k3 * 2.0 + k4) * (step / 6)
                if (!p.x.isFinite() || p.x !in -420.0..420.0 || p.y !in -420.0..420.0) return points
                points.add(p)
                if (poles.any { (c, q) -> q < 0 && c.distanceTo(p) < step }) return points
            }
            return points
        }

        /** the tip of the needle of compass [c], which points along the field */
        fun needle(c: Vector2, poles: List<Pair<Vector2, Double>>) = c + field(c, poles).normalized * 30.0

        val drag = extend(DragParameters(doubleArrayOf(-150.0, -60.0, 0.2, 140.0, 30.0, 2.5, 0.0, 170.0, -0.9), drawer.bounds.center) { x ->
            val poles = poles(x)
            (0 until magnets).flatMap { m -> listOf(Vector2(x[3 * m], x[3 * m + 1]), poles[2 * m].first) } +
                compasses.map { needle(it, poles) }
        })
        // the center of a magnet moves it, its north pole turns it, a needle moves and turns them all
        drag.affects = {
            when {
                it < 2 * magnets && it % 2 == 0 -> intArrayOf(3 * (it / 2), 3 * (it / 2) + 1)
                it < 2 * magnets -> intArrayOf(3 * (it / 2) + 2)
                else -> null
            }
        }
        drag.damping = 0.05

        extend {
            drawer.clear(ColorRGBa.fromHex("#101418"))
            drawer.translate(drag.origin)
            val x = drag.parameters
            val poles = poles(x)

            // the field lines, from around every north pole
            drawer.stroke = ColorRGBa.fromHex("#9fc8e8").opacify(0.55)
            for ((c, q) in poles) {
                if (q < 0) continue
                for (i in 0 until 16) {
                    val a = i * 2 * PI / 16
                    drawer.lineStrip(line(c + Vector2(cos(a), sin(a)) * 6.0, poles))
                }
            }

            // the magnets, the north half red
            for (m in 0 until magnets) {
                drawer.isolated {
                    translate(x[3 * m], x[3 * m + 1])
                    rotate(Math.toDegrees(x[3 * m + 2]))
                    stroke = null
                    fill = ColorRGBa.fromHex("#d0453a")
                    rectangle(0.0, -12.0, length / 2 + 8, 24.0)
                    fill = ColorRGBa.fromHex("#3a6bd0")
                    rectangle(-length / 2 - 8, -12.0, length / 2 + 8, 24.0)
                }
            }

            // the compasses
            drawer.lineCap = LineCap.ROUND
            for (c in compasses) {
                val tip = needle(c, poles)
                drawer.fill = ColorRGBa.fromHex("#e8e0d0")
                drawer.stroke = ColorRGBa.BLACK
                drawer.circle(c, 36.0)
                drawer.strokeWeight = 5.0
                drawer.stroke = ColorRGBa.fromHex("#3a6bd0")
                drawer.lineSegment(c, c * 2.0 - tip)
                drawer.stroke = ColorRGBa.fromHex("#d0453a")
                drawer.lineSegment(c, tip)
                drawer.strokeWeight = 1.0
            }
            drag.drawPoints(drawer, ColorRGBa.WHITE.opacify(0.8), ColorRGBa.WHITE)
        }
    }
}
