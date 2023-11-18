import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.equals.shouldBeEqual
import org.openrndr.extra.shapes.regularPolygon
import org.openrndr.extra.shapes.regularPolygonBeveled
import org.openrndr.extra.shapes.regularPolygonRounded
import org.openrndr.shape.Winding

class TestRegularPolygon : DescribeSpec({
    describe("a regular polygon with 3 sides") {
        val rp = regularPolygon(3)

        it("is closed") {
            rp.closed.shouldBeTrue()
        }

        it("has clockwise winding") {
            rp.winding.shouldBeEqual(Winding.CLOCKWISE)
        }
    }

    describe("a regular polygon with rounded corners and 3 sides") {
        val rp = regularPolygonRounded(3)

        it("is closed") {
            rp.closed.shouldBeTrue()
        }

        it("has clockwise winding") {
            rp.winding.shouldBeEqual(Winding.CLOCKWISE)
        }
    }

    describe("a regular polygon with beveled corners and 3 sides") {
        val rp = regularPolygonBeveled(3)

        it("is closed") {
            rp.closed.shouldBeTrue()
        }

        it("has clockwise winding") {
            rp.winding.shouldBeEqual(Winding.CLOCKWISE)
        }
    }
})