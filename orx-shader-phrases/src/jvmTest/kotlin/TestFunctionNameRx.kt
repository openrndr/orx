import io.kotest.assertions.shouldFail
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.throwables.shouldThrowAny
import io.kotest.assertions.throwables.shouldThrowUnit
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.equals.shouldBeEqual
import org.openrndr.extra.shaderphrases.ShaderPhraseRegistry.getGLSLFunctionName

class TestFunctionNameRx : DescribeSpec({
    describe("A function name") {
        mapOf(
            "ivec4 aaa() {" to "aaa",
            "ivec3 bbb() {" to "bbb",
            "ivec2 ccc() {" to "ccc",
            "bvec4 ddd() {" to "ddd",
            "bvec3 eee() {" to "eee",
            "bvec2 fff() {" to "fff",
            "vec4 ggg() {" to "ggg",
            "vec3 hhh() {" to "hhh",
            "vec2 iii() {" to "iii",
            "mat2 ii2() {" to "ii2",
            "mat3 jjj() {" to "jjj",
            "mat4 kkk() {" to "kkk",
            "float lll() {" to "lll",
            "   float  mmm( )  { " to "mmm",
            "int nnn() {" to "nnn",
            "vec2 limit(vec2 a, float b) {" to "limit",
            """
                vec4 white() {
                    return vec4(1.0);
                }
                """.trimMargin() to "white"
        ).forEach { (goodGLSL, expectedFuncName) ->
            it("can be extracted from valid GLSL") {
                getGLSLFunctionName(goodGLSL).shouldBeEqual(
                        expectedFuncName)
            }
        }
    }

    describe("A function name") {
        listOf(
            "float int mat4",
            "float rnd {",
        ).forEach { badGLSL ->
            it("is not extracted if GLSL function not declared") {
                shouldThrowUnit<Throwable> {
                    val funcName = getGLSLFunctionName(badGLSL)
                }
            }
        }
    }

})
