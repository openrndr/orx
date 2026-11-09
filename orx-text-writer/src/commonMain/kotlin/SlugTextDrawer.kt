package org.openrndr.extra.textwriter

import org.openrndr.math.Matrix44
import org.openrndr.Program
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.boundary.BoundaryCommand
import org.openrndr.draw.boundary.BoundaryDrawer
import org.openrndr.draw.boundary.BoundaryGlyphMap
import org.openrndr.draw.font.Face
import org.openrndr.draw.font.internal.Direction
import org.openrndr.draw.font.internal.ShapeFeature
import org.openrndr.draw.font.internal.TextShapingDriver
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import kotlin.jvm.JvmRecord

enum class StrokeMode(val mode: Int) {
    CENTER(0),
    INNER(1),
    OUTER(2),
    ERODE(3),
}

/**
 * Represents a style configuration for text rendering.
 *
 * @property justify Determines whether the text should be justified.
 * @property lineHeightInEm Specifies the line height multiplier for the text.
 * @property characterSpacingInEm Space added after every character, in em, like CSS letter-spacing. Ligatures are
 * not formed when it is not zero.
 * @property wordSpacingInEm Space added to every space between words, in em, like CSS word-spacing.
 * @property horizontalAlign Defines the horizontal alignment of the text, typically in the range [0.0, 1.0].
 * @property verticalAlign Defines the vertical alignment of the text, typically in the range [0.0, 1.0].
 * @property direction The paragraph direction, either [Direction.LEFT_TO_RIGHT] or [Direction.RIGHT_TO_LEFT], or
 * null to determine it from the first strong character of every paragraph. Mixed-direction text is ordered using
 * [org.openrndr.draw.font.internal.BidiTextDriver].
 * @property language The language of the text as a language tag, for example `en`, `en-GB` or `nl`. It is passed to
 * the shaper and, when [hyphenate] is enabled, used to hyphenate the text automatically, see [Hyphenator.findPatternSet].
 * @property hyphenate Determines whether text with a [language] is hyphenated automatically. Words that contain soft
 * hyphens are not hyphenated automatically.
 */

@JvmRecord
data class TextStyle(
    val face: Face? = null,
    val sizeInEm: Double? = null,
    val justify: Boolean? = null,
    val lineHeightInEm: Double? = null,
    val textWidthFactor: Double? = null,
    val characterSpacingInEm: Double? = null,
    val wordSpacingInEm: Double? = null,
    val baselineShiftInEm: Double? = null,
    val horizontalAlign: Double? = null,
    val verticalAlign: Double? = null,
    val fill: ColorRGBa? = null,
    val stroke: ColorRGBa? = null,
    val strokeWeight: Double? = null,
    val strokeMode: StrokeMode? = null,
    val features: List<ShapeFeature> = emptyList(),
    val direction: Direction? = null,
    val language: String? = null,
    val hyphenate: Boolean? = null,
) {
    fun cascade(other: TextStyle?): TextStyle {
        return if (other == null) this else copy(
            face = other.face ?: face,
            sizeInEm = other.sizeInEm ?: sizeInEm,
            justify = other.justify ?: justify,
            lineHeightInEm = other.lineHeightInEm ?: lineHeightInEm,
            textWidthFactor = other.textWidthFactor ?: textWidthFactor,
            characterSpacingInEm = other.characterSpacingInEm ?: characterSpacingInEm,
            wordSpacingInEm = other.wordSpacingInEm ?: wordSpacingInEm,
            baselineShiftInEm = other.baselineShiftInEm ?: baselineShiftInEm,
            horizontalAlign = other.horizontalAlign ?: horizontalAlign,
            verticalAlign = other.verticalAlign ?: verticalAlign,
            fill = other.fill ?: fill,
            stroke = other.stroke ?: stroke,
            strokeWeight = other.strokeWeight ?: strokeWeight,
            strokeMode = other.strokeMode ?: strokeMode,
            features = features + other.features,
            direction = other.direction ?: direction,
            language = other.language ?: language,
            hyphenate = other.hyphenate ?: hyphenate
        )
    }
    companion object {
        val Defaults = TextStyle(
            sizeInEm = 1.0,
            justify = false,
            lineHeightInEm = 1.0,
            textWidthFactor = 1.0,
            characterSpacingInEm = 0.0,
            wordSpacingInEm = 0.0,
            baselineShiftInEm = 0.0,
            horizontalAlign = 0.0,
            verticalAlign = 0.0,
            fill = ColorRGBa.WHITE,
            strokeMode = StrokeMode.CENTER,
            direction = Direction.LEFT_TO_RIGHT,
            hyphenate = true
        )
    }
}

data class TextSpan(val text: String, val style: TextStyle?)


/**
 * Draws text with a [BoundaryDrawer], which draws glyphs from their outlines: by default that of the program, a
 * Windfoil drawer, a [org.openrndr.draw.slug.SlugDrawer] when one is given.
 *
 * @param boundaryDrawer the boundary drawer that draws the text, for example a [org.openrndr.draw.slug.SlugDrawer],
 * or null for the boundary drawer of the drawer of the active program. The boundary drawer is used from the first
 * time glyphs are added on.
 */
class SlugTextDrawer(boundaryDrawer: BoundaryDrawer? = null) : TextDrawer {

    val shaper = TextShapingDriver.instance

    /** the boundary drawer that draws the text */
    val boundaryDrawer: BoundaryDrawer by lazy {
        boundaryDrawer ?: Program.active?.drawer?.boundaryDrawer
        ?: error("no boundary drawer given and there is no active program")
    }

    // the glyphs are shared with everything else that draws text with the same boundary drawer
    private val glyphMap by lazy { GlyphMaps.of(this.boundaryDrawer) }

    // the glyphs of faces whose outlines keep changing, which are cleared now and then, see GlyphMaps
    private val transientMap by lazy { GlyphMaps.transientOf(this.boundaryDrawer) }

    /** the commands of the glyphs, in the order they were added; those of transient glyphs index the transient map */
    val commands = mutableListOf<BoundaryCommand>()

    /** the face and the glyph of every command of a transient glyph, by its place in [commands], to find it again */
    private val transientGlyphs = mutableMapOf<Int, Pair<org.openrndr.draw.font.Face, Int>>()

    /** the generation of the transient map that the indices of the transient glyphs are of */
    private var transientGeneration = -1

    override var style = TextStyle.Defaults


    override fun addText(spans: List<TextSpan>, boxes: List<Rectangle>) {
        addGlyphs(typesetText(spans, boxes, style, shaper).flatMap { it.glyphs })
    }

    override fun addGlyphs(glyphs: List<TypesetGlyph>) {
        addGlyphs(glyphs) { it.transform() }
    }

    /**
     * Adds glyphs that are placed by [transform] instead of by their positions, for example along a path.
     * @param transform the transform of the outline of a glyph, replacing [TypesetGlyph.transform]
     */
    fun addGlyphs(glyphs: List<TypesetGlyph>, transform: (TypesetGlyph) -> Matrix44) {
        addCommands(glyphs.map { it to transform(it) })
    }

    /**
     * Adds the glyphs of text that is placed on a contour by [layoutTextOnContour]
     */
    fun addGlyphs(layout: ContourTextLayout) {
        addCommands(layout.glyphs.map { it.glyph to it.transform })
    }

    /** adds the glyphs [glyphs] of [map] in a single batch, so that the map is written once, before indices are used */
    private fun BoundaryGlyphMap.addAll(glyphs: Collection<Pair<org.openrndr.draw.font.Face, Int>>) {
        startBatch()
        try {
            for ((face, index) in glyphs) getBoundaryForGlyphIndex(face, index)
        } finally {
            endBatch()
        }
    }

    /** finds the transient glyphs in the transient map again when it was cleared since their indices were taken */
    private fun refreshTransient() {
        if (transientGlyphs.isEmpty()) return
        GlyphMaps.trimTransient(boundaryDrawer)
        if (transientMap.generation == transientGeneration) return
        transientMap.addAll(transientGlyphs.values.toSet())
        for ((i, glyph) in transientGlyphs) {
            commands[i] = commands[i].copy(boundaryIndex = transientMap.getBoundaryForGlyphIndex(glyph.first, glyph.second))
        }
        transientGeneration = transientMap.generation
    }

    private fun addCommands(glyphs: List<Pair<TypesetGlyph, Matrix44>>) {
        // the glyphs of faces whose masters keep changing go into the transient map, the others into the shared map
        val transient = glyphs.map { it.first.face }.distinct().associateWith { GlyphMaps.use(it) }
        if (transient.values.any { it }) {
            // the transient map is cleared before glyphs are added when it grew too large, the transient glyphs
            // added earlier are found again
            GlyphMaps.trimTransient(boundaryDrawer)
            refreshTransient()
            transientMap.addAll(glyphs.filter { transient.getValue(it.first.face) }.map { it.first.face to it.first.glyphIndex }.toSet())
            transientGeneration = transientMap.generation
        }
        glyphMap.addAll(glyphs.filter { !transient.getValue(it.first.face) }.map { it.first.face to it.first.glyphIndex }.toSet())

        for ((glyph, transform) in glyphs) {
            val glyphStyle = glyph.style
            val isTransient = transient.getValue(glyph.face)
            if (isTransient) transientGlyphs[commands.size] = glyph.face to glyph.glyphIndex
            commands.add(
                BoundaryCommand(
                    (if (isTransient) transientMap else glyphMap).getBoundaryForGlyphIndex(glyph.face, glyph.glyphIndex),
                    transform,
                    glyphStyle.fill,
                    glyphStyle.stroke,
                    glyphStyle.strokeWeight ?: 0.0,
                    strokeMode = (glyphStyle.strokeMode ?: StrokeMode.CENTER).mode
                )
            )
        }
    }

    override fun addText(face: Face, text: String, position: Vector2) {
        addGlyphs(typesetLine(face, text, position, style, shaper))
    }

    override fun clear() {
        commands.clear()
        transientGlyphs.clear()
    }

    /**
     * Draws the glyphs: those of the shared map, then those of the transient map, which are drawn over them since
     * every map is drawn on its own
     */
    override fun draw(drawer: Drawer) {
        if (commands.isEmpty()) return
        if (transientGlyphs.isEmpty()) {
            boundaryDrawer.prepare(glyphMap.map, commands)
            boundaryDrawer.draw(drawer, glyphMap.map)
            return
        }
        refreshTransient()
        val stable = commands.filterIndexed { i, _ -> i !in transientGlyphs }
        if (stable.isNotEmpty()) {
            boundaryDrawer.prepare(glyphMap.map, stable)
            boundaryDrawer.draw(drawer, glyphMap.map)
        }
        boundaryDrawer.prepare(transientMap.map, transientGlyphs.keys.sorted().map { commands[it] })
        boundaryDrawer.draw(drawer, transientMap.map)
    }

}