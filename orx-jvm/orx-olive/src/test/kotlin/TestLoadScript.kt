import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.equals.shouldBeEqual
import org.openrndr.extra.olive.ScriptObjectLoader

class TestLoadScript : DescribeSpec({

    describe("some script") {
        val loader = ScriptObjectLoader()

        val number = loader.load<Int>("5")

        it("should evaluate properly") {
            number shouldBeEqual 5
        }
    }
})