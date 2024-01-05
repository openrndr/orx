import org.amshove.kluent.invoking
import org.amshove.kluent.`should throw`
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import org.openrndr.extra.expressions.*

class TestCompiledFunctions {
    @Test
    fun `a simple compiled function1`() {
        val expression = "t"
        val function = compileFunction1(expression, "t")
        function(-5.0).shouldBeEqualTo(-5.0)
        function(5.0).shouldBeEqualTo(5.0)
    }

    @Test
    fun `a simple compiled function2`() {
        val expression = "x + y"
        val function = compileFunction2(expression, "x", "y")
        function(1.0, 2.0).shouldBeEqualTo(3.0)
    }

    @Test
    fun `a simple compiled function3`() {
        val expression = "x + y + z"
        val function = compileFunction3(expression, "x", "y", "z")
        function(1.0, 2.0, 3.0).shouldBeEqualTo(6.0)
    }
}