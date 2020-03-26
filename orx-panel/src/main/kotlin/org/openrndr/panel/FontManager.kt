package org.openrndr.panel

import org.openrndr.draw.FontImageMap
import org.openrndr.panel.style.LinearDimension
import org.openrndr.panel.style.StyleSheet
import org.openrndr.panel.style.fontFamily
import org.openrndr.panel.style.fontSize

class FontManager {
    val registry: MutableMap<String, String> = mutableMapOf()
    var contentScale: Double = 1.0

    fun resolve(name: String): String? = registry[name]

    fun font(cs: StyleSheet): FontImageMap {
        val fontUrl = resolve(cs.fontFamily) ?: "cp:fonts/Roboto-Medium.ttf"
        val fontSize = (cs.fontSize as? LinearDimension.PX)?.value ?: 16.0
        return FontImageMap.fromUrl(fontUrl, fontSize, contentScale)
    }

    fun register(name: String, url: String) {
        registry[name] = url
    }
}