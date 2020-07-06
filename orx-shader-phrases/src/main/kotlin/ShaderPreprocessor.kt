package org.openrndr.extra.shaderphrases

import org.openrndr.draw.codeFromURL
import org.openrndr.extra.shaderphrases.annotations.ShaderPhrases

/**
 * Preprocess shader source.
 * Looks for "#pragma import" statements and injects found phrases.
 * @param source GLSL source code encoded as string
 * @return GLSL source code with injected shader phrases
 */
fun preprocessShader(source: String, symbols: Set<String> = emptySet()): String {
    val newSymbols = mutableSetOf<String>()
    newSymbols.addAll(symbols)

    val lines = source.split("\n")
    val processed = lines.mapIndexed { index, it ->
        if (it.startsWith("#pragma import")) {
            val tokens = it.split(" ")
            val symbol = tokens[2].trim().replace(";", "")
            val fullTokens = symbol.split(".")
            val fieldName = fullTokens.last().replace(";", "").trim()
            val packageClassTokens = fullTokens.dropLast(1)
            val packageClass = packageClassTokens.joinToString(".")

            if (symbol !in newSymbols) {
                newSymbols.add(symbol)
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
                            var result: String?
                            try {
                                val methodName = "get${fieldName.take(1).toUpperCase() + fieldName.drop(1)}"
                                result = preprocessShader(c.getMethod(methodName).invoke(null) as String, newSymbols)
                            } catch (e: NoSuchMethodException) {
                                try {
                                    result = preprocessShader(c.getDeclaredField(fieldName).get(null) as String, newSymbols)
                                } catch (e: NoSuchFieldException) {
                                    error("field \"$fieldName\" not found in \"#pragma import $packageClass.$fieldName\" on line ${index + 1}")
                                }
                            }
                            result
                        }
                    } else {
                        throw IllegalArgumentException("class $packageClass has no ShaderPhrases annotation")
                    }
                } catch (e: ClassNotFoundException) {
                    error("class \"$packageClass\" not found in \"#pragma import $packageClass\" on line ${index + 1}")
                }
            } else {
                ""
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
fun preprocessShaderFromUrl(url: String, symbols: Set<String> = emptySet()): String {
    return preprocessShader(codeFromURL(url), symbols)
}