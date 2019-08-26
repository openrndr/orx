import org.openrndr.extra.shaderphrases.preprocessShader
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object TestPreprocessShader:Spek({

    describe("A shader with import statements") {
        val shader = """
#version 330
import org.openrndr.extra.shaderphrases.phrases.Dummy.*


""".trimIndent()
        describe("when preprocessed") {
            val processed = preprocessShader(shader)
            println(processed)
        }
    }
})