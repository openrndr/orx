import org.openrndr.extra.shapes.clothoid.clothoidFits
import org.openrndr.extra.shapes.elastica.elastica
import org.openrndr.extra.shapes.mvc.Fairness
import org.openrndr.extra.shapes.mvc.QuinticSegment
import org.openrndr.extra.shapes.mvc.fairSegments
import org.openrndr.math.Vector2
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestElastica {

    private fun List<Vector2>.elasticaSegments(closed: Boolean) = fairSegments(closed, Fairness.ENERGY)

    private fun assertG2(segments: List<QuinticSegment>, closed: Boolean, message: String) {
        val joints = if (closed) segments.size else segments.size - 1
        for (i in 0 until joints) {
            val a = segments[i]
            val b = segments[(i + 1) % segments.size]
            assertTrue(a.position(1.0).distanceTo(b.position(0.0)) < 1E-9, "$message: joint $i doesn't connect")
            assertTrue(a.derivative(1.0).normalized.distanceTo(b.derivative(0.0).normalized) < 1E-9, "$message: tangent at $i")
            val ka = a.curvature(1.0)
            assertEquals(ka, b.curvature(0.0), 1E-9 * (1.0 + abs(ka)), "$message: curvature jumps at $i")
        }
    }

    private fun assertInterpolates(segments: List<QuinticSegment>, points: List<Vector2>, closed: Boolean) {
        val joints = segments.map { it.position(0.0) } + if (closed) emptyList() else listOf(segments.last().position(1.0))
        var from = 0
        for (p in points) {
            val index = (from until joints.size).firstOrNull { joints[it].distanceTo(p) < 1E-9 }
            assertTrue(index != null, "$p is not a joint, in order")
            from = index + 1
        }
    }

    @Test
    fun pointsOnACircle() {
        val random = Random(1)
        val center = Vector2(200.0, 150.0)
        val angles = List(7) { random.nextDouble(0.0, 2 * PI) }.sorted()
        val points = angles.map { center + Vector2(cos(it), sin(it)) * 100.0 }
        val segments = points.elasticaSegments(closed = true)
        assertInterpolates(segments, points, true)
        assertG2(segments, true, "circle")
        var worst = 0.0
        for (segment in segments) for (k in 0..20) {
            worst = maxOf(worst, abs(segment.position(k / 20.0).distanceTo(center) - 100.0))
        }
        // 1E-4 at the time of writing; a quintic can't be exactly circular
        assertTrue(worst < 0.01, "deviates from the circle by $worst")
    }

    @Test
    fun collinear() {
        val points = List(5) { Vector2(it * 50.0, it * 20.0) }
        for (segment in points.elasticaSegments(closed = false)) for (p in segment.controlPoints) {
            assertEquals(0.0, p.x * 20.0 - p.y * 50.0, 1E-6)
        }
    }

    @Test
    fun freeEnds() {
        // the ends of an open elastica carry no bending moment, so their curvature vanishes
        val wave = List(9) { i -> Vector2(-180.0 + i * 45.0, 70.0 * sin(i * 0.9)) }
        val segments = wave.elasticaSegments(closed = false)
        assertInterpolates(segments, wave, false)
        assertG2(segments, false, "wave")
        val typical = segments.maxOf { s -> (0..10).maxOf { abs(s.curvature(it / 10.0)) } }
        val ends = maxOf(abs(segments.first().curvature(0.0)), abs(segments.last().curvature(1.0)))
        assertTrue(ends < 0.02 * typical, "end curvature $ends is not small compared to $typical")
    }

    /** The bending energy ∫κ² ds of a clothoid, along which κ = κ0 + κ'·s. */
    private fun clothoidEnergy(k0: Double, dk: Double, l: Double) = k0 * k0 * l + k0 * dk * l * l + dk * dk * l * l * l / 3.0

    @Test
    fun comparedToOtherFairCurves() {
        val blob = List(11) { i ->
            val a = i * 2 * PI / 11
            Vector2(cos(a), sin(a)) * (110.0 + 35.0 * sin(3 * a + 0.4))
        }
        val wave = List(9) { i -> Vector2(-180.0 + i * 45.0, 70.0 * sin(i * 0.9)) }
        for ((points, closed) in listOf(blob to true, wave to false)) {
            val elastica = points.elasticaSegments(closed)
            val mvc = points.fairSegments(closed, Fairness.VARIATION)
            val clothoids = points.clothoidFits(closed)
            fun energyTimesLength(segments: List<QuinticSegment>) =
                segments.sumOf { it.energy() } * segments.sumOf { it.length() }
            fun variationTimesLengthCubed(segments: List<QuinticSegment>) =
                segments.sumOf { it.variation() } * segments.sumOf { it.length() }.pow(3)
            val clothoidLength = clothoids.sumOf { it.length }
            val clothoidEnergy = clothoids.sumOf { clothoidEnergy(it.kappa, it.dkappa, it.length) } * clothoidLength
            // each curve wins at its own functional
            assertTrue(energyTimesLength(elastica) < energyTimesLength(mvc))
            assertTrue(energyTimesLength(elastica) < clothoidEnergy)
            assertTrue(variationTimesLengthCubed(mvc) < variationTimesLengthCubed(elastica))
        }
    }

    @Test
    fun randomPoints() {
        val random = Random(2)
        repeat(20) {
            val points = List(3 + it % 6) { Vector2(random.nextDouble(0.0, 400.0), random.nextDouble(0.0, 400.0)) }
            val closed = it % 2 == 1
            val segments = points.elasticaSegments(closed)
            assertInterpolates(segments, points, closed)
            assertG2(segments, closed, "random $it")
            assertEquals(closed, points.elastica(closed).closed)
        }
    }
}
