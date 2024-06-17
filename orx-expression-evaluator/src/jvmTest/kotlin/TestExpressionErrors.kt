import org.amshove.kluent.`should throw`
import org.amshove.kluent.`with message`
import org.amshove.kluent.invoking
import org.openrndr.extra.expressions.ExpressionException
import org.openrndr.extra.expressions.evaluateExpression

import kotlin.test.Test

class TestExpressionErrors {

    @Test
    fun `an expression with non-sensible writing`() {
        val expression = ")("
        invoking {
            evaluateExpression(expression)
        } `should throw` ExpressionException::class

    }


    @Test
    fun `an expression trying to reassign a number`() {
        val expression = "3 = 5"
        invoking {
            evaluateExpression(expression)
        } `should throw` ExpressionException::class
    }

    @Test
    fun `an expression that uses non-existing functions`() {
        val expression = "notExisting(5)"
        invoking {
            evaluateExpression(expression)
        } `should throw` ExpressionException::class `with message` "error in evaluation of 'notExisting(5)': unresolved function: 'notExisting(x0)'"

    }

    @Test
    fun `an expression that uses non-existing variables`() {
        val expression = "notExisting + 4"
        invoking {
            evaluateExpression(expression)
        } `should throw` ExpressionException::class `with message` "error in evaluation of 'notExisting+4': unresolved variable: 'notExisting'"
    }
}
