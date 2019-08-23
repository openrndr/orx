package org.openrndr.extra.shaderphrases

import org.openrndr.extra.shaderphrases.annotations.ShaderPhrases

fun preprocessShader(shader: String): String {
    val lines = shader.split("\n")
    val processed = lines.map {
        if (it.startsWith("import ")) {
            val tokens = it.split(" ")
            val full = tokens[1]
            val fullTokens = full.split(".")
            val fieldName = fullTokens.last().replace(";","")
            val packageClassTokens = fullTokens.dropLast(1)
            val packageClass = packageClassTokens.joinToString(".")

            val c = Class.forName(packageClass)
            if (c.annotations.any { it.annotationClass == ShaderPhrases::class }) {
                if (fieldName == "*") {
                    c.declaredFields.filter { println(it.type); it.type.name =="java.lang.String" }.map {
                        it.get(null)
                    }.joinToString("\n")
                } else {
                    c.getDeclaredField(fieldName).get(null)
                }
            } else {
                throw IllegalArgumentException("class $packageClass has no ShaderPhrases annotation")
            }
        } else {
            it
        }
    }
    return processed.joinToString("\n")
}