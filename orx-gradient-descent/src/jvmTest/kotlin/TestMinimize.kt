import io.kotest.core.spec.style.DescribeSpec
import org.openrndr.extra.gradientdescent.minimize

class TestMinimize : DescribeSpec({
    describe("a simple 1d function") {
        fun parabola(x: DoubleArray): Double {
            return (x[0] + 1) * (x[0] + 1)
        }
        it("it can be minimized") {
            val result = minimize(doubleArrayOf(10.0), f = ::parabola)
        }
    }
})