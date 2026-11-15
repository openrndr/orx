package org.openrndr.extra.textwriter.math

import org.openrndr.draw.font.Face
import org.openrndr.draw.font.MathConstant
import org.openrndr.draw.font.MathDirection
import org.openrndr.draw.font.MathGlyphAssembly
import org.openrndr.draw.font.MathTable
import org.openrndr.draw.font.internal.ShapeFeature
import org.openrndr.draw.font.internal.TextShapingDriver
import org.openrndr.extra.textwriter.TextStyle
import org.openrndr.extra.textwriter.TypesetGlyph
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * A laid out formula: its [glyphs], drawn like typeset text, for instance with
 * [org.openrndr.extra.textwriter.SlugTextDrawer.addGlyphs], and its [rules], the bars of fractions and the overbars of
 * radicals, which are filled rectangles. Its baseline starts at [origin]; it is [width] wide and reaches [ascent]
 * above and [descent] below the baseline.
 */
class MathLayout(
    val origin: Vector2,
    val width: Double,
    val ascent: Double,
    val descent: Double,
    val glyphs: List<TypesetGlyph>,
    val rules: List<Rectangle>
) {
    /** the rectangle the formula takes */
    val bounds: Rectangle get() = Rectangle(origin.x, origin.y - ascent, width, ascent + descent)
}

/**
 * Lays out [formula] with [face], a face of a math font, with the start of its baseline at [origin]. The layout
 * follows the OpenType MATH table of the face, [table], which the text shaping driver gives when it is null. A [MathNode.Formula] says itself whether it is displayed; [display] is for other nodes. The glyphs
 * have [style], like its fill, with the face and the size of the formula.
 */
fun layoutMath(
    formula: MathNode,
    face: Face,
    table: MathTable? = null,
    origin: Vector2 = Vector2.ZERO,
    display: Boolean = false,
    style: TextStyle = TextStyle.Defaults,
    shaper: TextShapingDriver = TextShapingDriver.instance
): MathLayout {
    val glyphStyle = TextStyle.Defaults.cascade(style)
    val mathTable = table ?: shaper.mathTable(face) ?: error("the face has no OpenType MATH table, math layout needs a math font")
    val box = MathLayouter(face, mathTable, shaper).layout(formula, MathStyle(display, 0, false))
    val glyphs = box.glyphs.map { g ->
        TypesetGlyph(face, g.glyphIndex, Vector2(origin.x + g.x, origin.y - g.y), glyphStyle.copy(face = face, sizeInEm = g.scale))
    }
    val rules = box.rules.map { r -> Rectangle(origin.x + r.x, origin.y - r.y - r.height, r.width, r.height) }
    return MathLayout(origin, box.width, box.ascent, box.descent, glyphs, rules)
}

/** [display] for display style, [level] the script level, [cramped] where superscripts are raised less */
internal data class MathStyle(val display: Boolean, val level: Int, val cramped: Boolean) {
    fun script() = MathStyle(false, level + 1, cramped)
    fun cramped() = copy(cramped = true)
}

/** a glyph at [x], [y] from the start of the baseline of a box, with y up, at [scale] times the size of the face */
internal class BoxGlyph(val x: Double, val y: Double, val glyphIndex: Int, val scale: Double)

/** a filled rectangle from [x], [y] (its bottom left, with y up) */
internal class BoxRule(val x: Double, val y: Double, val width: Double, val height: Double)

/**
 * A laid out part of a formula, with y up from its baseline. [italicCorrection] is how far its last glyph leans out
 * to the right; [operator] tells the operator properties of a box that is an operator.
 */
internal class MathBox(
    val width: Double,
    val ascent: Double,
    val descent: Double,
    val glyphs: List<BoxGlyph> = emptyList(),
    val rules: List<BoxRule> = emptyList(),
    val italicCorrection: Double = 0.0,
    val operator: OperatorProperties? = null
)

/** collects the glyphs and rules of boxes placed at offsets */
private class BoxBuilder {
    val glyphs = mutableListOf<BoxGlyph>()
    val rules = mutableListOf<BoxRule>()

    fun place(box: MathBox, x: Double, y: Double) {
        for (g in box.glyphs) glyphs.add(BoxGlyph(g.x + x, g.y + y, g.glyphIndex, g.scale))
        for (r in box.rules) rules.add(BoxRule(r.x + x, r.y + y, r.width, r.height))
    }

    fun build(width: Double, ascent: Double, descent: Double, italicCorrection: Double = 0.0, operator: OperatorProperties? = null) =
        MathBox(width, ascent, descent, glyphs.toList(), rules.toList(), italicCorrection, operator)
}

private class MathLayouter(val face: Face, val table: MathTable, val shaper: TextShapingDriver) {
    /** the size of the face in pixels, one em */
    val em = face.sizeInPoints

    fun scale(style: MathStyle): Double {
        val script = table.constant(MathConstant.SCRIPT_PERCENT_SCALE_DOWN).takeIf { it > 0.0 } ?: 71.0
        val scriptScript = table.constant(MathConstant.SCRIPT_SCRIPT_PERCENT_SCALE_DOWN).takeIf { it > 0.0 } ?: 50.0
        return when (style.level) {
            0 -> 1.0
            1 -> script / 100.0
            else -> scriptScript / 100.0 * 0.71.pow(style.level - 2)
        }
    }

    /** a length of the MATH table at the size of [style] */
    fun length(constant: MathConstant, style: MathStyle) = table.constant(constant) * scale(style)

    fun layout(node: MathNode, style: MathStyle): MathBox = when (node) {
        is MathNode.Formula -> layout(node.child, style.copy(display = node.display))
        is MathNode.Style -> layout(
            node.child,
            style.copy(display = node.displayStyle ?: style.display, level = node.scriptLevel ?: style.level)
        )
        is MathNode.Row -> row(node.children, style)
        is MathNode.Identifier -> token(identifierText(node), style)
        is MathNode.Number -> token(node.text, style)
        is MathNode.Text -> token(node.text, style)
        is MathNode.Operator -> operator(node, "infix", style, null)
        is MathNode.Space -> {
            val s = scale(style) * em
            MathBox(node.widthInEm * s, node.heightInEm * s, node.depthInEm * s)
        }
        is MathNode.Fraction -> fraction(node, style)
        is MathNode.SquareRoot -> radical(node.radicand, null, style)
        is MathNode.Root -> radical(node.radicand, node.index, style)
        is MathNode.Scripts -> scripts(node.base, node.subscript, node.superscript, style)
        is MathNode.UnderOver -> underOver(node, style)
    }

    //<editor-fold desc="tokens">
    /** a single letter in mathematical italic, as MathML does for identifiers */
    fun identifierText(node: MathNode.Identifier): String {
        if (node.variant != null && node.variant != "italic") return node.text
        val codePoints = node.text.mathCodePoints()
        if (codePoints.size != 1) return node.text
        val c = codePoints[0]
        val italic = when (c) {
            'h'.code -> 0x210E
            in 'a'.code..'z'.code -> 0x1D44E + (c - 'a'.code)
            in 'A'.code..'Z'.code -> 0x1D434 + (c - 'A'.code)
            in 0x3B1..0x3C9 -> 0x1D6FC + (c - 0x3B1)
            else -> c
        }
        return codePointString(italic)
    }

    /** the glyphs of [text] at the size of [style], with the script style alternates of the font in scripts */
    fun token(text: String, style: MathStyle): MathBox {
        if (text.isEmpty()) return MathBox(0.0, 0.0, 0.0)
        val scale = scale(style)
        val features = if (style.level > 0) listOf(ShapeFeature("ssty", min(style.level, 2))) else emptyList()
        val shaped = shaper.shape(face, text, features)
        val glyphs = mutableListOf<BoxGlyph>()
        var x = 0.0
        var ascent = 0.0
        var descent = 0.0
        for (r in shaped) {
            val gx = x + r.offset.x * scale
            val gy = -r.offset.y * scale
            glyphs.add(BoxGlyph(gx, gy, r.glyphIndex, scale))
            val (a, d) = ink(r.glyphIndex, scale)
            ascent = max(ascent, a + gy)
            descent = max(descent, d - gy)
            x += r.advance.x * scale
        }
        val italic = shaped.lastOrNull()?.let { table.italicCorrection(it.glyphIndex) * scale } ?: 0.0
        return MathBox(x, ascent, descent, glyphs, italicCorrection = italic)
    }

    /** how far the ink of a glyph reaches above and below the baseline */
    fun ink(glyphIndex: Int, scale: Double): Pair<Double, Double> {
        val b = face.glyphForIndex(glyphIndex).bounds()
        return (-b.y * scale) to ((b.y + b.height) * scale)
    }
    //</editor-fold>

    //<editor-fold desc="rows and operators">
    fun row(children: List<MathNode>, style: MathStyle): MathBox {
        if (children.isEmpty()) return MathBox(0.0, 0.0, 0.0)
        fun form(i: Int) = when {
            children.size == 1 -> "infix"
            i == 0 -> "prefix"
            i == children.lastIndex -> "postfix"
            else -> "infix"
        }
        fun stretchy(i: Int): Boolean {
            val op = children[i] as? MathNode.Operator ?: return false
            return op.stretchy ?: MathOperators.properties(op.text, op.form ?: form(i)).stretchy
        }
        // the boxes of all but the stretchy operators, which stretch to cover them
        val boxes = arrayOfNulls<MathBox>(children.size)
        for (i in children.indices) {
            if (stretchy(i)) continue
            val child = children[i]
            boxes[i] = if (child is MathNode.Operator) operator(child, child.form ?: form(i), style, null) else layout(child, style)
        }
        val others = boxes.filterNotNull()
        val target = if (others.isEmpty()) null else others.maxOf { it.ascent } to others.maxOf { it.descent }
        for (i in children.indices) {
            if (boxes[i] == null) {
                val op = children[i] as MathNode.Operator
                boxes[i] = operator(op, op.form ?: form(i), style, target)
            }
        }
        if (children.size == 1) return boxes[0]!!
        val builder = BoxBuilder()
        var x = 0.0
        var ascent = 0.0
        var descent = 0.0
        for (box in boxes) {
            box!!
            builder.place(box, x, 0.0)
            x += box.width
            ascent = max(ascent, box.ascent)
            descent = max(descent, box.descent)
        }
        return builder.build(x, ascent, descent, boxes.last()!!.italicCorrection)
    }

    /**
     * An operator: spaced by the dictionary, stretched to [target] (an ascent and a descent) when it is stretchy, and
     * enlarged in display style when it is a large operator.
     */
    fun operator(node: MathNode.Operator, form: String, style: MathStyle, target: Pair<Double, Double>?): MathBox {
        val dictionary = MathOperators.properties(node.text, node.form ?: form)
        val properties = dictionary.copy(
            leftSpaceInEm = node.leftSpaceInEm ?: dictionary.leftSpaceInEm,
            rightSpaceInEm = node.rightSpaceInEm ?: dictionary.rightSpaceInEm,
            stretchy = node.stretchy ?: dictionary.stretchy,
            largeOperator = node.largeOperator ?: dictionary.largeOperator,
            movableLimits = node.movableLimits ?: dictionary.movableLimits
        )
        val scale = scale(style)
        val token = token(MathOperators.glyphText(node.text), style)
        val axis = length(MathConstant.AXIS_HEIGHT, style)
        val single = token.glyphs.singleOrNull()

        val glyph: MathBox = when {
            single != null && properties.largeOperator && style.display -> {
                val minHeight = length(MathConstant.DISPLAY_OPERATOR_MIN_HEIGHT, style)
                val variants = table.variants(single.glyphIndex, MathDirection.VERTICAL)
                val variant = variants.firstOrNull { it.advance * scale >= minHeight } ?: variants.lastOrNull()
                centeredOnAxis(variant?.glyphIndex ?: single.glyphIndex, scale, axis)
            }
            single != null && properties.largeOperator -> centeredOnAxis(single.glyphIndex, scale, axis)
            single != null && properties.stretchy && target != null -> {
                val (ascent, descent) = target
                val height = if (properties.symmetric) 2.0 * max(ascent - axis, descent + axis) else ascent + descent
                val center = if (properties.symmetric) axis else (ascent - descent) / 2.0
                stretchVertically(single.glyphIndex, height, center, style)
            }
            else -> token
        }
        val left = properties.leftSpaceInEm * em * scale
        val right = properties.rightSpaceInEm * em * scale
        val builder = BoxBuilder()
        builder.place(glyph, left, 0.0)
        return builder.build(left + glyph.width + right, glyph.ascent, glyph.descent, glyph.italicCorrection, properties)
    }

    /** a glyph whose ink is centered on the math axis */
    fun centeredOnAxis(glyphIndex: Int, scale: Double, axis: Double): MathBox = glyphCenteredAt(glyphIndex, scale, axis)

    fun glyphCenteredAt(glyphIndex: Int, scale: Double, center: Double): MathBox {
        val (a, d) = ink(glyphIndex, scale)
        val dy = center - (a - d) / 2.0
        val advance = face.glyphForIndex(glyphIndex).advanceWidth() * scale
        return MathBox(
            advance, a + dy, d - dy, listOf(BoxGlyph(0.0, dy, glyphIndex, scale)),
            italicCorrection = table.italicCorrection(glyphIndex) * scale
        )
    }

    /**
     * the glyph [glyphIndex] at least [height] high, centered at [center]: the first variant that is high enough,
     * else an assembly of its parts, else its largest variant
     */
    fun stretchVertically(glyphIndex: Int, height: Double, center: Double, style: MathStyle): MathBox {
        val scale = scale(style)
        val variants = table.variants(glyphIndex, MathDirection.VERTICAL)
        variants.firstOrNull { it.advance * scale >= height }?.let { return glyphCenteredAt(it.glyphIndex, scale, center) }
        table.assembly(glyphIndex, MathDirection.VERTICAL)?.let { assembly ->
            assemble(assembly, height, scale)?.let { box ->
                val dy = center - (box.ascent - box.descent) / 2.0
                val builder = BoxBuilder()
                builder.place(box, 0.0, dy)
                return builder.build(box.width, box.ascent + dy, box.descent - dy, assembly.italicCorrection * scale)
            }
        }
        return glyphCenteredAt(variants.lastOrNull()?.glyphIndex ?: glyphIndex, scale, center)
    }

    /**
     * The parts of [assembly] stacked from the baseline up to at least [height]: extenders are repeated as few times
     * as needed, and the parts overlap evenly, at least by the minimum connector overlap.
     */
    fun assemble(assembly: MathGlyphAssembly, height: Double, scale: Double): MathBox? {
        val parts = assembly.parts
        if (parts.isEmpty()) return null
        val minOverlap = table.minConnectorOverlap(MathDirection.VERTICAL) * scale
        val hasExtenders = parts.any { it.isExtender }
        var sequence = parts.filter { !it.isExtender }.ifEmpty { parts }
        for (repeats in 1..256) {
            val longest = sequence.sumOf { it.fullAdvance * scale } - (sequence.size - 1) * minOverlap
            if (longest >= height || !hasExtenders) break
            sequence = parts.flatMap { if (it.isExtender) List(repeats) { _ -> it } else listOf(it) }
        }
        // the same overlap at every joint, as much as the connectors allow and no more than is needed
        val joints = sequence.zipWithNext { a, b -> min(a.endConnectorLength, b.startConnectorLength) * scale }
        val maxOverlap = joints.minOrNull() ?: 0.0
        val total = sequence.sumOf { it.fullAdvance * scale }
        val overlap = if (sequence.size > 1) {
            ((total - height) / (sequence.size - 1)).coerceIn(minOverlap, max(minOverlap, maxOverlap))
        } else 0.0
        val glyphs = mutableListOf<BoxGlyph>()
        var y = 0.0
        var width = 0.0
        for (part in sequence) {
            val (_, d) = ink(part.glyphIndex, scale)
            // the bottom of the ink of the part where the part starts
            glyphs.add(BoxGlyph(0.0, y + d, part.glyphIndex, scale))
            width = max(width, face.glyphForIndex(part.glyphIndex).advanceWidth() * scale)
            y += part.fullAdvance * scale - overlap
        }
        val extent = y + overlap
        return MathBox(width, extent, 0.0, glyphs)
    }
    //</editor-fold>

    //<editor-fold desc="fractions and radicals">
    fun fraction(node: MathNode.Fraction, style: MathStyle): MathBox {
        // the parts are a step smaller, except in display style, where they are in text style
        val inner = if (style.display) style.copy(display = false) else style.script()
        val numerator = layout(node.numerator, inner)
        val denominator = layout(node.denominator, inner.cramped())
        val axis = length(MathConstant.AXIS_HEIGHT, style)
        val thickness = length(MathConstant.FRACTION_RULE_THICKNESS, style) * node.lineThicknessFactor
        val display = style.display
        val numeratorShift: Double
        val denominatorShift: Double
        if (thickness > 0.0) {
            val shiftUp = length(if (display) MathConstant.FRACTION_NUMERATOR_DISPLAY_STYLE_SHIFT_UP else MathConstant.FRACTION_NUMERATOR_SHIFT_UP, style)
            val shiftDown = length(if (display) MathConstant.FRACTION_DENOMINATOR_DISPLAY_STYLE_SHIFT_DOWN else MathConstant.FRACTION_DENOMINATOR_SHIFT_DOWN, style)
            val gapAbove = length(if (display) MathConstant.FRACTION_NUM_DISPLAY_STYLE_GAP_MIN else MathConstant.FRACTION_NUMERATOR_GAP_MIN, style)
            val gapBelow = length(if (display) MathConstant.FRACTION_DENOM_DISPLAY_STYLE_GAP_MIN else MathConstant.FRACTION_DENOMINATOR_GAP_MIN, style)
            numeratorShift = max(shiftUp, axis + thickness / 2.0 + gapAbove + numerator.descent)
            denominatorShift = max(shiftDown, denominator.ascent + gapBelow - axis + thickness / 2.0)
        } else {
            // a stack without a rule
            val shiftUp = length(if (display) MathConstant.STACK_TOP_DISPLAY_STYLE_SHIFT_UP else MathConstant.STACK_TOP_SHIFT_UP, style)
            val shiftDown = length(if (display) MathConstant.STACK_BOTTOM_DISPLAY_STYLE_SHIFT_DOWN else MathConstant.STACK_BOTTOM_SHIFT_DOWN, style)
            val gap = length(if (display) MathConstant.STACK_DISPLAY_STYLE_GAP_MIN else MathConstant.STACK_GAP_MIN, style)
            var up = shiftUp
            var down = shiftDown
            val actual = (up - numerator.descent) - (denominator.ascent - down)
            if (actual < gap) {
                up += (gap - actual) / 2.0
                down += (gap - actual) / 2.0
            }
            numeratorShift = up
            denominatorShift = down
        }
        val width = max(numerator.width, denominator.width)
        val builder = BoxBuilder()
        builder.place(numerator, (width - numerator.width) / 2.0, numeratorShift)
        builder.place(denominator, (width - denominator.width) / 2.0, -denominatorShift)
        if (thickness > 0.0) builder.rules.add(BoxRule(0.0, axis - thickness / 2.0, width, thickness))
        return builder.build(width, numeratorShift + numerator.ascent, denominatorShift + denominator.descent)
    }

    fun radical(radicandNode: MathNode, indexNode: MathNode?, style: MathStyle): MathBox {
        val radicand = layout(radicandNode, style.cramped())
        val scale = scale(style)
        val thickness = length(MathConstant.RADICAL_RULE_THICKNESS, style)
        var gap = length(if (style.display) MathConstant.RADICAL_DISPLAY_STYLE_VERTICAL_GAP else MathConstant.RADICAL_VERTICAL_GAP, style)
        val extraAscender = length(MathConstant.RADICAL_EXTRA_ASCENDER, style)
        val height = radicand.ascent + radicand.descent + gap + thickness
        val sign = shaper.shape(face, "√").firstOrNull()?.glyphIndex ?: error("the font has no radical sign")
        // the sign stretched to the height, its top at the top of the rule
        val stretched = stretchVertically(sign, height, 0.0, style)
        val signHeight = stretched.ascent + stretched.descent
        // what the sign is higher than needed goes to the gap
        if (signHeight > height) gap += (signHeight - height) / 2.0
        val ruleBottom = radicand.ascent + gap
        val signShift = ruleBottom + thickness - stretched.ascent

        val builder = BoxBuilder()
        var x = 0.0
        var ascent = ruleBottom + thickness + extraAscender
        if (indexNode != null) {
            // the index two steps smaller, raised along the sign
            val index = layout(indexNode, MathStyle(false, style.level + 2, false))
            val before = length(MathConstant.RADICAL_KERN_BEFORE_DEGREE, style)
            val after = length(MathConstant.RADICAL_KERN_AFTER_DEGREE, style)
            val raise = table.constant(MathConstant.RADICAL_DEGREE_BOTTOM_RAISE_PERCENT) / 100.0
            val bottom = signShift - stretched.descent
            val indexShift = bottom + raise * signHeight + index.descent
            builder.place(index, max(0.0, before), indexShift)
            x = max(0.0, before + index.width + after)
            ascent = max(ascent, indexShift + index.ascent)
        }
        builder.place(stretched, x, signShift)
        x += stretched.width
        builder.rules.add(BoxRule(x, ruleBottom, radicand.width, thickness))
        builder.place(radicand, x, 0.0)
        val descent = max(radicand.descent, stretched.descent - signShift)
        return builder.build(x + radicand.width, ascent, descent)
    }
    //</editor-fold>

    //<editor-fold desc="scripts">
    /** the operator properties of [node] when it is an operator, also inside a row of one */
    fun operatorOf(node: MathNode): OperatorProperties? = when (node) {
        is MathNode.Operator -> MathOperators.properties(node.text, node.form ?: "prefix").let { d ->
            d.copy(
                largeOperator = node.largeOperator ?: d.largeOperator,
                movableLimits = node.movableLimits ?: d.movableLimits
            )
        }
        is MathNode.Row -> node.children.singleOrNull()?.let { operatorOf(it) }
        else -> null
    }

    fun scripts(baseNode: MathNode, subNode: MathNode?, supNode: MathNode?, style: MathStyle): MathBox {
        val base = layout(baseNode, style)
        val largeOperator = base.operator?.largeOperator == true
        val sub = subNode?.let { layout(it, style.script().cramped()) }
        val sup = supNode?.let { layout(it, style.script()) }
        val builder = BoxBuilder()
        builder.place(base, 0.0, 0.0)
        // superscripts lean out with the italic correction, subscripts of large operators lean in
        val italic = base.italicCorrection
        val supX = base.width + if (largeOperator) 0.0 else italic
        val subX = base.width - if (largeOperator) italic else 0.0
        val spaceAfter = length(MathConstant.SPACE_AFTER_SCRIPT, style)

        var subShift = 0.0
        var supShift = 0.0
        if (sub != null) {
            subShift = maxOf(
                length(MathConstant.SUBSCRIPT_SHIFT_DOWN, style),
                base.descent + length(MathConstant.SUBSCRIPT_BASELINE_DROP_MIN, style),
                sub.ascent - length(MathConstant.SUBSCRIPT_TOP_MAX, style)
            )
        }
        if (sup != null) {
            supShift = maxOf(
                length(if (style.cramped) MathConstant.SUPERSCRIPT_SHIFT_UP_CRAMPED else MathConstant.SUPERSCRIPT_SHIFT_UP, style),
                base.ascent - length(MathConstant.SUPERSCRIPT_BASELINE_DROP_MAX, style),
                length(MathConstant.SUPERSCRIPT_BOTTOM_MIN, style) + sup.descent
            )
        }
        if (sub != null && sup != null) {
            // a gap between the scripts, keeping the superscript low when the subscript allows it
            val gap = (supShift - sup.descent) - (sub.ascent - subShift)
            val minGap = length(MathConstant.SUB_SUPERSCRIPT_GAP_MIN, style)
            if (gap < minGap) {
                subShift += minGap - gap
                val bottomMax = length(MathConstant.SUPERSCRIPT_BOTTOM_MAX_WITH_SUBSCRIPT, style)
                val lift = bottomMax - (supShift - sup.descent)
                if (lift > 0.0) {
                    supShift += lift
                    subShift -= lift
                }
            }
        }
        var width = base.width
        var ascent = base.ascent
        var descent = base.descent
        if (sub != null) {
            builder.place(sub, subX, -subShift)
            width = max(width, subX + sub.width + spaceAfter)
            descent = max(descent, subShift + sub.descent)
            ascent = max(ascent, sub.ascent - subShift)
        }
        if (sup != null) {
            builder.place(sup, supX, supShift)
            width = max(width, supX + sup.width + spaceAfter)
            ascent = max(ascent, supShift + sup.ascent)
            descent = max(descent, sup.descent - supShift)
        }
        return builder.build(width, ascent, descent)
    }

    fun underOver(node: MathNode.UnderOver, style: MathStyle): MathBox {
        val operator = operatorOf(node.base)
        // limits of operators like sums are scripts outside display style
        if (operator?.movableLimits == true && !style.display) {
            return scripts(node.base, node.underscript, node.overscript, style)
        }
        val base = layout(node.base, style)
        val largeOperator = base.operator?.largeOperator == true
        val under = node.underscript?.let { layout(it, if (node.accentUnder) style.copy(display = false) else style.script()) }
        val over = node.overscript?.let { layout(it, if (node.accent) style.copy(display = false) else style.script()) }
        val width = maxOf(base.width, under?.width ?: 0.0, over?.width ?: 0.0)
        val builder = BoxBuilder()
        builder.place(base, (width - base.width) / 2.0, 0.0)
        var ascent = base.ascent
        var descent = base.descent
        // the limits of operators that lean are shifted by half their italic correction
        val lean = if (largeOperator) base.italicCorrection / 2.0 else 0.0
        if (over != null) {
            val shift = when {
                largeOperator -> max(
                    length(MathConstant.UPPER_LIMIT_BASELINE_RISE_MIN, style) + over.descent,
                    base.ascent + length(MathConstant.UPPER_LIMIT_GAP_MIN, style) + over.descent
                ) + 0.0
                node.accent -> max(base.ascent, length(MathConstant.ACCENT_BASE_HEIGHT, style)) -
                        length(MathConstant.ACCENT_BASE_HEIGHT, style) + over.descent.coerceAtLeast(0.0)
                else -> base.ascent + length(MathConstant.OVERBAR_VERTICAL_GAP, style) + over.descent
            }
            builder.place(over, (width - over.width) / 2.0 + lean, shift)
            ascent = max(ascent, shift + over.ascent)
        }
        if (under != null) {
            val shift = when {
                largeOperator -> max(
                    length(MathConstant.LOWER_LIMIT_BASELINE_DROP_MIN, style) + under.ascent,
                    base.descent + length(MathConstant.LOWER_LIMIT_GAP_MIN, style) + under.ascent
                )
                node.accentUnder -> base.descent + under.ascent
                else -> base.descent + length(MathConstant.UNDERBAR_VERTICAL_GAP, style) + under.ascent
            }
            builder.place(under, (width - under.width) / 2.0 - lean, -shift)
            descent = max(descent, shift + under.descent)
        }
        return builder.build(width, ascent, descent, operator = base.operator)
    }
    //</editor-fold>
}

private fun String.mathCodePoints(): List<Int> {
    val result = mutableListOf<Int>()
    var i = 0
    while (i < length) {
        val c = this[i]
        if (c.isHighSurrogate() && i + 1 < length && this[i + 1].isLowSurrogate()) {
            result.add(((c.code - 0xD800) shl 10) + (this[i + 1].code - 0xDC00) + 0x10000)
            i += 2
        } else {
            result.add(c.code)
            i++
        }
    }
    return result
}

private fun codePointString(codePoint: Int): String = if (codePoint < 0x10000) {
    codePoint.toChar().toString()
} else {
    val c = codePoint - 0x10000
    charArrayOf((0xD800 + (c shr 10)).toChar(), (0xDC00 + (c and 0x3FF)).toChar()).concatToString()
}
