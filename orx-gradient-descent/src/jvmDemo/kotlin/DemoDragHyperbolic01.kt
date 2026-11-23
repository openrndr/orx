import org.openrndr.WindowMultisample
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.math.Vector2
import org.openrndr.shape.ShapeContour
import kotlin.math.PI
import kotlin.math.acosh
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan
import kotlin.math.tanh

/**
 * The tiling of the hyperbolic plane by heptagons, three around every corner, in the Poincaré disk, seen through a
 * Möbius transformation of three parameters: a translation inside the disk and a rotation. Dragging any corner, or
 * any other place of the plane, changes the transformation, see [DragParameters], so that place follows the mouse and
 * the tiling slides along with it: easily near the center, hardly near the rim, which is infinitely far away in the
 * hyperbolic plane.
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
        fun conj(a: Vector2) = Vector2(a.x, -a.y)
        val one = Vector2(1.0, 0.0)

        /** the Möbius transformation that moves 0 to [a] */
        fun move(z: Vector2, a: Vector2) = div(z + a, one + mul(conj(a), z))

        /** [z] reflected in the geodesic through [p] and [q], a line through the center or a circle square to the rim */
        fun reflect(z: Vector2, p: Vector2, q: Vector2): Vector2 {
            val det = p.x * q.y - p.y * q.x
            if (kotlin.math.abs(det) < 1E-12) {
                val d = (q - p).normalized
                return p + d * (2 * (z - p).dot(d)) - (z - p)
            }
            // the center of the circle through p, q and the inversion of p in the rim
            val bp = (p.squaredLength + 1) / 2
            val bq = (q.squaredLength + 1) / 2
            val center = Vector2(bp * q.y - bq * p.y, p.x * bq - q.x * bp) / det
            val r2 = center.squaredLength - 1
            val d = z - center
            return center + d * (r2 / d.squaredLength)
        }

        // the tiling {p, q}: the central polygon, reflected in its edges, breadth first, up to near the rim
        val p = 7
        val q = 3
        val radius = tanh(acosh(1 / (tan(PI / p) * tan(PI / q))) / 2)
        val central = (0 until p).map { Vector2(cos(2 * PI * it / p), sin(2 * PI * it / p)) * radius }

        class Tile(val corners: List<Vector2>, val center: Vector2, val depth: Int)

        val tiles = mutableListOf(Tile(central, Vector2.ZERO, 0))
        val seen = hashSetOf(Pair(0L, 0L))
        fun key(v: Vector2) = Pair(Math.round(v.x * 1E6), Math.round(v.y * 1E6))
        var next = 0
        while (next < tiles.size && tiles.size < 1500) {
            val tile = tiles[next++]
            for (i in 0 until p) {
                val a = tile.corners[i]
                val b = tile.corners[(i + 1) % p]
                val center = reflect(tile.center, a, b)
                if (center.length > 0.985 || !seen.add(key(center))) continue
                tiles.add(Tile(tile.corners.map { reflect(it, a, b) }, center, tile.depth + 1))
            }
        }
        // the corners, each once
        val corners = tiles.flatMap { it.corners }.distinctBy(::key)

        /** the translation of the transformation, inside the disk for all parameters */
        fun translation(x: Double, y: Double): Vector2 {
            val v = Vector2(x, y)
            val l = v.length
            return if (l < 1E-12) v else v * (tanh(l) / l)
        }

        /** the transformation of the view: a rotation by [angle] after the translation */
        fun view(z: Vector2, x: Double, y: Double, angle: Double) =
            mul(Vector2(cos(angle), sin(angle)), move(z, translation(x, y)))

        val scale = 340.0
        val drag = extend(DragParameters(doubleArrayOf(0.0, 0.0, 0.0), drawer.bounds.center) { (x, y, angle) ->
            corners.map { view(it, x, y, angle) * scale }
        })
        drag.reach = 8.0
        // the view has a parameter more than a corner needs: the smallest change is taken
        drag.damping = 1000.0
        // away from the corners, the point of the plane under the mouse is grabbed: where the view shows it
        drag.grab = { position ->
            val (x, y, angle) = drag.parameters
            val w = position / scale
            if (w.length >= 1.0) null else {
                // the point that the view shows at the mouse, the view undone: the rotation, then the translation
                val point = move(mul(w, Vector2(cos(angle), -sin(angle))), -translation(x, y))
                val place: (DoubleArray) -> Vector2 = { (x, y, angle) -> view(point, x, y, angle) * scale }
                place
            }
        }

        /** points along the geodesic from [a] to [b]: a straight line once [a] is moved to the center */
        fun geodesic(a: Vector2, b: Vector2, n: Int = 12): List<Vector2> {
            val m = move(b, -a)
            return (0..n).map { move(m * (it.toDouble() / n), a) }
        }

        extend {
            drawer.clear(ColorRGBa.BLACK)
            drawer.translate(drag.origin)
            val (x, y, angle) = drag.parameters
            fun shown(z: Vector2) = view(z, x, y, angle) * scale

            drawer.stroke = null
            drawer.fill = ColorRGBa.fromHex("#14283c")
            drawer.circle(Vector2.ZERO, scale)
            // the tiles, in two shades by the number of reflections from the central one
            val outlines = tiles.map { tile ->
                val outline = (0 until p).flatMap { i ->
                    geodesic(tile.corners[i], tile.corners[(i + 1) % p], 6).dropLast(1)
                }.map(::shown)
                tile.depth % 2 to ShapeContour.fromPoints(outline, closed = true)
            }
            drawer.stroke = ColorRGBa.WHITE.opacify(0.5)
            for ((parity, color) in listOf(0 to "#2d5f8a", 1 to "#244a6b")) {
                drawer.fill = ColorRGBa.fromHex(color)
                drawer.contours(outlines.filter { it.first == parity }.map { it.second })
            }
            drawer.fill = null
            drawer.stroke = ColorRGBa.WHITE.opacify(0.6)
            drawer.circle(Vector2.ZERO, scale)
            // only the corner under the mouse is drawn, all of them can be dragged
            drag.drawPoints(drawer, ColorRGBa.TRANSPARENT)
        }
    }
}
