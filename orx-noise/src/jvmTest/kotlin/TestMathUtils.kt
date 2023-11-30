import io.kotest.core.spec.style.DescribeSpec
import org.openrndr.extra.noise.fastFloor
import kotlin.test.assertEquals

class TestMathUtils : DescribeSpec({
    describe("Fast floor") {
        it("it is corrrect for 0.0") {
            assertEquals(0, 0.0.fastFloor())
        }
    }
})