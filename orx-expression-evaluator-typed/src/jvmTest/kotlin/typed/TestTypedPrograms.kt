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

    @Test
    fun `a program declares functions for the statements after it`() {
        assertEquals(175.0, eval("fun f(x) { x * 5.0 }; fun g(x) = x * 6.0; val a = f(5); val b = g(a); a + b"))
        // a body of statements, over lines, that reads the values and functions declared before the function
        assertEquals(23.0, eval("val k = 2\nfun h(x) = x + k\nfun f(x, y) {\n  val s = x * y\n  h(s) + 1\n}\nf(4, 5)"))
        // of every type, with lambdas in their bodies, and called in lambdas
        assertEquals(Vector2(2.0, 4.0), eval("fun double(v) = v * 2; double(vec2(1.0, 2.0))"))
        assertEquals(listOf(2.0, 4.0), eval("fun scaled(xs, k) = xs.map { it * k }; scaled([1, 2], 2)"))
        assertEquals(listOf(11.0, 12.0), eval("val k = 10; fun f(x) = x + k; [1, 2].map { f(it) }"))
        assertEquals(listOf(listOf(3.0, 4.0)), eval("fun f(x) { val ys = [1, 2]; ys.map { y -> y + x } }; [2].map { f(it) }"))
        // overloads by number of parameters, functions in functions, and calls in the arguments of a call
        assertEquals(6.0, eval("fun f(x) = x; fun f(x, y) = x + y; f(1) + f(2, 3)"))
        assertEquals(12.0, eval("fun f(x) { fun g(y) = x * y; g(3) }; f(4)"))
        assertEquals(9.0, eval("fun f(x, y) = x + y * 2; f(1, f(0, 2))"))
        // a declared function comes before the built-in function and the constants of the same name
        assertEquals(4.0, eval("fun sin(x) = x * 2; sin(2)"))
        assertEquals(12.0, eval("fun f(x) = x * 2; f(3) + x", mapOf("x" to 6.0)))
        assertNull(eval("fun f(x) = x"))
        val f = compileFunction1<Double, Double>("fun sq(v) = v * v; sq(t) + 1", "t")
        assertEquals(10.0, f(3.0))
        assertEquals(17.0, f(4.0))
    }

    @Test
    fun `a function is declared once, after its body`() {
        val twice = assertFailsWith<ExpressionException> { eval("fun f(x) = x; fun f(y) = y; f(1)") }
        assertEquals(listOf(Kind.DUPLICATE_FUNCTION), twice.problems.map { it.kind })
        val parameters = assertFailsWith<ExpressionException> { eval("fun f(x, x) = x; 1") }
        assertEquals(listOf(Kind.DUPLICATE_VALUE), parameters.problems.map { it.kind })
        // a function can not call itself
        val itself = assertFailsWith<ExpressionException> { eval("fun f(x) = x < 1 ? 0 : f(x - 1); f(3)") }
        assertEquals(listOf(Kind.UNKNOWN_FUNCTION), itself.problems.map { it.kind })
        val many = assertFailsWith<ExpressionException> { eval("fun f(a, b, c, d, e, g) = a; 1") }
        assertEquals(listOf(Kind.EVALUATION), many.problems.map { it.kind })
        assertFailsWith<ExpressionException> { eval("fun f(x) { val y = x }; f(1)") }
    }

    @Test
    fun `the problems and names of a program with functions`() {
        val program = "fun f(x) = x * k; fun f(y) = y; fun g(a, a) = a; f(1, 2) + h(2) + [1].map { f(it + z) }.size"
        assertEquals(
            listOf(
                Kind.DUPLICATE_FUNCTION to "f", Kind.DUPLICATE_VALUE to "a", Kind.WRONG_ARGUMENTS to "f(1, 2)",
                Kind.UNKNOWN_FUNCTION to "h", Kind.UNKNOWN_VALUE to "z"
            ),
            checkTypedExpression(program, setOf("k")).map { it.kind to program.substring(it.start, it.end) }
        )
        assertTrue(checkTypedExpression("fun f(x) { val y = x * k; [y].map { it + x } }; f(2)", setOf("k")).isEmpty())
        assertEquals(setOf("k", "z"), typedExpressionNames("fun f(x) { val y = x * k; y }; f(z)"))
    }
}
