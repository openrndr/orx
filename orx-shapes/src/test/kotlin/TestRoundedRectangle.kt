import org.amshove.kluent.`should be equal to`
import org.openrndr.extra.shapes.*
import org.openrndr.shape.Winding
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object TestRoundedRectangle : Spek({
    describe("a rounded square") {
        val rs = RoundedRectangle(100.0, 100.0, 200.0, 200.0, 20.0).contour

        it("is closed") {
            rs.closed `should be equal to` true
        }

        it("has clockwise winding") {
            rs.winding `should be equal to` Winding.CLOCKWISE
        }
    }

})