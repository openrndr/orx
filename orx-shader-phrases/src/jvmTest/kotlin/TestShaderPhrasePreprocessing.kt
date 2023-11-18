import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.openrndr.extra.shaderphrases.ShaderPhrase
import org.openrndr.extra.shaderphrases.ShaderPhraseBook
import org.openrndr.extra.shaderphrases.preprocessShader

class TestShaderPhrasePreprocessing : DescribeSpec({
    describe("the glsl code") {
        val book = object : ShaderPhraseBook("testBook") {
            val phrase1 = ShaderPhrase("vec3 phrase1() { }")
            val phrase2 = ShaderPhrase("vec4 phrase2() { }")
        }
        book.register()

        val glsl = """
                // test expected usage
                #pragma import testBook.phrase1
                // test odd spacing and line termination
                  #pragma  import  testBook.phrase2; 
            """.trimIndent()

        val glslProcessed = preprocessShader(glsl)

        it("should not contain phrase1 before preprocessing") {
            glsl.shouldNotContain(book.phrase1.phrase)
        }

        it("should not contain phrase2 before preprocessing") {
            glsl.shouldNotContain(book.phrase2.phrase)
        }

        it("should contain phrase1 after preprocessing") {
            glslProcessed.shouldContain(book.phrase1.phrase)
        }

        it("should contain phrase2 after preprocessing") {
            glslProcessed.shouldContain(book.phrase2.phrase)
        }
    }
})