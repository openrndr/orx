import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should equal`
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object TestGradient : Spek({

    describe("a simple 1d function") {
        fun parabola(x: DoubleArray): Double {
            return x[0] * x[0]
        }
        it("its gradient at x=0 is 0.0") {
            val g0 = gradient(doubleArrayOf(0.0), ::parabola)
            g0.size `should equal` 1
            g0[0] `should be equal to` 0.0
        }
        it("its gradient at x=1 is ~2.0") {
            val g1 = gradient(doubleArrayOf(1.0), ::parabola)
        }
        it("its gradient at x=-1 is ~-2.0") {
            val g1 = gradient(doubleArrayOf(-1.0), ::parabola)
        }
    }

    describe("a simple 2d function") {
        fun parabola(x: DoubleArray): Double {
            return x[0] * x[0] + x[1] * x[1]
        }

        it("its gradient at x=0 is 0.0") {
            val g0 = gradient(doubleArrayOf(0.0, 0.0), ::parabola)
            g0.size `should equal` 2
            g0[0] `should be equal to` 0.0
        }

        it("its gradient at x=1 is ~2.0") {
            val g1 = gradient(doubleArrayOf(1.0, 1.0), ::parabola)
        }
        it("its gradient at x=-1 is ~-2.0") {
            val g1 = gradient(doubleArrayOf(-1.0, -1.0), ::parabola)
        }
    }
})