import org.openrndr.extra.expressions.evaluateExpression
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class TestOperators {
    @Test
    fun `an addition operation`() {
        val result = evaluateExpression("1 + 2")
        assertNotNull(result)
        assertEquals(result, 3.0, 10E-6)
    }

    @Test
    fun `a subtraction operation`() {
        val result = evaluateExpression("1 - 2")
        assertNotNull(result)
        assertEquals(result, -1.0, 10E-6)
    }

    @Test
    fun `a modulus operation`() {
        val result = evaluateExpression("4 % 2")
        assertNotNull(result)
        assertEquals(result, 0.0, 10E-6)
    }

    @Test
    fun `a multiplication operation`() {
        val result = evaluateExpression("4 * 2")
        assertNotNull(result)
        assertEquals(result, 8.0, 10E-6)
    }

    @Test
    fun `a division operation`() {
        val result = evaluateExpression("4 / 2")
        assertNotNull(result)
        assertEquals(result, 2.0, 10E-6)
    }

    @Test
    fun `a multiplication and addition operation`() {
        val result = evaluateExpression("4 * 2 + 1")
        assertNotNull(result)
        assertEquals(result, 9.0, 10E-6)
    }

    @Test
    fun `an addition and multiplication`() {
        val result = evaluateExpression("4 + 2 * 3")
        assertNotNull(result)
        assertEquals(result, 10.0, 10E-6)
    }

    @Test
    fun `unary minus`() {
        val result = evaluateExpression("-4.0")
        assertNotNull(result)
        assertEquals(result, -4.0, 10E-6)
    }
}
