import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object TestMinimize : Spek({

    describe("a simple 1d function") {
        fun parabola(x: DoubleArray): Double {
            return (x[0]+1) * (x[0]+1)
        }

        it("it can be minimized") {
            val result = minimize(doubleArrayOf(10.0), f = ::parabola)
            println(result.solution[0])
        }

    }

})