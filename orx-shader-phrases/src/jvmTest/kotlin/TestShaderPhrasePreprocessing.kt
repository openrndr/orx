import org.amshove.kluent.`should contain`
import org.amshove.kluent.`should not contain`
import org.openrndr.extra.shaderphrases.ShaderPhrase
import org.openrndr.extra.shaderphrases.ShaderPhraseBook
import org.openrndr.extra.shaderphrases.preprocessShader
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

class TestShaderPhrasePreprocessing : Spek({
    describe("glsl") {
        val book = object : ShaderPhraseBook("testBook") {
            val testPhrase = ShaderPhrase("vec4 test_phrase() { }")
        }
        book.register()

        val glsl = """
                // start
                #pragma import testBook.test_phrase
                // end
            """.trimIndent()

        val glslProcessed = preprocessShader(glsl)

        it("should not contain phrase before preprocessing") {
            glsl `should not contain` book.testPhrase.phrase
        }

        it("should contain phrase after preprocessing") {
            glslProcessed `should contain` book.testPhrase.phrase
        }
    }
})