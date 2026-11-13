import org.openrndr.extra.expressions.ExpressionException
import org.openrndr.extra.expressions.ExpressionProblem.Kind
import org.openrndr.extra.expressions.FunctionExtensions
import org.openrndr.extra.expressions.builtInFunctions
import org.openrndr.extra.expressions.checkExpression
import org.openrndr.extra.expressions.compileExpression
import org.openrndr.extra.expressions.evaluateExpression
import org.openrndr.extra.expressions.expressionNames
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class TestExpressionProblems {
    /** the problems of [expression] as kind, the text they mark and message */
    private fun problems(expression: String, values: Set<String> = setOf("a", "b"), functions: FunctionExtensions = FunctionExtensions.EMPTY) =
        checkExpression(expression, values, functions).map { Triple(it.kind, expression.substring(it.start, it.end), it.message) }

    @Test
    fun `problems mark where they are`() {
        assertEquals(listOf(Triple(Kind.SYNTAX, "(", "missing ')'")), problems("a * (b"))
        assertEquals(listOf(Triple(Kind.SYNTAX, "+", "incomplete expression")), problems("a +"))
        assertEquals(listOf(Triple(Kind.SYNTAX, "=", "unexpected '='")), problems("3 = 5"))
        assertEquals(listOf(Triple(Kind.SYNTAX, "#", "unexpected '#'")), problems("a * # b"))
        assertEquals(listOf(Triple(Kind.UNKNOWN_VALUE, "r", "no value r")), problems("a * r"))
        assertEquals(listOf(Triple(Kind.WRONG_ARGUMENTS, "sin(a, b)", "sin takes 1 argument, not 2")), problems("sin(a, b)"))
        assertEquals(listOf(Triple(Kind.WRONG_ARGUMENTS, "min(a)", "min takes 2 to 5 arguments, not 1")), problems("min(a)"))
        assertEquals(listOf(Triple(Kind.WRONG_ARGUMENTS, "random(a)", "random takes 0 or 2 arguments, not 1")), problems("random(a)"))
        assertEquals(listOf(Triple(Kind.UNKNOWN_FUNCTION, "foo", "no function foo")), problems("foo(a)"))
        assertEquals(listOf(Triple(Kind.UNSUPPORTED, "a < b", "comparisons are not supported")), problems("a < b"))
        // the names of values and functions that are given, and PI
        assertTrue(problems("a * r + foo(b) + PI", setOf("a", "b", "r"), FunctionExtensions(functions1 = mapOf("foo" to { x -> x }))).isEmpty())
        // all of them, in the order of the text
        assertEquals(
            listOf(Kind.UNKNOWN_VALUE to "x", Kind.WRONG_ARGUMENTS to "sin(a, b)", Kind.UNKNOWN_FUNCTION to "foo"),
            problems("x + sin(a, b) + foo()").map { it.first to it.second }
        )
        assertEquals("r", checkExpression("a * r", setOf("a")).single().name)
    }

    @Test
    fun `an expression that ends early is incomplete there only`() {
        val values = setOf("t", "a", "b", "c", "d", "i", "n")
        for ((expression, plus) in listOf("a * sin(t + i) * 2 + " to 19, "a * sin(t + i) * 2 +" to 19, "sin(t) + " to 7, "a * 2 + " to 6)) {
            val problems = checkExpression(expression, values)
            assertEquals(listOf(Triple(Kind.SYNTAX, plus, "incomplete expression")), problems.map { Triple(it.kind, it.start, it.message) }, expression)
        }
        // problems at several places, in the order of the text
        val several = checkExpression("a ) * (b", values)
        assertEquals(several.sortedBy { it.start }, several)
    }

    @Test
    fun `exceptions tell where the problems are`() {
        val syntax = assertFailsWith<ExpressionException> { evaluateExpression("a * (b", mapOf("a" to 1.0, "b" to 2.0)) }
        assertEquals(Kind.SYNTAX, syntax.problems.single().kind)
        assertEquals(4 until 5, syntax.problems.single().let { it.start until it.end })

        val value = assertFailsWith<ExpressionException> { evaluateExpression("a * r", mapOf("a" to 1.0)) }
        assertEquals(listOf(Kind.UNKNOWN_VALUE to (4 until 5)), value.problems.map { it.kind to (it.start until it.end) })
        // the message is as it was
        assertTrue(value.message!!.startsWith("error in evaluation of 'a*r': unresolved value: 'r'"))

        val function = assertFailsWith<ExpressionException> { evaluateExpression("1 + foo(2)") }
        assertEquals(listOf(Kind.UNKNOWN_FUNCTION to (4 until 7)), function.problems.map { it.kind to (it.start until it.end) })
        val arguments = assertFailsWith<ExpressionException> { evaluateExpression("1 + sin(2, 3)") }
        assertEquals(listOf(Kind.WRONG_ARGUMENTS to (4 until 13)), arguments.problems.map { it.kind to (it.start until it.end) })
        assertEquals("sin takes 1 argument, not 2", arguments.problems.single().message)

        // a compiled expression whose value is missing evaluates once it is there
        val values = mutableMapOf("a" to 2.0)
        val f = compileExpression("a * r", values)
        val missing = assertFailsWith<ExpressionException> { f() }
        assertEquals("r", missing.problems.single().name)
        values["r"] = 3.0
        assertEquals(6.0, f())
    }

    @Test
    fun `the names an expression reads, and the functions it can call`() {
        assertEquals(setOf("t", "a", "radius"), expressionNames("sin(t * a) + radius * PI + max(a, 2)"))
        // as far as it can be read
        assertEquals(setOf("t", "a"), expressionNames("t * (a +"))
        assertEquals(setOf(1), builtInFunctions["sin"])
        assertEquals(setOf(2, 3, 4, 5), builtInFunctions["max"])
    }
}
