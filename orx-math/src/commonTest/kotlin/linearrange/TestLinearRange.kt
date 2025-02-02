package linearrangeimport
import org.openrndr.math.Vector2
import org.openrndr.math.Vector4
import org.openrndr.extra.math.linearrange.rangeTo
import kotlin.test.Test
import kotlin.test.assertTrue

class TestLinearRange {
    val e = 1E-6

    /**
     * Verifies the interpolation behavior of a one-dimensional linear range.
     *
     * This test checks whether the interpolated value computed for a specific parameter (`t`)
     * within a linear range of two `Vector2` instances falls within a specified range of error tolerance.
     */
    @Test
    fun testLinearRange1D() {
        val v0 = Vector2.UNIT_X
        val v1 = Vector2.UNIT_Y
        val lr = v0..v1
        val c = lr.value(0.5)
        assertTrue(c.x in 0.5 - e..0.5 + e)
        assertTrue(c.y in 0.5 - e..0.5 + e)
    }


    /**
     * Verifies the interpolation behavior of a two-dimensional linear range.
     *
     * This test validates that the computed interpolated value within a 2D linear range of `Vector4` instances
     * falls within an acceptable range of error tolerance. The interpolation is performed using a `LinearRange2D`
     * created from two 1D linear ranges, which are themselves defined by start and end `Vector4` points.
     * The test checks the accuracy of the interpolation for the midpoints along both dimensions.
     */
    @Test
    fun testLinearRange2D() {
        val v00 = Vector4.UNIT_X
        val v01 = Vector4.UNIT_Y
        val lr0 = v00..v01

        val v10 = Vector4.UNIT_Z
        val v11 = Vector4.UNIT_W
        val lr1 = v10..v11

        val lr = lr0..lr1

        val c = lr.value(0.5, 0.5)
        assertTrue(c.x in 0.25 - e..0.25 + e)
        assertTrue(c.y in 0.25 - e..0.25 + e)
        assertTrue(c.z in 0.25 - e..0.25 + e)
        assertTrue(c.w in 0.25 - e..0.25 + e)
    }
}