import org.amshove.kluent.shouldBeEqualTo
import org.openrndr.extra.expressions.watchingExpression1
import kotlin.test.Test

class TestExpressionDelegates {

    @Test
    fun test() {
        val state = object {
            var expression = "x * x"
            val function1 by watchingExpression1(::expression, "x")
        }
        state.function1(5.0).shouldBeEqualTo(25.0)
    }

}