package org.openrndr.extra.textwriter.hyphenation

import org.openrndr.draw.font.internal.Script

/**
 * The data of a hyphenation pattern set, generated from hyph-utf8.
 */
internal interface PatternData {
    /** Liang patterns separated by spaces, for example `.ach4 a1ch` */
    val patterns: String

    /** Hyphenation exceptions separated by spaces, hyphens mark the hyphenation points, for example `as-so-ciate` */
    val exceptions: String

    /** The original header of the pattern file: title, copyright, licence and authors */
    val notice: String
}

/**
 * A set of hyphenation patterns for a language, see [hyphenationPatternSets] for the available sets.
 *
 * @property tag the hyph-utf8 language tag, for example `en-us`, `de-1996` or `mn-cyrl`
 * @property title a description of the pattern set
 * @property leftMin the minimum number of characters before a hyphenation point
 * @property rightMin the minimum number of characters after a hyphenation point
 * @property scripts the scripts the patterns are written in, null when the patterns cover multiple scripts
 */
class HyphenationPatternSet internal constructor(
    val tag: String,
    val title: String,
    val leftMin: Int,
    val rightMin: Int,
    val scripts: Set<Script>?,
    internal val data: () -> PatternData
) {
    /**
     * The copyright and licence notice of the patterns.
     */
    val notice: String get() = data().notice

    override fun toString() = "HyphenationPatternSet($tag)"
}
