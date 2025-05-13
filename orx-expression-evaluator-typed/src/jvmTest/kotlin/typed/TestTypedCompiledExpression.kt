package typed

import org.openrndr.extra.expressions.typed.compileFunction1OrNull
import org.openrndr.extra.noise.uniform
import org.openrndr.math.Vector2
import kotlin.test.Test
import kotlin.test.assertEquals

class TestTypedCompiledExpression {

    @Test
    fun testStringLiteral() {
        run {
            val c = compileFunction1OrNull<Double, String>(""""hoi"""", "t")!!
            val v = c(0.0)
            assertEquals("hoi", v)
        }
        run {
            val c = compileFunction1OrNull<Double, String>(""""hoi" + " " + "doei" * t""", "t")!!
            val v = c(2.0)
            assertEquals("hoi doeidoei", v)
        }

        run {
            val c = compileFunction1OrNull<Double, String>(""""hoi".take(t)""", "t")!!
            val v = c(2.0)
            assertEquals("ho", v)
        }
    }

    @Test
    fun testComparison() {
        run {
            val c = compileFunction1OrNull<Double, Double>("""t == t""", "t")!!
            val v = c(0.0)
            assertEquals(1.0, v)
        }
    }

    @Test
    fun testFunction1() {
        run {
            val c = compileFunction1OrNull<Double, Double>("x + 3.0", "x")!!
            assertEquals(1.0 + 3.0, c(1.0))
            assertEquals(2.0 + 3.0, c(2.0))
        }
        run {
            val c = compileFunction1OrNull<Vector2, Double>("x.x + x.y", "x")!!
            assertEquals(1.0 + 3.0, c(Vector2(1.0, 3.0)))
            assertEquals(2.0 + 3.0, c(Vector2(2.0, 3.0)))
        }
        run {
            val c = compileFunction1OrNull<Vector2, Double>("x.x + x.y", "x")!!
            val start = System.currentTimeMillis()
            for (i in 0 until 1000) {
                val r0 = Double.uniform(0.0, 1.0)
                val r1 = Double.uniform(0.0, 1.0)
                assertEquals(r0 + r1, c(Vector2(r0, r1)))
            }
            val end = System.currentTimeMillis()
            println("that took ${end - start}")
        }
    }

    @Test
    fun testFunction2() {
        run {
            val c = compileFunction1OrNull<Map<String, Any>, Double>("x.x + 3.0", "x")!!
            assertEquals(1.0 + 3.0, c(mapOf("x" to 1.0)))
            //assertEquals(2.0 + 3.0, c(mapOf("x" to 2.0))
        }
    }

    @Test
    fun testDynamicConstants() {

        val env = { n: String ->
            when (n) {
                "a" -> { nn: String ->
                    when (nn) {
                        "a" -> { nnn: String ->
                            when (nnn) {
                                "b" -> 7.0
                                "c" -> { x: Double -> x + 1.0 }
                                else -> null
                            }
                        }

                        "b" -> 5.0
                        "c" -> { x: Double -> x * 2.0 }
                        else -> null
                    }

                }
                "c" -> { x: Double -> x * 3.0 }
                else -> null
            }
        }

        val c0 = compileFunction1OrNull<Map<String, Any>, Double>("a.a.c(2.0)", "x", constants = env)!!
        val r0 = c0(emptyMap())
        assertEquals(3.0, r0)

        val c1 = compileFunction1OrNull<Map<String, Any>, Double>("a.c(2.0)", "x", constants = env)!!
        val r1 = c1(emptyMap())
        assertEquals(4.0, r1)

        val c2 = compileFunction1OrNull<Map<String, Any>, Double>("c(2.0)", "x", constants = env)!!
        val r2 = c2(emptyMap())
        assertEquals(6.0, r2)

        val c3 = compileFunction1OrNull<Map<String, Any>, Double>("cos(2.0)", "x", constants = env)!!
        val r3 = c3(emptyMap())


    }
}