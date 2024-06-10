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
}