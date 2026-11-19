package typed

import org.openrndr.extra.expressions.ExpressionProblem.Kind
import org.openrndr.extra.expressions.typed.*
import org.openrndr.math.Vector2
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TestTypedPrograms {
    private fun eval(expression: String, values: Map<String, Any> = emptyMap()) = evaluateTypedExpression(expression, { values[it] })

    @Test
    fun `a program declares values for the statements after it`() {
        assertEquals(8.0, eval("val a = 5; val b = 3; a + b"))
        assertEquals(8.0, eval("val a = 5\nval b = 3\na + b"))
        assertEquals(Vector2(2.0, 4.0), eval("val v = vec2(1.0, 2.0); val s = 2; v * s"))
        assertEquals(listOf(3.0, 6.0), eval("val xs = [1, 2]; val k = 3; xs.map { it * k }"))
        // a value comes before the constant of the same name, which its own expression reads
        assertEquals(20.0, eval("val x = x * 2; x", mapOf("x" to 10.0)))
        // the parameters of a lambda come before a value of the same name
        assertEquals(listOf(1.0, 2.0), eval("val it = 10; [1, 2].map { it }"))
        // a lambda that reads a value, called after the program
        @Suppress("UNCHECKED_CAST")
        val f = eval("val k = 3; { x -> x * k }") as (Any) -> Any
        assertEquals(6.0, f(2.0))
        assertNull(eval("val a = 5"))
    }

    @Test
    fun `compiled programs with parameters`() {
        val f = compileFunction1<Double, Double>("val y = x * 2; y + 1", "x")
        assertEquals(5.0, f(2.0))
        assertEquals(7.0, f(3.0))
        val g = compileFunction2<Double, Double, Vector2>("val v = vec2(x, y); v * 2", "x", "y")
        assertEquals(Vector2(2.0, 4.0), g(1.0, 2.0))
    }

    @Test
    fun `a value is declared once, not as a parameter, before it is read`() {
        val twice = assertFailsWith<ExpressionException> { eval("val a = 1; val a = 2; a") }
        assertEquals(listOf(Kind.DUPLICATE_VALUE), twice.problems.map { it.kind })
        val parameter = assertFailsWith<ExpressionException> { compileFunction1<Double, Double>("val x = 2; x", "x")(1.0) }
        assertEquals(listOf(Kind.DUPLICATE_VALUE), parameter.problems.map { it.kind })
        val before = assertFailsWith<ExpressionException> { eval("val a = b; val b = 2; a") }
        assertEquals(listOf(Kind.UNKNOWN_VALUE), before.problems.map { it.kind })
    }

    @Test
    fun `the problems and names of a program`() {
        val program = "val a = x * 2; val b = a + y; val a = 3; [b].map { it + c }"
        assertEquals(
            listOf(Kind.DUPLICATE_VALUE to "a", Kind.UNKNOWN_VALUE to "c"),
            checkTypedExpression(program, setOf("x", "y")).map { it.kind to program.substring(it.start, it.end) }
        )
        assertTrue(checkTypedExpression("val a = x; a", setOf("x")).isEmpty())
        assertEquals(setOf("x", "y", "c"), typedExpressionNames(program))
    }
}
