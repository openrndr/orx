import org.openrndr.WindowMultisample
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.shape.Rectangle
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * The stereographic projection: a sphere with an icosahedron on it, left, and its projection from the north pole onto
 * the plane of the equator, right, which maps circles on the sphere to circles in the plane, so the edges of the
 * icosahedron, arcs of great circles, become arcs of circles. Turning the sphere moves the projection by a Möbius
 * transformation.
 *
 * The parameters are the turn of the sphere, as an axis times an angle. Any point of the sphere can be grabbed, in
 * either view, see [DragParameters.grab]: the sphere turns, as little as it can, to bring that point to the mouse.
 */
fun main() = application {
    configure {
        width = 1080
        height = 540
        multisample = WindowMultisample.SampleCount(4)
    }
    program {
        val sphereCenter = Vector2(270.0, 270.0)
        val sphereRadius = 220.0
        val planeCenter = Vector2(810.0, 270.0)
        val planeScale = 110.0
        val tilt = 0.45

        /** [p] turned by the axis times angle [v] */
        fun turn(p: Vector3, v: Vector3): Vector3 {
            val angle = v.length
            if (angle < 1E-12) return p
            val k = v / angle
            return p * cos(angle) + k.cross(p) * sin(angle) + k * (k.dot(p) * (1 - cos(angle)))
        }

        // the sphere is seen from a little above the equator, from -y, such that the north pole, z, shows
        val toViewer = Vector3(0.0, -cos(tilt), sin(tilt))
        val up = Vector3(0.0, sin(tilt), cos(tilt))

        /** where the sphere view shows [p] */
        fun sphereView(p: Vector3) = sphereCenter + Vector2(p.x, -p.dot(up)) * sphereRadius

        /** whether [p] is on the side of the sphere that faces the viewer */
        fun front(p: Vector3) = p.dot(toViewer) > 0.0

        /** the point of the sphere that the sphere view shows at [s], on the front, null outside the sphere */
        fun fromSphereView(s: Vector2): Vector3? {
            val q = (s - sphereCenter) / sphereRadius
            if (q.squaredLength >= 1.0) return null
            return Vector3(q.x, 0.0, 0.0) + up * -q.y + toViewer * sqrt(1 - q.squaredLength)
        }

        /**
         * the stereographic projection of [p] from the north pole onto the plane of the equator, in the plane view,
         * which sees the plane from above, y up like in the sphere view
         */
        fun planeView(p: Vector3) = planeCenter + Vector2(p.x, -p.y) * (planeScale / (1 - p.z))

        /** the point of the sphere that the plane view shows at [s] */
        fun fromPlaneView(s: Vector2): Vector3 {
            val u = Vector2(s.x - planeCenter.x, planeCenter.y - s.y) / planeScale
            val d = 1 + u.squaredLength
            return Vector3(2 * u.x / d, 2 * u.y / d, (u.squaredLength - 1) / d)
        }

        // the icosahedron: its corners and the edges between the nearest of them
        val phi = (1 + sqrt(5.0)) / 2
        val corners = listOf(-1.0, 1.0).flatMap { a ->
            listOf(-phi, phi).flatMap { b -> listOf(Vector3(0.0, a, b), Vector3(a, b, 0.0), Vector3(b, 0.0, a)) }
        }.map { it.normalized }
        val edges = corners.indices.flatMap { i ->
            corners.indices.filter { j -> j > i && corners[i].distanceTo(corners[j]) < 1.1 }.map { j -> i to j }
        }

        /** points along the great circle arc from [a] to [b] */
        fun arc(a: Vector3, b: Vector3, n: Int = 32): List<Vector3> = (0..n).map { (a * (1.0 - it.toDouble() / n) + b * (it.toDouble() / n)).normalized }

        val drag = extend(DragParameters(doubleArrayOf(0.3, 0.2, 0.1), Vector2.ZERO) { emptyList() })
        drag.damping = 100.0

        fun rotation(x: DoubleArray) = Vector3(x[0], x[1], x[2])

        // a point of the sphere is grabbed in either view: where it is before the turn, which is kept
        drag.grab = { position ->
            val v = rotation(drag.parameters)
            val onSphere = fromSphereView(position)
            if (onSphere != null) {
                val point = turn(onSphere, -v)
                val place: (DoubleArray) -> Vector2 = { x -> sphereView(turn(point, rotation(x))) }
                place
            } else if (position.x > 540.0) {
                val point = turn(fromPlaneView(position), -v)
                val place: (DoubleArray) -> Vector2 = { x -> planeView(turn(point, rotation(x))) }
                place
            } else null
        }

        extend {
            val v = rotation(drag.parameters)
            val turned = corners.map { turn(it, v) }
            val arcs = edges.map { (i, j) -> arc(turned[i], turned[j]) }

            // the sphere: its outline, its equator and the north pole that projects to infinity
            drawer.fill = ColorRGBa.fromHex("#14283c")
            drawer.stroke = ColorRGBa.WHITE.opacify(0.5)
            drawer.circle(sphereCenter, sphereRadius)
            val equator = (0..96).map { Vector3(cos(it * Math.PI / 48), sin(it * Math.PI / 48), 0.0) }
            drawer.stroke = ColorRGBa.fromHex("#5ab0ff").opacify(0.6)
            drawer.lineStrip(equator.map(::sphereView))
            drawer.fill = ColorRGBa.fromHex("#5ab0ff")
            drawer.stroke = null
            drawer.circle(sphereView(Vector3(0.0, 0.0, 1.0)), 5.0)
            // the edges, those on the back faint
            for (arc in arcs) {
                for ((a, b) in arc.zipWithNext()) {
                    drawer.stroke = if (front(a)) ColorRGBa.WHITE else ColorRGBa.WHITE.opacify(0.2)
                    drawer.lineSegment(sphereView(a), sphereView(b))
                }
            }
            drawer.stroke = null
            for (c in turned) {
                drawer.fill = if (front(c)) ColorRGBa.PINK else ColorRGBa.PINK.opacify(0.3)
                drawer.circle(sphereView(c), 5.0)
            }

            // the plane, in its own half: the equator, where the sphere meets it, and the projection
            drawer.drawStyle.clip = Rectangle(540.0, 0.0, 540.0, 540.0)
            drawer.fill = null
            drawer.stroke = ColorRGBa.fromHex("#5ab0ff").opacify(0.6)
            drawer.circle(planeCenter, planeScale)
            drawer.stroke = ColorRGBa.WHITE
            for (arc in arcs) {
                // near the north pole a point projects far away, those parts are left out
                for ((a, b) in arc.zipWithNext()) {
                    val pa = planeView(a)
                    val pb = planeView(b)
                    if (pa.distanceTo(planeCenter) < 2000.0 && pb.distanceTo(planeCenter) < 2000.0) drawer.lineSegment(pa, pb)
                }
            }
            drawer.stroke = null
            drawer.fill = ColorRGBa.PINK
            drawer.circles(turned.map(::planeView).filter { it.distanceTo(planeCenter) < 2000.0 }, 5.0)
            drawer.drawStyle.clip = null
            drawer.stroke = ColorRGBa.WHITE.opacify(0.3)
            drawer.lineSegment(540.0, 0.0, 540.0, 540.0)
        }
    }
}
