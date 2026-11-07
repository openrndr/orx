package org.openrndr.extra.textwriter

import org.openrndr.draw.boundary.BoundaryDrawer
import org.openrndr.draw.boundary.BoundaryGlyphMap

/**
 * The glyph maps of boundary drawers, one for every boundary drawer: everything that draws text with the same
 * boundary drawer, like compositions and user interfaces, finds the outlines of glyphs in the same map, and uploads
 * every glyph once.
 */
object GlyphMaps {
    private val maps = mutableMapOf<BoundaryDrawer, BoundaryGlyphMap>()

    /** the glyph map of [boundaryDrawer] */
    fun of(boundaryDrawer: BoundaryDrawer): BoundaryGlyphMap =
        maps.getOrPut(boundaryDrawer) { BoundaryGlyphMap(boundaryDrawer.createMap()) }
}
