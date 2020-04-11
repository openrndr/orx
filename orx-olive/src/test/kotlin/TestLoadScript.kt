import org.amshove.kluent.`should be equal to`
import org.openrndr.extra.olive.ScriptObjectLoader
import org.openrndr.extra.olive.loadFromScript
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object TestLoadScript : Spek({

    describe("some script") {
        val loader  = ScriptObjectLoader()

        val number = loader.load<Int>("5")

        it("should evaluate properly") {
            number `should be equal to` 5
        }
    }
})