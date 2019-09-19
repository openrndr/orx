import org.amshove.kluent.`should be equal to`
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object TestDot : Spek({
    describe("some vectors") {
        val a = doubleArrayOf(10.0)
        val b = doubleArrayOf(4.0)

        dot(a,b) `should be equal to` 40.0

    }
    describe("a matrix and a vector") {
        val a = arrayOf(doubleArrayOf(10.0))
        val b = doubleArrayOf(1.0)

        val d = dot(a,b)
        d[0] `should be equal to` 10.0

    }
    describe("a matrix and a vector") {
        val a = arrayOf(doubleArrayOf(1.0))
        val b = doubleArrayOf(19.99999999995339)

        val d = dot(a,b)
        d[0] `should be equal to` 19.99999999995339

    }

})