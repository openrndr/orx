import org.amshove.kluent.invoking
import org.amshove.kluent.`should throw`
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import org.openrndr.extra.expressions.ExpressionException
import org.openrndr.extra.expressions.compileExpression

class TestCompiledExpression {
    @Test
    fun `a simple compiled expression`() {
        val expression = "someValue"
        val function = compileExpression(expression, constants = mutableMapOf("someValue" to 5.0))
        function().shouldBeEqualTo(5.0)
    }

    @Test
    fun `a compiled expression with updated context`() {
        val expression = "someValue"
        val context =  mutableMapOf("someValue" to 5.0)
        val function = compileExpression(expression, constants = context)
        function().shouldBeEqualTo(5.0)
        context["someValue"] = 6.0
        function().shouldBeEqualTo(6.0)
    }

    @Test
    fun `an erroneous compiled expression`() {
        val expression = "1bork"
        invoking {
            compileExpression(expression, constants = mutableMapOf("someValue" to 5.0))
        } `should throw` ExpressionException::class
    }
}