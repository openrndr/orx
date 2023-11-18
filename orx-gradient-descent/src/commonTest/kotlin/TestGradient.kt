import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.equals.shouldBeEqual
import org.openrndr.extra.gradientdescent.gradient

class TestGradient : DescribeSpec({
    describe("a simple 1d function") {
        fun parabola(x: DoubleArray): Double {
            return x[0] * x[0]
        }
        it("its gradient at x=0 is 0.0") {
            val g0 = gradient(doubleArrayOf(0.0), ::parabola)
            g0.size.shouldBeEqual(1)
            g0[0].shouldBeEqual(0.0)
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
            g0.size.shouldBeEqual(2)
            g0[0].shouldBeEqual(0.0)
        }

        it("its gradient at x=1 is ~2.0") {
            val g1 = gradient(doubleArrayOf(1.0, 1.0), ::parabola)
        }
        it("its gradient at x=-1 is ~-2.0") {
            val g1 = gradient(doubleArrayOf(-1.0, -1.0), ::parabola)
        }
    }
})