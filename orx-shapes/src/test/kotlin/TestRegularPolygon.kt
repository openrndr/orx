import org.amshove.kluent.`should be equal to`
import org.openrndr.extra.shapes.regularPolygon
import org.openrndr.extra.shapes.regularPolygonBeveled
import org.openrndr.extra.shapes.regularPolygonRounded
import org.openrndr.shape.Winding
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object TestRegularPolygon : Spek({


    describe("a regular polygon with 3 sides") {
        val rp = regularPolygon(3)

        it("is closed") {
            rp.closed `should be equal to` true
        }

        it("has clockwise winding") {
            rp.winding `should be equal to` Winding.CLOCKWISE
        }
    }

    describe("a regular polygon with rounded corners and 3 sides") {
        val rp = regularPolygonRounded(3)

        it("is closed") {
            rp.closed `should be equal to` true
        }

        it("has clockwise winding") {
            rp.winding `should be equal to` Winding.CLOCKWISE
        }
    }

    describe("a regular polygon with beveled corners and 3 sides") {
        val rp = regularPolygonBeveled(3)

        it("is closed") {
            rp.closed `should be equal to` true
        }

        it("has clockwise winding") {
            rp.winding `should be equal to` Winding.CLOCKWISE
        }
    }


})