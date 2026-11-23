import org.openrndr.extra.gradientdescent.dot
import kotlin.test.Test
import kotlin.test.assertEquals

class TestDot {
    @Test
    fun `the dot product of two vectors`() {
        assertEquals(40.0, dot(doubleArrayOf(10.0), doubleArrayOf(4.0)))
    }

    @Test
    fun `a matrix times a vector`() {
        assertEquals(10.0, dot(arrayOf(doubleArrayOf(10.0)), doubleArrayOf(1.0))[0])
        assertEquals(19.99999999995339, dot(arrayOf(doubleArrayOf(1.0)), doubleArrayOf(19.99999999995339))[0])
    }
}
