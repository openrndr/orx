import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.equals.shouldBeEqual
import org.openrndr.extra.gradientdescent.dot


class TestDot : DescribeSpec({
    describe("some vectors") {
        val a = doubleArrayOf(10.0)
        val b = doubleArrayOf(4.0)
        dot(a, b).shouldBeEqual(40.0)
    }

    describe("a matrix and a vector") {
        val a = arrayOf(doubleArrayOf(10.0))
        val b = doubleArrayOf(1.0)
        val d = dot(a, b)
        d[0].shouldBeEqual(10.0)
    }

    describe("another matrix and a vector") {
        val a = arrayOf(doubleArrayOf(1.0))
        val b = doubleArrayOf(19.99999999995339)
        val d = dot(a, b)
        d[0].shouldBeEqual(19.99999999995339)
    }
})