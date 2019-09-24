import org.amshove.kluent.shouldBeNear
import org.openrndr.extra.gradientdescent.minimizeModel
import org.openrndr.math.Vector2
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object TestMinimizeModel : Spek({
    describe("a model") {
        val m = object {
            var x = 0.0
            var y = 0.0
        }
        it("can be minimized") {
            minimizeModel(m) { m->
                (m.x - 4.0) * (m.x - 4.0) + (m.y - 3.0) * (m.y - 3.0)
            }
            m.x.shouldBeNear(4.0, 0.01)
            m.y.shouldBeNear(3.0, 0.01)
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
            m.position.x.shouldBeNear(4.0, 0.01)
            m.position.y.shouldBeNear(3.0, 0.01)
        }
    }
})