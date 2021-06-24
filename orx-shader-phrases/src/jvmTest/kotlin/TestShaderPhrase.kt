import org.amshove.kluent.`should be`
import org.openrndr.extra.shaderphrases.ShaderPhrase
import org.openrndr.extra.shaderphrases.ShaderPhraseRegistry
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object TestShaderPhrase : Spek({
    describe("A shader phrase") {
        val phrase = ShaderPhrase("""
            |vec4 test_phrase() {
            |}
        """.trimMargin() )
        it("can be registered") {
            phrase.register()
        }
        it("can be found") {
            ShaderPhraseRegistry.findPhrase("test_phrase") `should be` phrase
        }
    }
})
