import org.amshove.kluent.shouldBeNear
import org.openrndr.extra.keyframer.evaluateExpression
import kotlin.test.Test

class TestOperators {
    @Test
    fun `an addition operation`() {
        val result = evaluateExpression("1 + 2")
        result?.shouldBeNear(3.0, 10E-6)
    }

    @Test
    fun `a subtraction operation`() {
        val result = evaluateExpression("1 - 2")
        result?.shouldBeNear(-1.0, 10E-6)
    }

    @Test
    fun `a modulus operation`() {
        val result = evaluateExpression("4 % 2")
        result?.shouldBeNear(0.0, 10E-6)
    }

    @Test
    fun `a multiplication operation`() {
        val result = evaluateExpression("4 * 2")
        result?.shouldBeNear(8.0, 10E-6)
    }

    @Test
    fun `a division operation`() {
        val result = evaluateExpression("4 / 2")
        result?.shouldBeNear(2.0, 10E-6)
    }

    @Test
    fun `a multiplication and addition operation`() {
        val result = evaluateExpression("4 * 2 + 1")
        result?.shouldBeNear(9.0, 10E-6)
    }

    @Test
    fun `an addition and multiplication`() {
        val result = evaluateExpression("4 + 2 * 3")
        result?.shouldBeNear(10.0, 10E-6)
    }

    @Test
    fun `unary minus`() {
        val result = evaluateExpression("-4.0")
        result?.shouldBeNear(-4.0, 10E-6)
    }
}
