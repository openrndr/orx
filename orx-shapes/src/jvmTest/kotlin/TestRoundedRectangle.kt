import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.equals.shouldBeEqual
import org.openrndr.extra.shapes.*
import org.openrndr.shape.Winding

class TestRoundedRectangle : DescribeSpec({
    describe("a rounded square") {
        val rs = RoundedRectangle(100.0, 100.0, 200.0, 200.0, 20.0).contour

        it("is closed") {
            rs.closed.shouldBeTrue()
        }

        it("has clockwise winding") {
            rs.winding.shouldBeEqual(Winding.CLOCKWISE)
        }
    }
})