import org.junit.jupiter.api.assertThrows
import org.openrndr.extra.expressions.ExpressionException
import org.openrndr.extra.expressions.evaluateExpression

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TestExpressionErrors {

    @Test
    fun `an expression with non-sensible writing`() {
        val expression = ")("
        assertThrows<ExpressionException> {
            evaluateExpression(expression)
        }
    }


    @Test
    fun `an expression trying to reassign a number`() {
        val expression = "3 = 5"
        assertThrows<ExpressionException> {
            evaluateExpression(expression)
        }
    }

    @Test
    fun `an expression that uses non-existing functions`() {
        val expression = "notExisting(5)"
        val exception = assertFailsWith<ExpressionException> {
            evaluateExpression(expression)
        }
        assertEquals(
            "error in evaluation of 'notExisting(5)': unresolved function: 'notExisting(x0)'",
            exception.message
        )
    }

    @Test
    fun `an expression that uses non-existing variables`() {
        val expression = "notExisting + 4"
        val exception = assertFailsWith<ExpressionException> {
            evaluateExpression(expression)
        }
        assertEquals(
            "error in evaluation of 'notExisting+4': unresolved value: 'notExisting'. available values: {}",
            exception.message
        )
    }
}
