import org.amshove.kluent.`should be equal to`
import org.openrndr.extra.shapes.regularPolygonBeveled
import org.openrndr.extra.shapes.regularPolygonRounded
import org.openrndr.extra.shapes.regularStar
import org.openrndr.extra.shapes.regularStarRounded
import org.openrndr.shape.Winding
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object TestRegularStar : Spek({
    describe("a regular star with 5 points") {
        val rs = regularStar(5, 10.0, 20.0)

        it("is closed") {
            rs.closed `should be equal to` true
        }

        it("has clockwise winding") {
            rs.winding `should be equal to` Winding.CLOCKWISE
        }
    }

    describe("a regular star with rounded corners and 5 points") {
        val rs = regularStarRounded(5, 10.0, 20.0, 0.2, 0.2)

        it("is closed") {
            rs.closed `should be equal to` true
        }

        it("has clockwise winding") {
            rs.winding `should be equal to` Winding.CLOCKWISE
        }
    }

    describe("a regular star with beveled corners and 5 points") {
        val rs = regularPolygonBeveled(5, 0.5)

        it("is closed") {
            rs.closed `should be equal to` true
        }

        it("has clockwise winding") {
            rs.winding `should be equal to` Winding.CLOCKWISE
        }
    }

})