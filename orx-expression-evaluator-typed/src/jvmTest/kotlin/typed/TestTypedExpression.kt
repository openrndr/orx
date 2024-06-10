package typed

import org.junit.jupiter.api.Assertions.assertEquals
import org.openrndr.extra.expressions.typed.evaluateTypedExpression
import org.openrndr.math.Vector2
import kotlin.test.Test

class TestTypedExpression {

    @Test
    fun testTernary() {
        println("result is: ${evaluateTypedExpression("2.0 > 0.5 ? 1.3 : 0.7")}")
    }

    @Test
    fun testJoin() {
        assertEquals(0.0, evaluateTypedExpression("1.0 && 0.0"))
        assertEquals(1.0, evaluateTypedExpression("1.0 && 1.0"))
        assertEquals(0.0, evaluateTypedExpression("0.0 && 0.0"))
        assertEquals(0.0, evaluateTypedExpression("0.0 && 1.0"))

        assertEquals(1.0, evaluateTypedExpression("1.0 || 0.0"))
        assertEquals(1.0, evaluateTypedExpression("1.0 || 1.0"))
        assertEquals(0.0, evaluateTypedExpression("0.0 || 0.0"))
        assertEquals(1.0, evaluateTypedExpression("0.0 || 1.0"))

        assertEquals(1.0, evaluateTypedExpression("(0.0 || 1.0) && (1.0 || 0.0)"))
    }

    @Test
    fun testNegate() {
        assertEquals(0.0, evaluateTypedExpression("!1.0"))
        assertEquals(1.0, evaluateTypedExpression("!0.0"))
        assertEquals(1.0, evaluateTypedExpression("!!2.0"))
    }

    @Test
    fun testTyped() {
        println(evaluateTypedExpression("vec2(1.0, 1.0) + vec2(1.0, 1.0)"))
        println(evaluateTypedExpression("vec3(1.0, 1.0, 1.0) + vec3(2.0, 3.0, 4.0)"))
        println(evaluateTypedExpression("vec3(1.0, 1.0, 1.0) * vec3(2.0, 3.0, 4.0)"))
        println(evaluateTypedExpression("translate(vec3(1.0, 0.0, 0.0)) * mat4(vec4(1,0,0,0), vec4(0,1,0,0), vec4(0,0,1,0), vec4(0.0, 0.0, 0.0, 1.0))"))
        println(evaluateTypedExpression("(translate(vec3(1.0, 0.0, 0.0)) * vec4(0.0, 0.0, 0.0, 1.0)).xyz"))
    }

    fun Map<String, Any>.function(): (String) -> Any? {
        return fun(p: String): Any? {
            val v = this[p]
            if (v is Map<*, *>) {
                return (v as Map<String, Any>).function()
            } else {
                return v
            }
        }
    }

    @Test
    fun testPropref() {
        println(evaluateTypedExpression("a.b.c", constants = mapOf("a" to mapOf("b" to mapOf("c" to 8.0))).function()))
        println(
            evaluateTypedExpression(
                "a.yx.yx.normalized * -5.0",
                constants = mapOf("a" to Vector2(1.0, 2.0)).function()
            )
        )
        println(
            evaluateTypedExpression(
                "vec2(2.0, 3.0).normalized",
                constants = mapOf("a" to Vector2(1.0, 2.0)).function()
            )
        )
    }

    @Test
    fun testMethodCall() {
        println(
            evaluateTypedExpression(
                "a.b.c(5.0) + a.b.sum(3.0, 5.0)",
                constants = mapOf(
                    "a" to
                            mapOf(
                                "b" to
                                        mapOf(
                                            "c" to { x: Double -> x * 5.0 },
                                            "sum" to { x: Double, y: Double -> x + y }
                                        )
                            )
                ).function()
            )
        )
    }

    @Suppress("NAME_SHADOWING")
    @Test
    fun testMethodCallF() {
        println(
            evaluateTypedExpression(
                "vec2(2.0, 3.0) * (a.b.c(5.0) + a.b.sum(3.0, 5.0))",
                constants = { name: String ->
                    when (name) {
                        "a" -> { name: String ->
                            when (name) {
                                "b" -> { name: String ->
                                    when (name) {
                                        "c" -> { x: Double -> x * 5.0 }
                                        "sum" -> { x: Double, y: Double -> x + y }
                                        else -> null
                                    }
                                }

                                else -> null
                            }
                        }

                        else -> null
                    }
                }
            )
        )
    }

}