package loft

import org.openrndr.extra.shapes.loft.Loft
import org.openrndr.math.Matrix44
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.math.transforms.buildTransform
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertTrue

class TestSelfCrossingCaps {
    /** a figure 8, sampled so that no sample falls on its crossing */
    private fun figure8(u: Double): Vector2 {
        val a = 2 * PI * (u + 1.0 / 128)
        return Vector2(sin(a), sin(a) * cos(a))
    }

    private val loft = Loft(
        crossSection = { u, _ -> figure8(u) },
        pose = { v -> buildTransform { translate(0.0, 0.0, v * 5.0) } },
        dposedv = { _ -> Matrix44.ZERO },
        closedCrossSection = true
    )

    @Test
    fun aCrossSectionThatCrossesItselfIsCapped() {
        for (end in listOf(false, true)) {
            val cap = loft.cap(end, 64)
            assertTrue(cap.isNotEmpty())
            assertTrue(cap.all { it.position.x.isFinite() && it.position.y.isFinite() })
            // both lobes are filled, the crossing is a corner of the cap, and the area is that of the two lobes
            fun covers(q: Vector2) = (0 until cap.size / 3).any { t ->
                val p = (0..2).map { cap[3 * t + it].position.let { v -> Vector2(v.x, v.y) } }
                fun s(a: Vector2, b: Vector2) = (b.x - a.x) * (q.y - a.y) - (q.x - a.x) * (b.y - a.y)
                val d = listOf(s(p[0], p[1]), s(p[1], p[2]), s(p[2], p[0]))
                d.all { it > 0 } || d.all { it < 0 }
            }
            assertTrue(covers(Vector2(0.6, 0.1)) && covers(Vector2(-0.6, -0.1)), "a lobe is not filled")
            assertTrue(cap.any { abs(it.position.x) < 1E-9 && abs(it.position.y) < 1E-9 }, "no corner at the crossing")
            var area = 0.0
            for (t in 0 until cap.size / 3) {
                val (a, b, c) = (0..2).map { cap[3 * t + it].position }
                area += ((b - a).cross(c - a)).length / 2
            }
            // two lobes of the figure 8 x = sin a, y = sin a cos a, each of area 2/3, less the chords of 64 samples
            assertTrue(abs(area - 4.0 / 3.0) < 0.02, "area $area")
            // facing back at the start and along the path at the end
            val facing = if (end) Vector3.UNIT_Z else -Vector3.UNIT_Z
            assertTrue(cap.all { it.normal.distanceTo(facing) < 1E-9 })
        }
    }
}
