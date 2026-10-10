import org.openrndr.extra.shapes.distort.FoldMode
import org.openrndr.extra.shapes.distort.distortUniform
import org.openrndr.extra.shapes.primitives.regularStar
import org.openrndr.extra.shapes.rectify.RectifiedContour
import org.openrndr.extra.shapes.rectify.rectified
import org.openrndr.extra.shapes.smoothnormal.smoothNormalAngular
import org.openrndr.extra.shapes.smoothnormal.smoothNormalLinear
import org.openrndr.math.Vector2
import org.openrndr.shape.Circle
import org.openrndr.shape.ShapeContour
import org.openrndr.shape.isSelfIntersecting
import org.openrndr.shape.removeSelfIntersections
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestDistortUniformFold {
    private val star = regularStar(5, 70.0, 170.0, Vector2(270.0, 240.0), phase = -90.0).rectified()

    private fun RectifiedContour.offset(normal: RectifiedContour.(Double, Double) -> Vector2, radius: Double, d: Double, foldMode: FoldMode) =
        distortUniform(sampleDistance = 1.0, errorTolerance = 0.1, foldMode = foldMode) { t, p -> p + normal(t, radius) * d }

    private fun assertContinuous(c: ShapeContour) {
        for ((s0, s1) in c.segments.zipWithNext()) {
            assertTrue(s0.end.distanceTo(s1.start) < 1E-9, "gap between segments: ${s0.end} ${s1.start}")
        }
        if (c.closed) {
            assertTrue(c.segments.last().end.distanceTo(c.segments.first().start) < 1E-9, "contour does not close")
        }
    }

    @Test
    fun defaultIsFold() {
        val a = star.distortUniform(sampleDistance = 1.0) { t, p -> p + star.smoothNormalLinear(t, 10.0) * 30.0 }
        val b = star.distortUniform(sampleDistance = 1.0, foldMode = FoldMode.FOLD) { t, p -> p + star.smoothNormalLinear(t, 10.0) * 30.0 }
        assertEquals(a.segments, b.segments)
    }

    @Test
    fun trimWithoutFoldsEqualsFold() {
        val circle = Circle(200.0, 200.0, 100.0).contour.rectified()
        for (d in listOf(-50.0, 30.0)) {
            val fold = circle.distortUniform(foldMode = FoldMode.FOLD) { t, p -> p + circle.normal(t) * d }
            val trim = circle.distortUniform(foldMode = FoldMode.TRIM) { t, p -> p + circle.normal(t) * d }
            assertEquals(fold.segments, trim.segments)
        }
    }

    @Test
    fun trimRemovesFolds() {
        val cases = listOf<Triple<RectifiedContour.(Double, Double) -> Vector2, Double, Double>>(
            Triple({ t, r -> smoothNormalLinear(t, r) }, 6.5, -30.0),
            Triple({ t, r -> smoothNormalLinear(t, r) }, 7.5, 30.0),
            Triple({ t, r -> smoothNormalAngular(t, r) }, 9.5, 30.0),
            Triple({ t, r -> smoothNormalAngular(t, r) }, 9.0, -20.0),
        )
        for ((normal, radius, d) in cases) {
            assertTrimmed(normal, radius, d)
        }
    }

    /**
     * Inward offsets with a large blend radius: the fold at each tip is detected as several backwards intervals and
     * its sides only cross far outside of them. Untrimmed these leave a lobe at every tip.
     */
    @Test
    fun trimRemovesWideFoldsAtTips() {
        val cases = listOf<Triple<RectifiedContour.(Double, Double) -> Vector2, Double, Double>>(
            Triple({ t, r -> smoothNormalLinear(t, r) }, 39.5, -25.0),
            Triple({ t, r -> smoothNormalLinear(t, r) }, 47.5, -30.0),
            Triple({ t, r -> smoothNormalAngular(t, r) }, 26.5, -20.0),
            Triple({ t, r -> smoothNormalAngular(t, r) }, 33.0, -25.0),
        )
        for ((normal, radius, d) in cases) {
            assertTrimmed(normal, radius, d)
            val trim = star.offset(normal, radius, d, FoldMode.TRIM)
            assertEquals(1, trim.removeSelfIntersections().contours.size, "expected a single contour (radius=$radius d=$d)")
        }
    }

    /**
     * Short fitted pieces must not overshoot: with too few samples the fit interpolates them exactly and a control
     * point can land beyond the piece's end, leaving a tiny loop at the junction (found in DemoSmoothNormal02).
     */
    @Test
    fun shortPiecesDoNotOvershoot() {
        val trim = star.offset({ t, r -> smoothNormalLinear(t, r) }, 5.401, -25.0, FoldMode.TRIM)
        assertTrue(!trim.isSelfIntersecting, "trimmed offset self-intersects")
        assertEquals(1, trim.removeSelfIntersections().contours.size)
    }

    private fun assertTrimmed(normal: RectifiedContour.(Double, Double) -> Vector2, radius: Double, d: Double) {
        val fold = star.offset(normal, radius, d, FoldMode.FOLD)
        val trim = star.offset(normal, radius, d, FoldMode.TRIM)
        assertTrue(fold.isSelfIntersecting, "expected the folded offset to self-intersect (radius=$radius d=$d)")
        assertTrue(trim.closed)
        assertContinuous(trim)
        assertTrue(!trim.isSelfIntersecting, "trimmed offset self-intersects (radius=$radius d=$d)")

        val reference = fold.removeSelfIntersections().contours.maxOf { abs(it.shape.area) }
        val area = abs(trim.shape.area)
        assertTrue(abs(area - reference) / reference < 0.005, "area $area differs from $reference (radius=$radius d=$d)")
    }
}
