import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import org.openrndr.extra.gradientdescent.minimizeModel
import org.openrndr.math.Vector2

class TestMinimizeModel : DescribeSpec({
    describe("a model") {
        val m = object {
            var x = 0.0
            var y = 0.0
        }
        it("can be minimized") {
            minimizeModel(m) { m->
                (m.x - 4.0) * (m.x - 4.0) + (m.y - 3.0) * (m.y - 3.0)
            }
            m.x.shouldBe(4.0.plusOrMinus(0.01))
            m.y.shouldBe(3.0.plusOrMinus(0.01))
        }
    }

    describe("a model with a Vector2 property") {
        val m = object {
            var position = Vector2.ZERO
        }
        it("can be minimized") {
            minimizeModel(m) { m->
                (m.position.x - 4.0) * (m.position.x - 4.0) + (m.position.y - 3.0) * (m.position.y - 3.0)
            }
            m.position.x.shouldBe(4.0.plusOrMinus(0.01))
            m.position.y.shouldBe(3.0.plusOrMinus(0.01))
        }
    }
})