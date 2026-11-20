import org.openrndr.extra.expressions.ExpressionException
import org.openrndr.extra.expressions.ExpressionProblem.Kind
import org.openrndr.extra.expressions.checkExpression
import org.openrndr.extra.expressions.compileExpression
import org.openrndr.extra.expressions.compileFunction1
import org.openrndr.extra.expressions.evaluateExpression
import org.openrndr.extra.expressions.expressionNames
import kotlin.math.PI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TestPrograms {
    @Test
    fun `a program declares values for the statements after it`() {
        assertEquals(8.0, evaluateExpression("val a = 5; val b = 3; a + b"))
        assertEquals(8.0, evaluateExpression("val a = 5\nval b = 3\na + b"))
        // separators in a row, before and after the statements
        assertEquals(8.0, evaluateExpression("\nval a = 5;\n  val b = a - 2;\n\na + b;\n"))
        assertEquals(30.0, evaluateExpression("val a = x * 2; sqrt(a * a) + x", constants = mapOf("x" to 10.0)))
        // a value comes before the constant of the same name, which its own expression reads
        assertEquals(20.0, evaluateExpression("val x = x * 2; x", constants = mapOf("x" to 10.0)))
        // a program that ends in a declaration gives no value
        assertNull(evaluateExpression("val a = 5"))
    }

    @Test
    fun `a compiled program evaluates again with the constants of then`() {
        val constants = mutableMapOf("t" to 1.0)
        val program = compileExpression("val u = t * t; u + 1", constants)
        assertEquals(2.0, program())
        constants["t"] = 3.0
        assertEquals(10.0, program())
        val f = compileFunction1("val y = x * 2; y + 1", "x")
        assertEquals(5.0, f(2.0))
        assertEquals(7.0, f(3.0))
    }

    @Test
    fun `a value is declared once, before it is read`() {
        val twice = assertFailsWith<ExpressionException> { evaluateExpression("val a = 1; val a = 2; a") }
        assertEquals(listOf(Kind.DUPLICATE_VALUE), twice.problems.map { it.kind })
        val before = assertFailsWith<ExpressionException> { evaluateExpression("val a = b; val b = 2; a") }
        assertEquals(listOf(Kind.UNKNOWN_VALUE), before.problems.map { it.kind })
        // val is a keyword
        assertFailsWith<ExpressionException> { evaluateExpression("val = 2") }
    }

    @Test
    fun `the problems and names of a program`() {
        val program = "val a = x * 2; val b = a + y; val a = 3; b + c"
        assertEquals(
            listOf(Kind.DUPLICATE_VALUE to "a", Kind.UNKNOWN_VALUE to "c"),
            checkExpression(program, setOf("x", "y")).map { it.kind to program.substring(it.start, it.end) }
        )
        assertTrue(checkExpression("val a = x; a", setOf("x")).isEmpty())
        // the values a program reads, not the values it declares, also when it can not be read
        assertEquals(setOf("x", "y", "c"), expressionNames(program))
        assertEquals(setOf("x", "y", "c"), expressionNames("val a = x * 2; val b = a + y; b + c +"))
        // a value read before it is declared is read from the constants
        assertEquals(setOf("a"), expressionNames("val b = a; val a = 2; a"))
    }

    @Test
    fun `a program declares functions for the statements after it`() {
        assertEquals(175.0, evaluateExpression("fun f(x) { x * 5.0 }; fun g(x) = x * 6.0; val a = f(5); val b = g(a); a + b"))
        // a body of statements, over lines, that reads the values and functions declared before the function
        assertEquals(23.0, evaluateExpression("val k = 2\nfun h(x) = x + k\nfun f(x, y) {\n  val s = x * y\n  h(s) + 1\n}\nf(4, 5)"))
        // functions with as many parameters as a call takes, and none
        assertEquals(15.0, evaluateExpression("fun s(a, b, c, d, e) = a + b + c + d + e; s(1, 2, 3, 4, 5)"))
        assertEquals(PI, evaluateExpression("fun p() = PI; p()"))
        // overloads by number of parameters, functions in functions, and calls in the arguments of a call
        assertEquals(1.0 + 5.0, evaluateExpression("fun f(x) = x; fun f(x, y) = x + y; f(1) + f(2, 3)"))
        assertEquals(12.0, evaluateExpression("fun f(x) { fun g(y) = x * y; g(3) }; f(4)"))
        assertEquals(1.0 + 4.0 * 2, evaluateExpression("fun f(x, y) = x + y * 2; f(1, f(0, 2))"))
        // a declared function comes before the built-in function and the constants of the same name
        assertEquals(4.0, evaluateExpression("fun sin(x) = x * 2; sin(2)"))
        assertEquals(3.0, evaluateExpression("fun f(x) = x + 1; f(x)", constants = mapOf("x" to 2.0)))
        // the parameters of a function only in its body
        assertEquals(12.0, evaluateExpression("fun f(x) = x * 2; f(3) + x", constants = mapOf("x" to 6.0)))
        assertNull(evaluateExpression("fun f(x) = x"))
        val f = compileFunction1("fun sq(v) = v * v; sq(t) + 1", "t")
        assertEquals(10.0, f(3.0))
        assertEquals(17.0, f(4.0))
    }

    @Test
    fun `a function is declared once, after its body`() {
        val twice = assertFailsWith<ExpressionException> { evaluateExpression("fun f(x) = x; fun f(y) = y; f(1)") }
        assertEquals(listOf(Kind.DUPLICATE_FUNCTION), twice.problems.map { it.kind })
        val parameters = assertFailsWith<ExpressionException> { evaluateExpression("fun f(x, x) = x; 1") }
        assertEquals(listOf(Kind.DUPLICATE_VALUE), parameters.problems.map { it.kind })
        // a function can not call itself
        val itself = assertFailsWith<ExpressionException> { evaluateExpression("fun f(x) = f(x); f(1)") }
        assertEquals(listOf(Kind.UNKNOWN_FUNCTION), itself.problems.map { it.kind })
        val arguments = assertFailsWith<ExpressionException> { evaluateExpression("fun f(x) = x; f(1, 2)") }
        assertEquals(listOf("f takes 1 argument, not 2"), arguments.problems.map { it.message })
        val many = assertFailsWith<ExpressionException> { evaluateExpression("fun f(a, b, c, d, e, g) = a; 1") }
        assertEquals(listOf(Kind.UNSUPPORTED), many.problems.map { it.kind })
        // a body ends in an expression
        assertFailsWith<ExpressionException> { evaluateExpression("fun f(x) { val y = x }; f(1)") }
    }

    @Test
    fun `the problems and names of a program with functions`() {
        val program = "fun f(x) = x * k; fun f(y) = y; fun g(a, a) = a; f(1, 2) + h(2) + f(z)"
        assertEquals(
            listOf(
                Kind.DUPLICATE_FUNCTION to "f", Kind.DUPLICATE_VALUE to "a", Kind.WRONG_ARGUMENTS to "f(1, 2)",
                Kind.UNKNOWN_FUNCTION to "h", Kind.UNKNOWN_VALUE to "z"
            ),
            checkExpression(program, setOf("k")).map { it.kind to program.substring(it.start, it.end) }
        )
        assertTrue(checkExpression("fun f(x) { val y = x * k; y }; f(2)", setOf("k")).isEmpty())
        assertEquals(setOf("k", "z"), expressionNames("fun f(x) { val y = x * k; y }; f(z)"))
        assertEquals(setOf("k", "z"), expressionNames("fun f(x) { val y = x * k; y }; f(z) +"))
    }

    @Test
    fun `a lambda over lines is read, not evaluated`() {
        val program = "val k = 2\n[1, 2].map {\n    val y = it * k\n    y + z\n}"
        assertEquals(
            listOf(
                Kind.UNSUPPORTED to "[1, 2].map {\n    val y = it * k\n    y + z\n}", Kind.UNSUPPORTED to "[1, 2]",
                Kind.UNKNOWN_VALUE to "z"
            ),
            checkExpression(program, emptySet()).map { it.kind to program.substring(it.start, it.end) }
        )
        // what a lambda declares is not declared after it
        assertEquals(setOf("y"), expressionNames("[1].map { val y = it; y }; y"))
    }
}
