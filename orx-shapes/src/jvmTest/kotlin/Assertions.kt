import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import org.openrndr.math.Vector2

infix fun Vector2.`should be near`(other: Vector2) {
    x shouldBe other.x.plusOrMinus(1E-5)
    y shouldBe other.y.plusOrMinus(1E-5)
}
