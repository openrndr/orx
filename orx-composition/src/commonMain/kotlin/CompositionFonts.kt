package org.openrndr.extra.composition

import kotlin.math.abs

/**
 * Resolves the font families of text in compositions to font files, see [fontFamily], [fontWeight] and [fontStyle].
 */
object CompositionFonts {
    private class Registration(val weight: Int, val italic: Boolean, val fileOrUrl: String)

    private val families = mutableMapOf<String, MutableList<Registration>>()

    /**
     * The font file used for families that are not registered, by default OPENRNDR's default font
     */
    var fallback: String? = "data/fonts/default.otf"

    /**
     * Registers the font file for a font family, weight and style. Families are matched without regard to case.
     */
    fun register(family: String, fileOrUrl: String, weight: Int = 400, italic: Boolean = false) {
        families.getOrPut(family.lowercase()) { mutableListOf() }.add(Registration(weight, italic, fileOrUrl))
    }

    /**
     * The font file for the first registered family in [families], with the requested style and the nearest weight,
     * or [fallback]
     */
    fun resolve(families: List<String>, weight: Int = 400, italic: Boolean = false): String? {
        for (family in families) {
            val registrations = this.families[family.trim().lowercase()] ?: continue
            val styled = registrations.filter { it.italic == italic }.ifEmpty { registrations }
            return styled.minBy { abs(it.weight - weight) }.fileOrUrl
        }
        return fallback
    }
}
