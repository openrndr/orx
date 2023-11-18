import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.equals.shouldBeEqual
import org.openrndr.extra.shaderphrases.ShaderPhrase
import org.openrndr.extra.shaderphrases.ShaderPhraseBook
import org.openrndr.extra.shaderphrases.ShaderPhraseRegistry

class TestShaderPhraseBook : DescribeSpec({
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
            )!!.shouldBeEqual(book.phrase)
        }
    }
})