import org.openrndr.extra.noise.*
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import kotlin.test.assertEquals

object TestGradient : Spek({
    describe("Noise") {
        it("has a gradient") {
            gradient1D(::perlinLinear, 100, 0.1)
        }
    }

    describe("FBM noise func") {
        it("has a gradient") {
            val func = fbmFunc1D(::perlinLinear)
            gradient1D(func, 100, 0.1)
        }
    }

    describe("Billow noise func") {
        it("has a gradient") {
            val func = billowFunc1D(::perlinLinear)
            gradient1D(func, 100, 0.1)
        }
    }

    describe("Rigid noise func") {
        it("has a gradient") {
            val func = rigidFunc1D(::perlinLinear)
            gradient1D(func, 100, 0.1)
        }
    }
})