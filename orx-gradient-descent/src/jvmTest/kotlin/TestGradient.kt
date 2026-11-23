import org.openrndr.extra.gradientdescent.gradient
import kotlin.test.Test
import kotlin.test.assertEquals

class TestGradient {
    @Test
    fun `the gradient of a 1d parabola`() {
        val parabola = { x: DoubleArray -> x[0] * x[0] }
        val g0 = gradient(doubleArrayOf(0.0), parabola)
        assertEquals(1, g0.size)
        assertEquals(0.0, g0[0])
        assertEquals(2.0, gradient(doubleArrayOf(1.0), parabola)[0], 1E-6)
        assertEquals(-2.0, gradient(doubleArrayOf(-1.0), parabola)[0], 1E-6)
    }

    @Test
    fun `the gradient of a 2d parabola`() {
        val parabola = { x: DoubleArray -> x[0] * x[0] + x[1] * x[1] }
        val g0 = gradient(doubleArrayOf(0.0, 0.0), parabola)
        assertEquals(2, g0.size)
        assertEquals(0.0, g0[0])
        assertEquals(0.0, g0[1])
        for ((x, expected) in listOf(1.0 to 2.0, -1.0 to -2.0)) {
            val g = gradient(doubleArrayOf(x, x), parabola)
            assertEquals(expected, g[0], 1E-6)
            assertEquals(expected, g[1], 1E-6)
        }
    }
}
