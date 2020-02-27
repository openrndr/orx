package org.openrndr.extra.shaderphrases

import org.openrndr.draw.codeFromURL
import org.openrndr.extra.shaderphrases.annotations.ShaderPhrases

/**
 * Preprocess shader source.
 * Looks for "#pragma import" statements and injects found phrases.
 * @param source GLSL source code encoded as string
 * @return GLSL source code with injected shader phrases
 */
fun preprocessShader(source: String): String {
    val lines = source.split("\n")
    val processed = lines.mapIndexed { index, it ->
        if (it.startsWith("#pragma import")) {
            val tokens = it.split(" ")
            val full = tokens[2]
            val fullTokens = full.split(".")
            val fieldName = fullTokens.last().replace(";", "").trim()
            val packageClassTokens = fullTokens.dropLast(1)
            val packageClass = packageClassTokens.joinToString(".")

            try {
                /*  Note that JVM-style reflection is used here because of short-comings in the Kotlin reflection
                library (as of 1.3.61), most notably reflection support for file facades is missing. */
                val c = Class.forName(packageClass)
                if (c.annotations.any { it.annotationClass == ShaderPhrases::class }) {
                    if (fieldName == "*") {
                        c.declaredMethods.filter { it.returnType.name == "java.lang.String" }.map {
                            "/* imported from $packageClass.$it */\n${it.invoke(null)}\n"
                        }.joinToString("\n") +

                        c.declaredFields.filter { it.type.name == "java.lang.String" }.map {
                            "/* imported from $packageClass.$it */\n${it.get(null)}\n"
                        }.joinToString("\n")
                    } else {
                        // TODO add method based phrase resolver like in the wildcard case above.
                        try {
                            c.getDeclaredField(fieldName).get(null)
                        } catch (e: NoSuchFieldException) {
                            error("field \"$fieldName\" not found in \"#pragma import $packageClass.$fieldName\" on line ${index + 1}")
                        }
                    }
                } else {
                    throw IllegalArgumentException("class $packageClass has no ShaderPhrases annotation")
                }
            } catch (e: ClassNotFoundException) {
                error("class \"$packageClass\" not found in \"#pragma import $packageClass\" on line ${index + 1}")
            }
        } else {
            it
        }
    }
    return processed.joinToString("\n")
}

/**
 * Preprocess shader source from url
 * Looks for "#pragma import" statements and injects found phrases.
 * @param url url pointing to GLSL shader source
 * @return GLSL source code with injected shader phrases
 */
fun preprocessShaderFromUrl(url: String): String {
    return preprocessShader(codeFromURL(url))
}