import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.equals.shouldBeEqual
import org.openrndr.extra.shapes.primitives.regularPolygonBeveled
import org.openrndr.extra.shapes.primitives.regularStar
import org.openrndr.extra.shapes.primitives.regularStarRounded
import org.openrndr.shape.Winding

class TestRegularStar : DescribeSpec({
    describe("the regular star with 5 points") {
        val rs = regularStar(5, 10.0, 20.0)

        it("should be closed") {
            rs.closed.shouldBeTrue()
        }

        it("should have clockwise winding") {
            rs.winding.shouldBeEqual(Winding.CLOCKWISE)
        }
    }

    describe("a regular star with rounded corners and 5 points") {
        val rs = regularStarRounded(5, 10.0, 20.0, 0.2, 0.2)

        it("is closed") {
            rs.closed.shouldBeTrue()
        }

        it("has clockwise winding") {
            rs.winding.shouldBeEqual(Winding.CLOCKWISE)
        }
    }

    describe("a regular star with beveled corners and 5 points") {
        val rs = regularPolygonBeveled(5, 0.5)

        it("is closed") {
            rs.closed.shouldBeTrue()
        }

        it("has clockwise winding") {
            rs.winding.shouldBeEqual(Winding.CLOCKWISE)
        }
    }
})