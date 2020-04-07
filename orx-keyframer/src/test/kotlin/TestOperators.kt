import org.amshove.kluent.shouldBeNear
import org.openrndr.extra.keyframer.evaluateExpression
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object TestOperators : Spek({
    describe("an addition operation") {
        val result = evaluateExpression("1 + 2")
        result?.shouldBeNear(3.0, 10E-6)
    }
    describe("a subtraction operation") {
        val result = evaluateExpression("1 - 2")
        result?.shouldBeNear(-1.0, 10E-6)
    }
    describe("a modulus operation") {
        val result = evaluateExpression("4 % 2")
        result?.shouldBeNear(0.0, 10E-6)
    }
    describe("a multiplication operation") {
        val result = evaluateExpression("4 * 2")
        result?.shouldBeNear(8.0, 10E-6)
    }
    describe("a division operation") {
        val result = evaluateExpression("4 / 2")
        result?.shouldBeNear(2.0, 10E-6)
    }
    describe("a multiplication/addition operation") {
        val result = evaluateExpression("4 * 2 + 1")
        result?.shouldBeNear(9.0, 10E-6)
    }
    describe("an addition/multiplication") {
        val result = evaluateExpression("4 + 2 * 3")
        result?.shouldBeNear(10.0, 10E-6)
    }
})
