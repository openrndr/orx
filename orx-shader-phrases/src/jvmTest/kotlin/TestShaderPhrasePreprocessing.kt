import org.amshove.kluent.`should contain`
import org.openrndr.extra.shaderphrases.ShaderPhrase
import org.openrndr.extra.shaderphrases.ShaderPhraseBook
import org.openrndr.extra.shaderphrases.preprocessShader
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

class TestShaderPhrasePreprocessing : Spek({
    describe("preprocessShader()") {
        val book = object : ShaderPhraseBook("testBook") {
            val testPhrase = ShaderPhrase("vec4 test_phrase() { }")
        }
        it("should replace #pragma with phrase") {
            book.register()
            preprocessShader(
                """
                // start
                #pragma import testBook.test_phrase
                // end
            """.trimIndent()
            ) `should contain` book.testPhrase.phrase
        }
    }
})