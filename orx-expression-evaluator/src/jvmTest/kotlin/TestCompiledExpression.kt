import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.openrndr.extra.expressions.ExpressionException
import org.openrndr.extra.expressions.compileExpression
import kotlin.test.assertEquals

class TestCompiledExpression {
    @Test
    fun `a simple compiled expression`() {
        val expression = "someValue"
        val function = compileExpression(expression, constants = mutableMapOf("someValue" to 5.0))
        assertEquals(5.0, function())
    }

    @Test
    fun `a compiled expression with updated context`() {
        val expression = "someValue"
        val context = mutableMapOf("someValue" to 5.0)
        val function = compileExpression(expression, constants = context)
        assertEquals(5.0, function())
        context["someValue"] = 6.0
        assertEquals(6.0, function())
    }

    @Test
    fun `an erroneous compiled expression`() {
        val expression = "1bork"
        assertThrows<ExpressionException> {
            compileExpression(expression, constants = mutableMapOf("someValue" to 5.0))
        }
    }
}