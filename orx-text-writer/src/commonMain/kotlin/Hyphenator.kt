package org.openrndr.extra.textwriter

import org.openrndr.draw.font.internal.Script
import org.openrndr.extra.textwriter.hyphenation.HyphenationPatternSet
import org.openrndr.extra.textwriter.hyphenation.hyphenationPatternSets

/**
 * A hyphenator based on Frank Liang's pattern algorithm as used by TeX, with the patterns of hyph-utf8.
 *
 * Use [Hyphenator.forLanguage] to find a hyphenator for a language, or the top level [hyphenate] function.
 *
 * @param patternSet the patterns to use
 * @param leftMin the minimum number of characters before a hyphenation point
 * @param rightMin the minimum number of characters after a hyphenation point
 */
class Hyphenator(
    val patternSet: HyphenationPatternSet,
    val leftMin: Int = patternSet.leftMin,
    val rightMin: Int = patternSet.rightMin
) {
    private val patterns = HashMap<String, ByteArray>()
    private var maxPatternLength = 0
    private val exceptions = HashMap<String, IntArray>()
    private val alphabet = HashSet<Char>()

    init {
        val data = patternSet.data()
        for (pattern in data.patterns.split(' ')) {
            if (pattern.isEmpty()) continue
            val letters = StringBuilder()
            val values = ByteArray(pattern.length + 1)
            for (c in pattern) {
                if (c in '0'..'9') {
                    values[letters.length] = (c - '0').toByte()
                } else {
                    letters.append(c)
                    if (c != '.') alphabet.add(c)
                }
            }
            patterns[letters.toString()] = values.copyOf(letters.length + 1)
            maxPatternLength = maxOf(maxPatternLength, letters.length)
        }
        for (exception in data.exceptions.split(' ')) {
            if (exception.isEmpty()) continue
            val word = StringBuilder()
            val points = mutableListOf<Int>()
            for (c in exception) {
                if (c == '-') points.add(word.length) else word.append(c.lowercaseChar())
            }
            exceptions[word.toString()] = points.toIntArray()
        }
    }

    /**
     * Returns the hyphenation points of [word]: the UTF-16 indices of the characters a hyphen may be inserted before.
     */
    fun hyphenationPoints(word: String): List<Int> {
        if (word.length < leftMin + rightMin || SOFT_HYPHEN in word) return emptyList()
        val lower = buildString(word.length) { for (c in word) append(c.lowercaseChar()) }

        exceptions[lower]?.let { points -> return points.filter { isValidPoint(word, it) } }

        val text = ".$lower."
        val values = IntArray(text.length + 1)
        for (start in text.indices) {
            for (end in start + 1..minOf(text.length, start + maxPatternLength)) {
                val pattern = patterns[text.substring(start, end)] ?: continue
                for (k in pattern.indices) {
                    if (pattern[k] > values[start + k]) values[start + k] = pattern[k].toInt()
                }
            }
        }

        // values[p + 1] is the value before character p of the word, the text starts with '.'
        return (leftMin..word.length - rightMin).filter { p -> values[p + 1] % 2 == 1 && isValidPoint(word, p) }
    }

    private fun isValidPoint(word: String, index: Int): Boolean {
        if (index <= 0 || index >= word.length) return false
        val c = word[index]
        return !c.isLowSurrogate() && !c.isMark() && c != ZERO_WIDTH_JOINER && c != ZERO_WIDTH_NON_JOINER &&
                word[index - 1] != ZERO_WIDTH_JOINER
    }

    /**
     * Returns [word] with [hyphen] inserted at its hyphenation points.
     */
    fun hyphenateWord(word: String, hyphen: Char = SOFT_HYPHEN): String {
        val points = hyphenationPoints(word)
        if (points.isEmpty()) return word
        return buildString(word.length + points.size) {
            var last = 0
            for (point in points) {
                append(word, last, point)
                append(hyphen)
                last = point
            }
            append(word, last, word.length)
        }
    }

    /**
     * Returns [text] with [hyphen] inserted at the hyphenation points of every word. Words are maximal sequences of
     * letters and marks, words that already contain a soft hyphen are left as they are.
     */
    fun hyphenate(text: String, hyphen: Char = SOFT_HYPHEN): String {
        val points = textHyphenationPoints(text)
        if (points.isEmpty()) return text
        return buildString(text.length + points.size) {
            var last = 0
            for (point in points) {
                append(text, last, point)
                append(hyphen)
                last = point
            }
            append(text, last, text.length)
        }
    }

    /**
     * Returns the hyphenation points of all words in [text]: the UTF-16 indices of the characters a hyphen may be
     * inserted before. Words are maximal sequences of letters and marks, words that already contain a soft hyphen
     * have no hyphenation points.
     */
    fun textHyphenationPoints(text: String): List<Int> {
        val points = mutableListOf<Int>()
        var i = 0
        while (i < text.length) {
            if (!isWordChar(text[i])) {
                i++
                continue
            }
            var j = i
            while (j < text.length && isWordChar(text[j])) j++
            for (point in hyphenationPoints(text.substring(i, j))) points.add(i + point)
            i = j
        }
        return points
    }

    // soft hyphens are part of a word so that words that already contain them are left as they are
    private fun isWordChar(c: Char) = c.isLetter() || c.isMark() || c.isSurrogate() || c in alphabet ||
            c == ZERO_WIDTH_JOINER || c == ZERO_WIDTH_NON_JOINER || c == SOFT_HYPHEN

    companion object {
        /** The soft hyphen (U+00AD) that marks a possible hyphenation point */
        const val SOFT_HYPHEN = '\u00AD'
        private const val ZERO_WIDTH_JOINER = '‍'
        private const val ZERO_WIDTH_NON_JOINER = '‌'

        // Language tags that map to a pattern set with a different tag
        private val aliases = mapOf(
            "en" to "en-us", "en-au" to "en-gb", "en-nz" to "en-gb", "en-ie" to "en-gb", "en-za" to "en-gb",
            "en-in" to "en-gb", "de" to "de-1996", "de-de" to "de-1996", "de-at" to "de-1996",
            "de-ch" to "de-1996", "de-li" to "de-1996", "de-lu" to "de-1996", "el" to "el-monoton",
            "no" to "nb", "mn" to "mn-cyrl", "zh-latn" to "zh-latn-pinyin", "am" to "mul-ethi", "ti" to "mul-ethi",
            "gez" to "mul-ethi", "tig" to "mul-ethi", "byn" to "mul-ethi",
            "sh" to "sh-latn", "sr" to "sh-cyrl", "sr-cyrl" to "sh-cyrl", "sr-latn" to "sh-latn", "bs" to "sh-latn",
            "bs-latn" to "sh-latn", "bs-cyrl" to "sh-cyrl"
        )

        private val hyphenators = HashMap<HyphenationPatternSet, Hyphenator>()
        private val resolved = HashMap<Pair<String, Script?>, Hyphenator?>()

        /**
         * Finds the pattern set for a [language] and [script].
         *
         * @param language a language tag like `en`, `en-GB`, `de-CH`, `sr_Latn` or a hyph-utf8 tag like `de-1901`,
         * region subtags that have no pattern set of their own fall back to the language
         * @param script the script of the text, selects between pattern sets for the same language. When null the
         * script subtag of [language] is used, if any
         * @return the pattern set or null when no pattern set matches
         */
        fun findPatternSet(language: String, script: Script? = null): HyphenationPatternSet? {
            val tag = language.trim().replace('_', '-').lowercase()
            val subtags = tag.split('-').filter { it.isNotEmpty() }
            if (subtags.isEmpty()) return null

            val tagScript = subtags.getOrNull(1)?.takeIf { it.length == 4 }?.let { subtag ->
                Script.entries.firstOrNull { it.tag.equals(subtag, ignoreCase = true) }
            }
            val effectiveScript = script ?: tagScript

            val candidates = mutableListOf<String>()
            for (count in subtags.size downTo 1) {
                val prefix = subtags.take(count).joinToString("-")
                candidates.add(prefix)
                aliases[prefix]?.let { candidates.add(it) }
                if (count == 1 && effectiveScript != null) {
                    val withScript = "$prefix-${effectiveScript.tag.lowercase()}"
                    candidates.add(withScript)
                    aliases[withScript]?.let { candidates.add(it) }
                }
            }

            val sets = hyphenationPatternSets.associateBy { it.tag }
            return candidates.asSequence().mapNotNull { sets[it] }.firstOrNull { set ->
                effectiveScript == null || set.scripts == null || effectiveScript in set.scripts
            }
        }

        /**
         * Returns a hyphenator for [language] and [script], see [findPatternSet]. Hyphenators are cached.
         * @return the hyphenator or null when there are no patterns for the language
         */
        fun forLanguage(language: String, script: Script? = null): Hyphenator? {
            val key = language to script
            if (key in resolved) return resolved[key]
            val hyphenator = findPatternSet(language, script)?.let { set -> hyphenators.getOrPut(set) { Hyphenator(set) } }
            resolved[key] = hyphenator
            return hyphenator
        }
    }
}

private fun Char.isMark() = when (category) {
    CharCategory.NON_SPACING_MARK, CharCategory.COMBINING_SPACING_MARK, CharCategory.ENCLOSING_MARK -> true
    else -> false
}

/**
 * Returns [text] with soft hyphens (U+00AD) inserted at the hyphenation points of every word, using the
 * Knuth-Liang patterns for [language] and [script]. Returns [text] unchanged when there are no patterns.
 *
 * @param language a language tag like `en`, `en-GB`, `nl` or `de-1901`, see [Hyphenator.findPatternSet]
 * @param script the script of the text, selects between pattern sets for the same language
 */
fun hyphenate(text: String, language: String, script: Script? = null): String =
    Hyphenator.forLanguage(language, script)?.hyphenate(text) ?: text
