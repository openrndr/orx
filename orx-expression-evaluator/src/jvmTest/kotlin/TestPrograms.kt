import org.openrndr.extra.expressions.ExpressionException
import org.openrndr.extra.expressions.ExpressionProblem.Kind
import org.openrndr.extra.expressions.checkExpression
import org.openrndr.extra.expressions.compileExpression
import org.openrndr.extra.expressions.compileFunction1
import org.openrndr.extra.expressions.evaluateExpression
import org.openrndr.extra.expressions.expressionNames
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
}
