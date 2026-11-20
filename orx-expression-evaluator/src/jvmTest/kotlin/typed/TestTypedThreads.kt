package typed

import org.openrndr.extra.expressions.typed.compileFunction1
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** evaluations of the same compiled expression on threads at the same time, on the JVM */
class TestTypedThreads {
    @Test
    fun `a compiled function is evaluated on several threads at once`() {
        val f = compileFunction1<Double, Double>("[x, x * 2].map { y -> y + x }.max()", "x")
        val pool = Executors.newFixedThreadPool(8)
        val failures = java.util.concurrent.atomic.AtomicInteger()
        repeat(8) { t ->
            pool.submit {
                repeat(2000) { i ->
                    val x = t * 1000.0 + i
                    if (f(x) != 3 * x) failures.incrementAndGet()
                }
            }
        }
        pool.shutdown()
        assertTrue(pool.awaitTermination(30, TimeUnit.SECONDS))
        assertEquals(0, failures.get())
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
}
