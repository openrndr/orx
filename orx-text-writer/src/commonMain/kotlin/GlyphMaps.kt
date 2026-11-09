package org.openrndr.extra.textwriter

import org.openrndr.draw.boundary.BoundaryDrawer
import org.openrndr.draw.boundary.BoundaryGlyphMap
import org.openrndr.draw.font.Face
import kotlin.time.TimeSource

/**
 * The glyph maps of boundary drawers, one for every boundary drawer: everything that draws text with the same
 * boundary drawer, like compositions and user interfaces, finds the outlines of glyphs in the same map, and uploads
 * every glyph once.
 *
 * Glyphs whose outlines keep changing, those of variable faces whose axes are animated, would make that map grow
 * without end, since every master has outlines of its own. Their glyphs go into a transient map instead, one per
 * boundary drawer as well, which is cleared as a whole once it uses more than [transientBudget] bytes. A face goes
 * there when it is marked with [setTransient], also a face that is not variable, or by itself when its font is seen
 * with more than [churnMasters] masters within [churnSeconds]. Who keeps indices of the transient map, like
 * [SlugTextDrawer], finds them again when its [BoundaryGlyphMap.generation] changed.
 */
object GlyphMaps {
    private val maps = mutableMapOf<BoundaryDrawer, BoundaryGlyphMap>()
    private val transientMaps = mutableMapOf<BoundaryDrawer, BoundaryGlyphMap>()

    /** the glyph map of [boundaryDrawer] */
    fun of(boundaryDrawer: BoundaryDrawer): BoundaryGlyphMap =
        maps.getOrPut(boundaryDrawer) { BoundaryGlyphMap(boundaryDrawer.createMap()) }

    /** the transient glyph map of [boundaryDrawer], for glyphs of faces that [isTransient] */
    fun transientOf(boundaryDrawer: BoundaryDrawer): BoundaryGlyphMap =
        transientMaps.getOrPut(boundaryDrawer) { BoundaryGlyphMap(boundaryDrawer.createMap()) }

    /** the most bytes the transient map of a boundary drawer uses before it is cleared, 8 MiB by default */
    var transientBudget: Long = 8L shl 20

    /** the number of masters of a font within [churnSeconds] above which its glyphs become transient */
    var churnMasters = 8

    /** the time over which the masters of a font are counted, see [churnMasters] */
    var churnSeconds = 1.0

    /**
     * A font across its masters: variable faces of the same font differ in their masters only. Different fonts with
     * the same axes and units per em are taken for one, which at worst makes both transient.
     */
    private data class Font(val ranges: Map<String, ClosedFloatingPointRange<Double>>, val unitsPerEm: Int)

    private fun fontOf(face: Face) = Font(face.master.ranges, face.unitsPerEm())

    /** the fonts that are transient, by [setTransient] or by their masters changing often */
    private val transientFonts = mutableSetOf<Font>()

    /** the fonts that were marked as not transient, which the heuristic leaves alone */
    private val stableFonts = mutableSetOf<Font>()

    /** the masters of every variable font that were seen, with when they were seen last */
    private val seen = mutableMapOf<Font, MutableMap<Map<String, Double>, TimeSource.Monotonic.ValueTimeMark>>()

    /**
     * Marks the font of [face], with all its masters, as one whose glyphs are transient, like a variable face whose
     * axes are animated, or as not transient when [transient] is false, which also keeps the heuristic from making
     * it transient
     */
    fun setTransient(face: Face, transient: Boolean = true) {
        val font = fontOf(face)
        if (transient) {
            transientFonts.add(font); stableFonts.remove(font)
        } else {
            transientFonts.remove(font); stableFonts.add(font)
        }
    }

    /** true when the glyphs of [face] go into the transient map */
    fun isTransient(face: Face): Boolean = fontOf(face) in transientFonts

    /**
     * Notes that glyphs of [face] are about to be used, for the heuristic that makes fonts whose masters change
     * often transient; gives true when the glyphs of [face] go into the transient map
     */
    fun use(face: Face): Boolean {
        val font = fontOf(face)
        if (font in transientFonts) return true
        // only the masters of variable faces change
        if (!face.isVariable || font in stableFonts) return false
        val now = TimeSource.Monotonic.markNow()
        val masters = seen.getOrPut(font) { mutableMapOf() }
        masters[face.master.axes.toMap()] = now
        // the masters that were not seen within the time are forgotten
        masters.entries.removeAll { (_, at) -> (now - at).inWholeMilliseconds > churnSeconds * 1000.0 }
        if (masters.size > churnMasters) {
            transientFonts.add(font)
            seen.remove(font)
            return true
        }
        return false
    }

    /**
     * Clears the transient map of [boundaryDrawer] when it uses more than [transientBudget], before glyphs are
     * added to it; gives true when it was cleared, which bumps its [BoundaryGlyphMap.generation]
     */
    fun trimTransient(boundaryDrawer: BoundaryDrawer): Boolean {
        val map = transientMaps[boundaryDrawer] ?: return false
        if (map.map.usedBytes <= transientBudget) return false
        map.clear()
        return true
    }
}
