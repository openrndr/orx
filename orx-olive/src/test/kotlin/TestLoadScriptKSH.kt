import org.amshove.kluent.`should be equal to`
import org.openrndr.extra.olive.loadFromScriptContentsKSH
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object TestLoadScriptKSH : Spek({

    describe("some script") {
        val number = loadFromScriptContentsKSH<Int>("5")

        it("should evaluate properly") {
            number `should be equal to` 5
        }
    }
})