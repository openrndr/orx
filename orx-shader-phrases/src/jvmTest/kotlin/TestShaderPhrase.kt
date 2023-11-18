import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.equals.shouldBeEqual
import org.openrndr.extra.shaderphrases.ShaderPhrase
import org.openrndr.extra.shaderphrases.ShaderPhraseRegistry

class TestShaderPhrase : DescribeSpec({
    describe("A shader phrase") {
        val phrase = ShaderPhrase(
            """
            |vec4 test_phrase() {
            |}
        """.trimMargin()
        )
        it("can be registered") {
            phrase.register()
        }
        it("can be found") {
            ShaderPhraseRegistry.findPhrase("test_phrase")?.shouldBeEqual(phrase)
        }
    }
})
