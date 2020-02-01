@file:JvmName("Dummy")
@file:ShaderPhrases

package org.openrndr.extra.shaderphrases.phrases
import org.openrndr.extra.shaderphrases.annotations.ShaderPhrases
import org.openrndr.extra.shaderphrases.preprocessShader

const val phraseDummy = """
float dummy() {
    return 0.0;    
}
"""

fun main() {
    val c = Class.forName("org.openrndr.extra.shaderphrases.phrases.Dummy")

    if (c.annotations.any { it.annotationClass == ShaderPhrases::class }) {
        println(c.getDeclaredField("phraseDummy").get(null))
    }
    println(preprocessShader("import org.openrndr.extra.shaderphrases.phrases.Dummy.*"))
}