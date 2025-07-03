import org.openrndr.extra.expressions.evaluateExpression
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class TestOperators {
    @Test
    fun `an addition operation`() {
        val result = evaluateExpression("1 + 2")
        assertNotNull(result)
        assertEquals(3.0, result, 10E-6)
    }

    @Test
    fun `a subtraction operation`() {
        val result = evaluateExpression("1 - 2")
        assertNotNull(result)
        assertEquals(-1.0, result, 10E-6)
    }

    @Test
    fun `a modulus operation`() {
        val result = evaluateExpression("4 % 2")
        assertNotNull(result)
        assertEquals(0.0, result, 10E-6)
    }

    @Test
    fun `a multiplication operation`() {
        val result = evaluateExpression("4 * 2")
        assertNotNull(result)
        assertEquals(8.0, result, 10E-6)
    }

    @Test
    fun `a division operation`() {
        val result = evaluateExpression("4 / 2")
        assertNotNull(result)
        assertEquals(2.0, result, 10E-6)
    }

    @Test
    fun `a multiplication and addition operation`() {
        val result = evaluateExpression("4 * 2 + 1")
        assertNotNull(result)
        assertEquals(9.0, result, 10E-6)
    }

    @Test
    fun `an addition and multiplication`() {
        val result = evaluateExpression("4 + 2 * 3")
        assertNotNull(result)
        assertEquals(10.0, result, 10E-6)
    }

    @Test
    fun `unary minus`() {
        val result = evaluateExpression("-4.0")
        assertNotNull(result)
        assertEquals(-4.0, result, 10E-6)
    }
}
