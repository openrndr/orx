package org.openrndr.extra.composition

import kotlin.math.abs
import kotlin.math.min

/**
 * A font face that text in a group can use, like a @font-face rule of CSS: the font of [family] with [weight] and
 * [italic] is loaded from the first of [sources] that can be loaded, a file, a URL or a data URL. A source
 * `local(name)` refers to a family registered in [CompositionFonts].
 */
class FontFace(
    val family: String,
    val sources: List<String>,
    val weight: IntRange = 400..400,
    val italic: Boolean = false
)

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
    fun resolve(families: List<String>, weight: Int = 400, italic: Boolean = false): String? =
        candidates(families, weight, italic).firstOrNull()

    /**
     * The font files for [families], in order of preference: for every family, the sources of the matching one of
     * [faces] or the registered file, then [fallback]. Faces are matched like registrations are. A font that can not
     * be loaded is followed by the fonts of the next families, as in CSS.
     */
    fun candidates(
        families: List<String>, weight: Int = 400, italic: Boolean = false, faces: List<FontFace> = emptyList()
    ): List<String> {
        val candidates = mutableListOf<String>()
        for (family in families) {
            val name = family.trim().lowercase()
            val local = faces.filter { it.family.lowercase() == name }
            if (local.isNotEmpty()) {
                val styled = local.filter { it.italic == italic }.ifEmpty { local }
                val face = styled.minBy { f ->
                    if (weight in f.weight) 0 else min(abs(f.weight.first - weight), abs(f.weight.last - weight))
                }
                val sources = face.sources.flatMap { source ->
                    Regex("^local\\((.*)\\)$").matchEntire(source.trim())?.let { m ->
                        listOfNotNull(registered(m.groupValues[1].trim().trim('"', '\''), weight, italic))
                    } ?: listOf(source)
                }
                candidates.addAll(sources)
            } else {
                registered(name, weight, italic)?.let { candidates.add(it) }
            }
        }
        return (candidates + listOfNotNull(fallback)).distinct()
    }

    /** the registered file of [family] with the requested style and the nearest weight */
    private fun registered(family: String, weight: Int, italic: Boolean): String? {
        val registrations = this.families[family.lowercase()] ?: return null
        val styled = registrations.filter { it.italic == italic }.ifEmpty { registrations }
        return styled.minBy { abs(it.weight - weight) }.fileOrUrl
    }
}
