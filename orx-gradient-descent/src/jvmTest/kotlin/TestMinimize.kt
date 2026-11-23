import org.openrndr.extra.gradientdescent.minimize
import kotlin.test.Test
import kotlin.test.assertEquals

class TestMinimize {
    @Test
    fun `a 1d parabola is minimized`() {
        val result = minimize(doubleArrayOf(10.0)) { x -> (x[0] + 1) * (x[0] + 1) }
        assertEquals(-1.0, result.solution[0], 1E-3)
        assertEquals(0.0, result.value, 1E-6)
    }
}
