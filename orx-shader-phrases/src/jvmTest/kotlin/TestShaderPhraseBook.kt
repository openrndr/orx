import org.amshove.kluent.`should be`
import org.openrndr.extra.shaderphrases.ShaderPhrase
import org.openrndr.extra.shaderphrases.ShaderPhraseBook
import org.openrndr.extra.shaderphrases.ShaderPhraseRegistry
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

class TestShaderPhraseBook : Spek({
    describe("A shader phrase book") {
        val book = object : ShaderPhraseBook("testBook") {
            val phrase = ShaderPhrase(
                """
            vec4 test_phrase() {
            }
        """.trimMargin()
            )
        }
        it("can be registered") {
            book.register()
        }
        it("can be found") {
            ShaderPhraseRegistry.findPhrase(
                "testBook.test_phrase"
            ) `should be` book.phrase
        }
    }
})