import org.openrndr.extra.expressions.watchingExpression1
import kotlin.test.Test
import kotlin.test.assertEquals

class TestExpressionDelegates {

    @Test
    fun test() {
        val state = object {
            var expression = "x * x"
            val function1 by watchingExpression1(::expression, "x")
        }
        assertEquals(25.0, state.function1(5.0))
    }
}