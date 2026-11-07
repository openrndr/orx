package org.openrndr.extra.textwriter

import io.github.oshai.kotlinlogging.KotlinLogging
import org.openrndr.draw.font.Face
import org.openrndr.draw.font.loadFace
import kotlin.math.abs
import kotlin.math.min

private val logger = KotlinLogging.logger {}

/**
 * A font face that text can use, like a @font-face rule of CSS: the font of [family] with [weight] and [italic] is
 * loaded from the first of [sources] that can be loaded, a file, a URL or a data URL. A source `local(name)` refers
 * to a family registered in a [FontRegistry].
 */
class FontFace(
    val family: String,
    val sources: List<String>,
    val weight: IntRange = 400..400,
    val italic: Boolean = false
)

/**
 * Resolves font families to font files, by family, weight and style as in CSS. Families are matched without regard to
 * case. A registry with a [parent] resolves the families it does not know in its parent.
 */
open class FontRegistry(val parent: FontRegistry? = null) {
    private class Registration(val weight: Int, val italic: Boolean, val fileOrUrl: String)

    private val families = mutableMapOf<String, MutableList<Registration>>()

    /**
     * The font file used for families that are not registered, the fallback of the [parent] when null
     */
    var fallback: String? = null

    private val effectiveFallback: String? get() = fallback ?: parent?.effectiveFallback

    /**
     * Registers the font file for a font family, weight and style. A later registration of the same family, weight
     * and style replaces it.
     */
    fun register(family: String, fileOrUrl: String, weight: Int = 400, italic: Boolean = false) {
        val registrations = families.getOrPut(family.lowercase()) { mutableListOf() }
        registrations.removeAll { it.weight == weight && it.italic == italic }
        registrations.add(Registration(weight, italic, fileOrUrl))
    }

    /** true when [family] is registered here or in the parent */
    fun isRegistered(family: String): Boolean =
        family.trim().lowercase() in families || parent?.isRegistered(family) == true

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
            val name = family.trim().trim('"', '\'').lowercase()
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
        return (candidates + listOfNotNull(effectiveFallback)).distinct()
    }

    /** the registered file of [family] with the requested style and the nearest weight, here or in the parent */
    private fun registered(family: String, weight: Int, italic: Boolean): String? {
        val registrations = this.families[family.lowercase()] ?: return parent?.registered(family, weight, italic)
        val styled = registrations.filter { it.italic == italic }.ifEmpty { registrations }
        return styled.minBy { abs(it.weight - weight) }.fileOrUrl
    }
}

/**
 * The fonts registered for all programs, which the registries of compositions and of user interfaces fall back on
 */
object Fonts : FontRegistry() {
    init {
        fallback = "data/fonts/default.otf"
    }
}

/**
 * Faces loaded once for every font file, size and variation: the same font at the same size is the same [Face], so
 * that caches keyed by faces, like those of shaped text and of glyphs, are shared by everything that draws text.
 */
object FontFaces {
    private data class Key(val file: String, val size: Double, val weight: Int, val opticalSize: Double?, val settings: Map<String, Double>)

    private val faces = HashMap<Pair<String, Double>, Face>()
    private val variations = HashMap<Key, Face>()
    private val failed = mutableSetOf<String>()

    /** the names of the registered axes of variable fonts by their tags */
    private val registeredAxes = mapOf(
        "wght" to "Weight", "wdth" to "Width", "opsz" to "OpticalSize", "ital" to "Italic", "slnt" to "Slant"
    )

    /**
     * The face of the first of [candidates] that loads, at [size]; null when none loads. Fonts that fail to load are
     * not tried again. See [face] for the variations.
     */
    fun first(
        candidates: List<String>, size: Double, weight: Int = 400, opticalSize: Double? = size,
        settings: Map<String, Double> = emptyMap()
    ): Face? {
        for (file in candidates) {
            if (file in failed) continue
            try {
                return face(file, size, weight, opticalSize, settings)
            } catch (e: Throwable) {
                failed.add(file)
                logger.warn { "could not load font '${file.take(80)}': ${e.message}" }
            }
        }
        return null
    }

    /**
     * The face of [file] at [size]. A variable font is set to the axes of [settings], given by their tags as in CSS
     * font-variation-settings; its weight axis follows [weight] and its optical size follows [opticalSize] when the
     * settings do not give them, as browsers do.
     */
    fun face(
        file: String, size: Double, weight: Int = 400, opticalSize: Double? = size,
        settings: Map<String, Double> = emptyMap()
    ): Face {
        val face = faces.getOrPut(file to size) { loadFace(file).withSize(size) }
        if (!face.isVariable) return face
        return variations.getOrPut(Key(file, size, weight, opticalSize, settings)) {
            val master = face.master.copy()
            // the axes of faces are named by their names, registered axes are given by their tags in CSS
            fun axis(tag: String) = registeredAxes[tag]?.takeIf { it in master } ?: tag.takeIf { it in master }
            val named = settings.mapNotNull { (tag, value) -> axis(tag)?.let { it to value } }.toMap()
            for ((name, value) in named) master[name] = value
            axis("wght")?.takeIf { it !in named }?.let { master[it] = weight.toDouble() }
            if (opticalSize != null) axis("opsz")?.takeIf { it !in named }?.let { master[it] = opticalSize }
            if (master == face.master) face else face.withMaster(master)
        }
    }
}
