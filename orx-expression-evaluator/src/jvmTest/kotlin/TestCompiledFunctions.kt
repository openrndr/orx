import org.junit.jupiter.api.Test
import org.openrndr.extra.expressions.compileFunction1
import org.openrndr.extra.expressions.compileFunction2
import org.openrndr.extra.expressions.compileFunction3
import kotlin.test.assertEquals

class TestCompiledFunctions {
    @Test
    fun `a simple compiled function1`() {
        val expression = "t"
        val function = compileFunction1(expression, "t")
        assertEquals(-5.0, function(-5.0))
        assertEquals(5.0, function(5.0))
    }

    @Test
    fun `a simple compiled function2`() {
        val expression = "x + y"
        val function = compileFunction2(expression, "x", "y")
        assertEquals(3.0, function(1.0, 2.0))
    }

    @Test
    fun `a simple compiled function3`() {
        val expression = "x + y + z"
        val function = compileFunction3(expression, "x", "y", "z")
        assertEquals(6.0, function(1.0, 2.0, 3.0))
    }
}