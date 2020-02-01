import org.amshove.kluent.`should contain`
import org.amshove.kluent.`should throw`
import org.amshove.kluent.`with message`
import org.amshove.kluent.invoking
import org.openrndr.extra.shaderphrases.preprocessShader
import org.openrndr.extra.shaderphrases.preprocessShaderFromUrl
import org.openrndr.resourceUrl
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object TestPreprocessShader : Spek({
    describe("An url pointing to a shader resource") {
        val url = resourceUrl("/from-url-test.frag")
        describe("results in injected dummy phrase when preprocessed") {
            val processed = preprocessShaderFromUrl(url)
            processed `should contain` "float dummy"
        }
    }

    describe("A shader with import statements") {
        val shader = """#version 330
#pragma import org.openrndr.extra.shaderphrases.phrases.Dummy.*
"""
        describe("injects dummy phrase when preprocessed") {
            val processed = preprocessShader(shader)
            processed `should contain` "float dummy"
        }
    }

    describe("A shader with non-resolvable class statements") {
        val shader = """#version 330
#pragma import invalid.Class.*
"""
        describe("throws exception when preprocessed") {
            invoking {
                preprocessShader(shader)
            } `should throw` RuntimeException::class `with message`
                    ("class \"invalid.Class\" not found in \"#pragma import invalid.Class\" on line 2")
        }
    }

    describe("A shader with non-resolvable property statements") {
        val shader = """#version 330
#pragma import org.openrndr.extra.shaderphrases.phrases.Dummy.invalid
"""
        describe("throws exception when preprocessed") {
            invoking {
                preprocessShader(shader)
            } `should throw` RuntimeException::class `with message`
                    ("field \"invalid\" not found in \"#pragma import org.openrndr.extra.shaderphrases.phrases.Dummy.invalid\" on line 2")
        }
    }
})