package org.openrndr.extra.composition

import org.openrndr.extra.textwriter.Fonts

/**
 * A font face that text in a group can use, like a @font-face rule of CSS, see
 * [org.openrndr.extra.textwriter.FontFace]
 */
typealias FontFace = org.openrndr.extra.textwriter.FontFace

/**
 * Resolves the font families of text in compositions to font files, see [fontFamily], [fontWeight] and [fontStyle].
 * Registers in and resolves with [Fonts], the fonts of all programs, which user interfaces use too.
 */
object CompositionFonts {
    /**
     * The font file used for families that are not registered, by default OPENRNDR's default font
     */
    var fallback: String?
        get() = Fonts.fallback
        set(value) {
            Fonts.fallback = value
        }

    /**
     * Registers the font file for a font family, weight and style. Families are matched without regard to case.
     */
    fun register(family: String, fileOrUrl: String, weight: Int = 400, italic: Boolean = false) =
        Fonts.register(family, fileOrUrl, weight, italic)

    /**
     * The font file for the first registered family in [families], with the requested style and the nearest weight,
     * or [fallback]
     */
    fun resolve(families: List<String>, weight: Int = 400, italic: Boolean = false): String? =
        Fonts.resolve(families, weight, italic)

    /**
     * The font files for [families], in order of preference, see [org.openrndr.extra.textwriter.FontRegistry.candidates]
     */
    fun candidates(
        families: List<String>, weight: Int = 400, italic: Boolean = false, faces: List<FontFace> = emptyList()
    ): List<String> = Fonts.candidates(families, weight, italic, faces)
}
