import org.amshove.kluent.*
import org.openrndr.extra.glslify.preprocessGlslify
import org.openrndr.extra.glslify.preprocessGlslifyFromUrl
import org.openrndr.resourceUrl
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

const val glslifyPath = "src/test/resources/glslify"

object TestGlslify : Spek({
    describe("glslify") {
        describe("should download shader with full path") {
            val shader = """#version 330
#pragma glslify: perlin = require(glsl-noise/classic/3d)
"""
            val processed = preprocessGlslify(shader, glslifyPath = glslifyPath)

            processed `should contain` "float perlin(vec3 P)"
        }

        describe("should download shader with just the module's name") {
            val shader = """#version 330
#pragma glslify: luma = require(glsl-luma)
"""
            val processed = preprocessGlslify(shader, glslifyPath = glslifyPath)

            processed `should contain` "float luma(vec4 color)"
        }

        describe("throws exception when the import doesn't lead to a shader file") {
            invoking {
                val shader = """#version 330
#pragma glslify: perlin = require(glsl-noise)
"""
                preprocessGlslify(shader, glslifyPath = glslifyPath)
            } `should throw` RuntimeException::class `with message`
                    ("[glslify] glsl-noise: index doesn't lead to any shader file")
        }
    }

    describe("preprocessGlslify") {
        describe("should import everything in order + write naming") {
            val url = resourceUrl("/a.glsl")
            val processed = preprocessGlslifyFromUrl(url, glslifyPath = glslifyPath)

            processed shouldContainAll listOf(
                "float add(float a, float b)",
                "float multiply(float a, float b)",
                "float equation(float a, float b)",
                "float luminance(vec3 color)",
                "void main()"
            )
        }

        describe("should import complex") {
            val url = resourceUrl("/complex.glsl")
            val processed = preprocessGlslifyFromUrl(url, glslifyPath = glslifyPath)

            processed shouldContainAll listOf(
                    "float checker(vec2 uv, float repeats)",
                    "float noise3d(vec3 P)",
                    "float easing(float t)",
                    "void main()"
            )
        }

        describe("should import only once") {
            val shader = """#version 330
#pragma glslify: luma = require(glsl-luma)
#pragma glslify: luma = require(glsl-luma)
#pragma glslify: luma = require(glsl-luma)
"""
            val processed = preprocessGlslify(shader, glslifyPath = glslifyPath).trimEnd()

            processed shouldBeEqualTo """#version 330
float luma(vec3 color) {
  return dot(color, vec3(0.299, 0.587, 0.114));
}

float luma(vec4 color) {
  return dot(color.rgb, vec3(0.299, 0.587, 0.114));
}"""
        }
    }
})