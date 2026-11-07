package org.openrndr.panel

import org.openrndr.Program
import org.openrndr.draw.FontImageMap
import org.openrndr.draw.loadFont
import org.openrndr.extra.textwriter.FontRegistry
import org.openrndr.extra.textwriter.Fonts
import org.openrndr.panel.style.FontStyle
import org.openrndr.panel.style.LinearDimension
import org.openrndr.panel.style.StyleSheet
import org.openrndr.panel.style.fontFamily
import org.openrndr.panel.style.fontSize
import org.openrndr.panel.style.fontStyle
import org.openrndr.panel.style.fontWeight

/**
 * The fonts of a control manager: the families registered here, then those of [Fonts], the fonts of all programs,
 * which compositions and SVG documents use too. Families are resolved by weight and style, as in CSS.
 */
class FontManager() {
    var program: Program? = null

    private val names = mutableMapOf<String, String>()

    /** the files of the regular faces of the families registered here by their names, putting one registers it */
    val registry: MutableMap<String, String> = object : AbstractMutableMap<String, String>() {
        override val entries get() = names.entries
        override fun put(key: String, value: String): String? = names[key].also { register(key, value) }
    }

    /** the families of the control manager, falling back on [Fonts] */
    val fonts = FontRegistry(parent = Fonts)

    var contentScale: Double = 1.0

    /** the font file registered for the family [name], or of the first registered family of a list of families */
    fun resolve(name: String): String? = families(name).takeIf { list -> list.any { fonts.isRegistered(it) } }
        ?.let { fonts.resolve(it) }

    /** the font file of the font family, weight and style of [style] */
    fun resolve(style: StyleSheet): String? =
        fonts.resolve(families(style.fontFamily) + "default", style.fontWeight, style.fontStyle == FontStyle.ITALIC)

    fun font(cs: StyleSheet): FontImageMap {
        val fontUrl = resolve(cs) ?: "cp:fonts/Roboto-Medium.ttf"
        val fontSize = (cs.fontSize as? LinearDimension.PX)?.value ?: 16.0
        return program?.loadFont(fontUrl, fontSize) ?: error("no program")
    }

    /** registers the font file [url] for the family [name], with [weight] and as italic when [italic] is set */
    fun register(name: String, url: String, weight: Int = 400, italic: Boolean = false) {
        if (weight == 400 && !italic) names[name] = url
        fonts.register(name, url, weight, italic)
    }

    private fun families(value: String) = value.split(',').map { it.trim().trim('"', '\'') }.filter { it.isNotEmpty() }
}
