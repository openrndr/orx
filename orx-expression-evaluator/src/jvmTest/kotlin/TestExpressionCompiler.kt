import org.openrndr.extra.expressions.ExpressionException
import org.openrndr.extra.expressions.ExpressionProblem.Kind
import org.openrndr.extra.expressions.FunctionExtensions
import org.openrndr.extra.expressions.compileExpression
import org.openrndr.extra.expressions.compileFunction1
import org.openrndr.extra.expressions.evaluateExpression
import org.openrndr.math.map
import org.openrndr.math.mix
import org.openrndr.math.smoothstep
import kotlin.math.*
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class TestExpressionCompiler {
    @Test
    fun `compiled expressions give exactly what the same arithmetic in Kotlin gives`() {
        val cases: List<Pair<String, (Double, Double, Double) -> Double>> = listOf(
            "a + b * c" to { a, b, c -> a + b * c },
            "a - b - c + a" to { a, b, c -> a - b - c + a },
            "a / b / c * a" to { a, b, c -> a / b / c * a },
            "-a * b - -c" to { a, b, c -> -a * b - -c },
            "a % b + -a % b" to { a, b, _ -> a.mod(b) + (-a).mod(b) },
            "(a + b) * (c - a)" to { a, b, c -> (a + b) * (c - a) },
            "sin(a) * cos(b) + tan(c) + atan2(a, b) + atan(c)" to { a, b, c -> sin(a) * cos(b) + tan(c) + atan2(a, b) + atan(c) },
            "pow(abs(a), b) + sqrt(abs(c)) + exp(b / 10)" to { a, b, c -> abs(a).pow(b) + sqrt(abs(c)) + exp(b / 10) },
            "mix(a, b, c) + smoothstep(a, b, c) + map(a, b, c, 0, 1)" to { a, b, c -> mix(a, b, c) + smoothstep(a, b, c) + map(a, b, c, 0.0, 1.0) },
            "min(a, b, c) + max(a, b) + min(a, b, c, 1, 2) + sum(a, b, c)" to { a, b, c -> minOf(a, b, c) + max(a, b) + minOf(a, b, c, 1.0, 2.0) + (a + b + c) },
            "length(a, b, c) + length(a, b) + floor(a) + ceil(b) + saturate(c) + mod(a, b)" to { a, b, c ->
                sqrt(a * a + b * b + c * c) + sqrt(a * a + b * b) + floor(a) + ceil(b) + c.coerceIn(0.0, 1.0) + a.mod(b)
            },
            "a * PI" to { a, _, _ -> a * PI },
        )
        val random = Random(3)
        val values = mutableMapOf("a" to 0.0, "b" to 0.0, "c" to 0.0)
        for ((expression, kotlin) in cases) {
            val f = compileExpression(expression, values)
            repeat(2000) {
                val (a, b, c) = List(3) { random.nextDouble(-10.0, 10.0) }
                values["a"] = a; values["b"] = b; values["c"] = c
                val want = kotlin(a, b, c)
                val got = f()
                assertTrue(got == want || (got.isNaN() && want.isNaN()), "$expression at $a, $b, $c: $got, not $want")
            }
        }
    }

    @Test
    fun `numbers with exponents and without a leading digit`() {
        for ((text, value) in listOf("1e3" to 1000.0, "2.5E-3" to 0.0025, ".5" to 0.5, ".5e2" to 50.0, "1e+2" to 100.0, "3 * 1e-1" to 3 * 0.1, "007" to 7.0)) {
            assertEquals(value, evaluateExpression(text), text)
        }
        // a number that ends in its dot is not one: it is the start of a range or a member
        assertFailsWith<ExpressionException> { evaluateExpression("1.") }
    }

    @Test
    fun `round rounds halves away from zero`() {
        for ((x, r) in listOf(2.5 to 3.0, -2.5 to -3.0, 0.5 to 1.0, -0.5 to -1.0, 1.4 to 1.0, -1.6 to -2.0, 0.49999999999999994 to 0.0, 3.0 to 3.0)) {
            assertEquals(r, evaluateExpression("round(x)", mapOf("x" to x)), "round($x)")
        }
    }

    @Test
    fun `compiled expressions follow their values and functions`() {
        // the values as they are at every call
        val values = mutableMapOf("x" to 1.0)
        val f = compileExpression("x * 2", values)
        assertEquals(2.0, f())
        values["x"] = 5.0
        assertEquals(10.0, f())
        // a function that is added after the expression is compiled
        val functions = mutableMapOf<String, (Double) -> Double>()
        val g = compileExpression("twice(x)", values, FunctionExtensions(functions1 = functions))
        val missing = assertFailsWith<ExpressionException> { g() }
        assertEquals(Kind.UNKNOWN_FUNCTION, missing.problems.single().kind)
        functions["twice"] = { it * 2 }
        assertEquals(10.0, g())
        // compiled functions of a parameter
        val h = compileFunction1("x * x + 1", "x")
        assertEquals(10.0, h(3.0))
    }

    @Test
    fun `all the names that are missing are reported, with the message of the last`() {
        val e = assertFailsWith<ExpressionException> { evaluateExpression("p + q * 2", mapOf()) }
        assertEquals(listOf("p", "q"), e.problems.map { it.name })
        assertTrue(e.message!!.startsWith("error in evaluation of 'p+q*2': unresolved value: 'q'"), e.message)
        // what the grammar reads but is not evaluated
        val unsupported = assertFailsWith<ExpressionException> { evaluateExpression("1 < 2") }
        assertEquals(Kind.UNSUPPORTED, unsupported.problems.single().kind)
    }
}
