package org.openrndr.extra.shaderphrases

import mu.KotlinLogging
import org.openrndr.draw.Shader
import org.openrndr.extra.shaderphrases.ShaderPhraseRegistry.getGLSLFunctionName
//import org.openrndr.extra.shaderphrases.phrases.phraseTbnMatrix
import org.openrndr.utils.url.textFromURL

private val logger = KotlinLogging.logger {}

/**
 * A single shader phrase.
 */
class ShaderPhrase(val phrase: String) {
    /**
     * Register this shader phrase in the [ShaderPhraseRegistry]
     * This will likely be called by [ShaderPhraseBook]
     */
    fun register(bookId: String? = null) {
        val id = getGLSLFunctionName(phrase)
        val prefix = bookId?.let { "$it." } ?: ""
        ShaderPhraseRegistry.registerPhrase("$prefix$id", this)
    }
}
/**
 * A book of shader phrases.
 */
expect open class ShaderPhraseBook(bookId: String) {
    val bookId: String
    /**
     * Registers all known shader phrases
     */
    fun register()

}

/**
 * The global, application-wide, shader phrase registry
 */
object ShaderPhraseRegistry {
    private val phrases = mutableMapOf<String, ShaderPhrase>()
    /**
     * Registers a [phrase] with [id]
     */
    fun registerPhrase(id: String, phrase: ShaderPhrase) {
        phrases[id] = phrase
    }
    /**
     * Finds a phrase for [id], returns null when no phrase found
     */
    fun findPhrase(id: String): ShaderPhrase? {
        val phrase = phrases[id]
        if (phrase == null) {
            logger.warn { "no phrase found for id: \"$id\"" }
        }
        return phrase
    }

    /**
     * Gets the first GLSL function name out of GLSL source code
     */
    fun getGLSLFunctionName(glsl: String): String {
        val functionRex =
            Regex("""\s*(float|int|[bi]?vec[234]|mat[34])\s+(\w+)\s*\(.*\).*""")
        val defs = glsl.split("\n").filter {
            functionRex.matches(it)
        }.take(1).mapNotNull {
            val m = functionRex.find(it)
            m?.groupValues?.getOrNull(2)
        }
        return defs.firstOrNull()
            ?: error("no function body found in phrase")
    }
}

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
                val registryPhrase = ShaderPhraseRegistry.findPhrase(symbol)
                registryPhrase?.let { preprocessShader(it.phrase, newSymbols) }
            } else {
                ""
            }
        } else {
            it
        }
    }
    return processed.joinToString("\n")
}

fun String.preprocess() = preprocessShader(this)

/**
 * Preprocess shader source from url
 * Looks for "#pragma import" statements and injects found phrases.
 * @param url url pointing to GLSL shader source
 * @return GLSL source code with injected shader phrases
 */
fun preprocessShaderFromUrl(url: String, symbols: Set<String> = emptySet()): String {
    return preprocessShader(textFromURL(url), symbols)
}

fun Shader.Companion.preprocessedFromUrls(
    vsUrl: String,
    tcsUrl: String? = null,
    tesUrl: String? = null,
    gsUrl: String? = null,
    fsUrl: String
): Shader {
    val vsCode = textFromURL(vsUrl).preprocess()
    val tcsCode = tcsUrl?.let { textFromURL(it) }?.preprocess()
    val tesCode = tesUrl?.let { textFromURL(it) }?.preprocess()
    val gsCode = gsUrl?.let { textFromURL(it) }?.preprocess()
    val fsCode = textFromURL(fsUrl).preprocess()
    val name = "$$vsUrl / $gsUrl / $fsUrl"
    return Shader.createFromCode(vsCode, tcsCode, tesCode, gsCode, fsCode, name)
}

