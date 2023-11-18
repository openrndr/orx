import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.equals.shouldBeEqual
import org.openrndr.extra.olive.loadFromScriptContentsKSH

class TestLoadScriptKSH : DescribeSpec({

    describe("some script") {
        val number = loadFromScriptContentsKSH<Int>("5")

        it("should evaluate properly") {
            number shouldBeEqual 5
        }
    }
})