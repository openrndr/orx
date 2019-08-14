import org.openrndr.extra.noise.fastFloor
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import kotlin.test.assertEquals

object TestMathUtils : Spek({
    describe("Fast floor") {
        it("it is corrrect for 0.0") {
            assertEquals(0, 0.0.fastFloor())
        }
    }
})