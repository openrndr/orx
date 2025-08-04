import io.kotest.core.spec.style.DescribeSpec
import org.openrndr.extra.noise.*

class TestGradient : DescribeSpec({
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