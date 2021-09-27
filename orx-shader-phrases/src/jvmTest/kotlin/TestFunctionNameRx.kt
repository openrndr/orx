import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.internal.assertFails
import org.openrndr.extra.shaderphrases.ShaderPhraseRegistry.getGLSLFunctionName
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object TestFunctionNameRx : Spek({
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
            "mat3 jjj() {" to "jjj",
            "mat4 kkk() {" to "kkk",
            "float lll() {" to "lll",
            "   float  mmm( )  { " to "mmm",
            "int nnn() {" to "nnn",
            """
                vec4 white() {
                    return vec4(1.0);
                }
                """.trimMargin() to "white"
        ).forEach { (goodGLSL, expectedFuncName) ->
            it("can be extracted from valid GLSL") {
                getGLSLFunctionName(goodGLSL) `should be equal to`
                        expectedFuncName
            }
        }
    }

    describe("A function name") {
        listOf(
            "float int mat4",
            "float rnd {",
        ).forEach { badGLSL ->
            it("is not extracted if GLSL function not declared") {
                assertFails {
                    val funcName = getGLSLFunctionName(badGLSL)
                }
            }
        }
    }

})
