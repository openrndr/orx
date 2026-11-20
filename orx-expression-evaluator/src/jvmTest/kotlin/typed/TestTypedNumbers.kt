package typed

import org.openrndr.extra.expressions.ExpressionProblem.Kind
import org.openrndr.extra.expressions.typed.ExpressionException
import org.openrndr.extra.expressions.typed.compileFunction1
import org.openrndr.extra.expressions.typed.compileTypedExpression
import org.openrndr.extra.expressions.typed.evaluateTypedExpression
import org.openrndr.math.Vector2
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/** numbers are computed without boxing them, and values that are no numbers among them as before */
class TestTypedNumbers {
    private fun eval(expression: String, vararg constants: Pair<String, Any>) =
        evaluateTypedExpression(expression, { name -> constants.toMap()[name] })

    @Test
    fun `numbers give what they gave`() {
        val f = compileFunction1<Double, Double>("x * sin(x) * 2 + 1 - x / 3 % 2", "x")
        for (x in listOf(0.0, 0.5, 2.0, -3.25)) assertEquals(x * sin(x) * 2 + 1 - (x / 3).mod(2.0), f(x))
        assertEquals(3.0, eval("round(2.5)"))
        assertEquals(-1.0, eval("round(-0.5)"))
        assertEquals(1.0, eval("min(3, 1) + clamp(-2, 0, 1) * mix(0, 10, 0.5)"))
        assertEquals(listOf(1.0, 0.0, 1.0, 1.0, 0.0), eval("[2 > 1, 2 < 1, 1 <= 1, -0.0 == 0.0, !3]"))
        assertEquals(5.0, eval("1 > 2 ? 3 : 5"))
        assertEquals(Double.NaN, eval("0 / 0"))
    }

    @Test
    fun `values that are no numbers among numbers`() {
        assertEquals(Vector2(2.0, 5.0), eval("vec2(1, 2) * 2 + vec2(0, x)", "x" to 1.0))
        assertEquals(Vector2(-1.0, -2.0), eval("-vec2(1, 2)"))
        assertEquals(Vector2(0.0, 1.0), eval("sin(vec2(0, PI / 2))"))
        assertEquals(Vector2(1.0, 0.0), eval("min(vec2(1, 3), vec2(2, 0))"))
        assertEquals(Vector2(0.5, 1.0), eval("clamp(vec2(0.5, 2), 0, 1)"))
        assertEquals(Vector2(1.0, 1.0), eval("x > 1 ? vec2(1) : 2", "x" to 2.0))
        assertEquals(2.0, eval("(x > 1 ? vec2(1) : 2) * 1", "x" to 0.0))
        assertEquals("ab" + "ab", eval("\"ab\" * 2"))
        assertEquals(listOf(4.0), eval("[1].map { val y = it + 1; y * 2 }"))
    }

    @Test
    fun `missing names and wrong values among numbers`() {
        val missing = assertFailsWith<ExpressionException> { eval("sin(q) + 1 + r * 2") }
        assertEquals(listOf(Kind.UNKNOWN_VALUE to "q", Kind.UNKNOWN_VALUE to "r"), missing.problems.map { it.kind to it.name })
        val condition = assertFailsWith<ExpressionException> { eval("vec2(1) ? 1 : 2") }
        assertEquals("a condition is a number, not Vector2", condition.problems.single().message)
        val operands = assertFailsWith<ExpressionException> { eval("1 + vec2(1)") }
        assertEquals("unsupported operands for +: Double and Vector2", operands.problems.single().message)
        assertFailsWith<ExpressionException> { eval("!vec2(1)") }
        assertFailsWith<ExpressionException> { eval("1 < vec2(1)") }
    }

    @Test
    fun `calls of a function in the arguments of calls of it`() {
        val f = compileFunction1<Double, Double>("fun f(a, b) = a * 10 + b\nf(1, f(2, 3)) + f(f(4, 5), f(6, f(7, 8)))", "x")
        repeat(3) { assertEquals(10.0 + 23.0 + (450.0 + 6 * 10 + 78), f(0.0)) }
        val g = compileFunction1<Double, Double>("fun h(p) = p * 2\nfun k(p) = h(p) + h(p + 1)\nk(x) + k(x * 2) + h(k(x))", "x")
        for (x in listOf(1.0, 2.5, -3.0)) {
            fun h(p: Double) = p * 2
            fun k(p: Double) = h(p) + h(p + 1)
            assertEquals(k(x) + k(x * 2) + h(k(x)), g(x))
        }
    }

    @Test
    fun `the values of calls and evaluations stay those of their own`() {
        // a lambda in a function keeps the values of its call, a lambda of the program those of its evaluation
        @Suppress("UNCHECKED_CAST")
        val adders = compileFunction1<Double, List<Any>>("fun adder(k) = { y -> y + k }\n[adder(x), adder(x * 10)]", "x")
        @Suppress("UNCHECKED_CAST")
        val first = adders(1.0) as List<(Any) -> Any>
        @Suppress("UNCHECKED_CAST")
        val second = adders(2.0) as List<(Any) -> Any>
        assertEquals(listOf(101.0, 110.0, 102.0, 120.0), (first + second).map { it(100.0) })
        @Suppress("UNCHECKED_CAST")
        val lambda = compileFunction1<Double, (Any) -> Any>("val k = x\n{ y -> y * k }", "x")
        val times2 = lambda(2.0)
        val times3 = lambda(3.0)
        assertEquals(listOf(20.0, 30.0), listOf(times2(10.0), times3(10.0)))
        // a call of a function in a lambda, while it is called
        val mapped = compileTypedExpression("fun f(v) = v * 2\nf([1, 2].map { f(it) + 1 }.max())")
        assertEquals(10.0, mapped())
    }

    @Test
    fun `evaluations on threads at the same time`() {
        val f = compileFunction1<Double, Double>("fun g(p, q) {\n  val s = p * q\n  s + p\n}\nval a = x + 1\ng(a, x) + g(x, a)", "x")
        val pool = Executors.newFixedThreadPool(8)
        val results = (0 until 8).map { t ->
            pool.submit<Boolean> {
                (0 until 20_000).all { i ->
                    val x = t * 1000.0 + i
                    val a = x + 1
                    f(x) == (a * x + a) + (x * a + x)
                }
            }
        }
        val all = results.all { it.get() }
        pool.shutdown()
        pool.awaitTermination(10, TimeUnit.SECONDS)
        assertTrue(all)
    }

    @Test
    fun `an evaluation in an evaluation of the same expression`() {
        lateinit var f: (Double) -> Double
        val inner = { v: Any -> if ((v as Double) > 0.0) f(v - 1.0) else 0.0 }
        f = compileFunction1("fun g(p) = p * 2 + 1\ng(x) + g(again(x))", "x", { if (it == "again") inner else null })
        // f(x) = 2x + 1 + 2 f(x - 1) + 1, f(0) = 1 + 1
        fun expected(x: Double): Double = if (x > 0.0) 2 * x + 1 + 2 * expected(x - 1) + 1 else 2.0
        assertEquals(expected(3.0), f(3.0))
    }
}
