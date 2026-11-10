package loft

import org.openrndr.extra.shapes.loft.Loft
import org.openrndr.extra.shapes.loft.LoftVertex
import org.openrndr.extra.shapes.loft.loft
import org.openrndr.extra.shapes.pose.PosePath3D
import org.openrndr.extra.shapes.rectify.rectified
import org.openrndr.extra.shapes.splines.catmullRom
import org.openrndr.extra.shapes.splines.toPath3D
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.shape.Circle
import org.openrndr.shape.Rectangle
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.round
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestLoftPrecision {
    private val helix = PosePath3D(
        List(33) { i -> Vector3(cos(i * 0.4) * 6.0, i * 0.25, sin(i * 0.4) * 6.0) }
            .catmullRom(0.5, false).toPath3D().rectified()
    )
    private val trefoil = PosePath3D(
        List(64) { i ->
            val a = i * 2 * PI / 64
            Vector3(sin(a) + 2 * sin(2 * a), cos(a) - 2 * cos(2 * a), -sin(3 * a)) * 3.0
        }.catmullRom(0.5, true).toPath3D().rectified()
    )
    private val circle = Circle(0.0, 0.0, 0.5).contour.rectified()
    private val square = Rectangle.fromCenter(Vector2.ZERO, 1.0, 1.0).contour.rectified()

    private fun degrees(a: Vector3, b: Vector3) = acos((a.normalized dot b.normalized).coerceIn(-1.0, 1.0)) * 180 / PI

    /** the normal of [loft] by fine differences of its positions, one-sided at open edges */
    private fun referenceNormal(loft: Loft, u: Double, v: Double): Vector3 {
        val h = 1E-6
        val u0 = if (loft.closedCrossSection) u - h else (u - h).coerceAtLeast(0.0)
        val u1 = if (loft.closedCrossSection) u + h else (u + h).coerceAtMost(1.0)
        val v0 = if (loft.closedPath) v - h else (v - h).coerceAtLeast(0.0)
        val v1 = if (loft.closedPath) v + h else (v + h).coerceAtMost(1.0)
        val du = (loft.position(u1, v) - loft.position(u0, v)) / (u1 - u0)
        val dv = (loft.position(u, v1) - loft.position(u, v0)) / (v1 - v0)
        return du cross dv
    }

    @Test
    fun normalsOfTheSideAreThoseOfTheSurface() {
        for (poses in listOf(helix, trefoil)) {
            val loft = circle.loft(poses)
            val along = 100
            val around = 16
            val side = loft.side(along, around)
            for (vertex in side) {
                val (u, v) = vertex.texCoord
                val error = degrees(vertex.normal, referenceNormal(loft, u, v))
                assertTrue(error < 2E-3, "normal off by $error degrees at $u, $v")
            }
        }
    }

    @Test
    fun theSideEvaluatesThePoseOncePerStep() {
        var poseCalls = 0
        var crossSectionCalls = 0
        val along = 50
        val around = 12
        val loft = Loft(
            crossSection = { u, _ -> crossSectionCalls++; circle.position(u) },
            dcrossSectiondu = { u, _ -> circle.direction(u) * circle.contour.length },
            pose = { v -> poseCalls++; helix.pose(v) },
            dposedv = { v -> helix.poseDerivative(v) },
            constantCrossSection = true
        )
        loft.side(along, around)
        assertEquals(along + 1, poseCalls)
        // the columns once, and the rim once to find which way the cross-section goes
        assertEquals(around + 1 + around, crossSectionCalls)
    }

    @Test
    fun cornersOfTheCrossSectionAreFound() {
        assertEquals(listOf(0.0, 0.25, 0.5, 0.75), square.loft(helix).corners.map { round(it * 1E9) / 1E9 })
        assertEquals(emptyList(), circle.loft(helix).corners)
    }

    @Test
    fun sharpCornersHaveTheNormalsOfTheirFaces() {
        val loft = square.loft(helix)
        // around is not a multiple of 4: without the corners in the grid they would be cut off
        val along = 400
        val around = 30
        fun worst(side: List<LoftVertex>): Double = (0 until side.size / 3).maxOf { t ->
            val (a, b, c) = (0..2).map { side[3 * t + it] }
            val face = (b.position - a.position) cross (c.position - a.position)
            listOf(a, b, c).maxOf { degrees(it.normal, face) }
        }
        // the path bends by about 1.8 degrees from one step to the next, the corners turn by 90
        val sharp = worst(loft.side(along, around, sharpCorners = true))
        assertTrue(sharp < 2.5, "sharp corners: normals up to $sharp degrees off their faces")
        val round = worst(loft.side(along, around))
        assertTrue(round > 30.0, "round corners: normals only up to $round degrees off their faces")
    }

    @Test
    fun sharpCornersAreInTheGrid() {
        val loft = square.loft(helix)
        val side = loft.side(40, 30, sharpCorners = true)
        val start = side.filter { it.texCoord.y == 0.0 }.map { it.position }
        val pose = helix.pose(0.0)
        for (corner in listOf(Vector2(-0.5, -0.5), Vector2(0.5, -0.5), Vector2(0.5, 0.5), Vector2(-0.5, 0.5))) {
            val expected = pose[3].xyz + pose[0].xyz * corner.x + pose[1].xyz * corner.y
            assertTrue(start.minOf { it.distanceTo(expected) } < 1E-9, "corner $corner is cut off")
        }
    }

    @Test
    fun sharpCornersMakeAClosedSolidWithTheCaps() {
        val loft = square.loft(helix)
        val triangles = loft.side(40, 30, sharpCorners = true) +
                loft.cap(end = false, around = 30, sharpCorners = true) +
                loft.cap(end = true, around = 30, sharpCorners = true)
        fun key(p: Vector3) = listOf(p.x, p.y, p.z).joinToString(" ") { round(it * 1E6).toLong().toString() }
        val count = mutableMapOf<Pair<String, String>, Int>()
        for (t in 0 until triangles.size / 3) {
            val c = (0..2).map { key(triangles[3 * t + it].position) }
            for (i in 0..2) {
                val a = c[i]
                val b = c[(i + 1) % 3]
                if (a == b) continue
                val e = if (a < b) a to b else b to a
                count[e] = (count[e] ?: 0) + 1
            }
        }
        val open = count.filterValues { it != 2 }
        assertTrue(open.isEmpty(), "${open.size} edges not shared by two triangles")
        assertTrue(triangles.none { abs(it.normal.length - 1.0) > 1E-9 })
    }
}
