package typed

import org.openrndr.extra.expressions.ExpressionProblem.Kind
import org.openrndr.extra.expressions.typed.*
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.math.mix
import kotlin.math.sin
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class TestTypedCompiler {
    private fun eval(expression: String, values: Map<String, Any> = emptyMap()) = evaluateTypedExpression(expression, { values[it] })

    @Test
    fun `expressions of every kind evaluate to what they did`() {
        assertEquals(emptyList<Any>(), eval("[] + []"))
        assertEquals(List(5) { listOf(1.0, 1.0, 2.0) }.flatten(), eval("([1] * 2 + [2] * 1) * 5"))
        assertEquals(listOf(1.0), eval("[[0, 1, 2][1]]"))
        assertEquals(2.0, eval("[0, 1, 2].max()"))
        assertEquals("three", eval("""["one", "two", "three"].maxBy { x -> x.length }"""))
        assertEquals(1.3, eval("2.0 > 0.5 ? 1.3 : 0.7"))
        assertEquals(Vector3(1.0, 0.0, 0.0), eval("(translate(vec3(1.0, 0.0, 0.0)) * vec4(0.0, 0.0, 0.0, 1.0)).xyz"))
        assertEquals(8.0, eval("a.b.c", mapOf("a" to mapOf("b" to mapOf("c" to 8.0)))))
        assertEquals("", eval("\"\""))
        assertEquals("ab ab ", eval("\"ab \" * 2"))
        assertEquals(listOf(0.0, 2.0, 4.0), eval("(0..4 step 2)"))
        assertEquals(1.0, eval("!!2.0"))
        // the parameters of the lambdas around a lambda
        assertEquals(listOf(listOf(11.0, 21.0), listOf(12.0, 22.0)), eval("[1, 2].map { x -> [10, 20].map { y -> x + y } }"))
        assertEquals(listOf(3.0, 4.0), eval("[1, 2].map { it + c }", mapOf("c" to 2.0)))
        // round, halves away from zero, also of vectors
        assertEquals(3.0, eval("round(2.5)"))
        assertEquals(Vector2(1.0, -2.0), eval("round(vec2(0.5, -1.5))"))
    }

    @Test
    fun `compiled expressions give exactly what Kotlin gives`() {
        val random = Random(5)
        val values = mutableMapOf<String, Any>()
        val cases: List<Pair<String, () -> Any>> = listOf(
            "a + b * c - a / b" to { values.d("a") + values.d("b") * values.d("c") - values.d("a") / values.d("b") },
            "sin(a) * -b % c" to { (sin(values.d("a")) * -values.d("b")).mod(values.d("c")) },
            "p + q * a - p / b" to { values.v("p") + values.v("q") * values.d("a") - values.v("p") / values.d("b") },
            "mix(p, q, c).length + (p - q).x" to { values.v("p").mix(values.v("q"), values.d("c")).length + (values.v("p") - values.v("q")).x },
        )
        for ((expression, kotlin) in cases) {
            val f = compileTypedExpression(expression, { values[it] })
            repeat(1000) {
                for (k in listOf("a", "b", "c")) values[k] = random.nextDouble(-10.0, 10.0)
                for (k in listOf("p", "q")) values[k] = Vector2(random.nextDouble(-10.0, 10.0), random.nextDouble(-10.0, 10.0))
                assertEquals(kotlin(), f(), expression)
            }
        }
    }

    @Test
    fun `problems tell where they are, also of what goes wrong while evaluating`() {
        fun problems(expression: String, values: Set<String> = setOf("p", "q")) =
            checkTypedExpression(expression, values).map { Triple(it.kind, expression.substring(it.start, it.end), it.message) }
        assertEquals(listOf(Triple(Kind.SYNTAX, "(", "missing ')'")), problems("p + q.x * (p"))
        assertEquals(listOf(Triple(Kind.UNKNOWN_VALUE, "r", "no value r")), problems("p + r"))
        assertEquals(listOf(Triple(Kind.UNKNOWN_FUNCTION, "foo", "no function foo")), problems("foo(p)"))
        assertEquals(listOf(Triple(Kind.WRONG_ARGUMENTS, "rotate(p)", "rotate takes 2 arguments, not 1")), problems("rotate(p)"))
        // the parameters of lambdas are values, names inside lambdas are checked
        assertEquals(listOf(Triple(Kind.UNKNOWN_VALUE, "y", "no value y")), problems("[1, 2].map { x -> x * y + it2 }", setOf("it2")))
        assertEquals(listOf(Kind.UNSUPPORTED), problems("{ a, b, c -> a }").map { it.first })
        // a constant can be a function
        assertTrue(problems("f(p)", setOf("f", "p")).isEmpty())

        // what goes wrong while evaluating is an ExpressionException with where it went wrong
        val missing = assertFailsWith<ExpressionException> { eval("p + r") }
        assertEquals(listOf(Kind.UNKNOWN_VALUE, Kind.UNKNOWN_VALUE), missing.problems.map { it.kind })
        val types = assertFailsWith<ExpressionException> { eval("1 + (\"a\" + 2)") }
        assertEquals(Kind.EVALUATION, types.problems.single().kind)
        assertEquals("\"a\" + 2", "1 + (\"a\" + 2)".substring(types.problems.single().start, types.problems.single().end))
        assertEquals("unsupported operands for +: String and Double", types.problems.single().message)
        val member = assertFailsWith<ExpressionException> { eval("[1, 2].sum()") }
        assertEquals("List has no function 'sum'", member.problems.single().message)
        val syntax = assertFailsWith<ExpressionException> { eval("1 +") }
        assertEquals(Kind.SYNTAX, syntax.problems.single().kind)

        assertEquals(setOf("p", "c"), typedExpressionNames("[p, 2].map { x -> x * c + PI }"))
    }
}

private fun Map<String, Any>.d(k: String) = this[k] as Double
private fun Map<String, Any>.v(k: String) = this[k] as Vector2
