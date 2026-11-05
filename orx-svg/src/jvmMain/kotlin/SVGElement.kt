package org.openrndr.extra.svg

import io.github.oshai.kotlinlogging.KotlinLogging
import org.jsoup.nodes.*
import org.openrndr.extra.composition.*
import org.openrndr.math.*
import org.openrndr.shape.*

private val logger = KotlinLogging.logger {}

internal sealed class SVGElement(element: Element?) {
    /** the element this is read from, for lengths relative to its font size */
    protected val source: Element? = element

    /** the origin of the transform, like transform-origin, null for the origin of the coordinates */
    var transformOrigin: String? = null

    /** the transform of the element around its [transformOrigin] */
    fun effectiveTransform(): org.openrndr.math.Matrix44 {
        val transform = style.transform.value
        val origin = transformOrigin ?: return transform
        val size = SVGParse.viewport.get() ?: Vector2(100.0, 100.0)
        val parts = origin.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        fun component(value: String?, axis: SVGParse.LengthAxis, extent: Double): Double = when (value) {
            null, "center" -> extent / 2.0
            "left", "top" -> 0.0
            "right", "bottom" -> extent
            else -> SVGParse.lengthValue(value, source, axis) ?: 0.0
        }
        // a vertical keyword first is the y component
        val (xValue, yValue) = if (parts.firstOrNull() in setOf("top", "bottom")) parts.getOrNull(1) to parts[0]
            else parts.getOrNull(0) to parts.getOrNull(1)
        val o = Vector2(component(xValue, SVGParse.LengthAxis.X, size.x), component(yValue, SVGParse.LengthAxis.Y, size.y))
        if (o == Vector2.ZERO) return transform
        return org.openrndr.math.transforms.buildTransform { translate(o.x, o.y, 0.0) } * transform *
                org.openrndr.math.transforms.buildTransform { translate(-o.x, -o.y, 0.0) }
    }

    var tag: String = element?.tagName() ?: ""
    var id: String = element?.id() ?: ""

    /** the text of the title element in the element */
    val title: String? = element?.children()?.firstOrNull { it.tagName() == "title" }?.text()

    /** the text of the desc element in the element */
    val description: String? = element?.children()?.firstOrNull { it.tagName() == "desc" }?.text()

    /** the properties that are not inherited but have the value inherit, they take the value of the parent */
    val inheritedProperties = mutableSetOf<AttributeOrPropertyKey>()

    /**
     * Applies the CSS-wide keywords inherit, initial and unset to property [key]
     * @return false when [value] is not one of them
     */
    private fun cssWideKeyword(key: String, value: String): Boolean {
        val keyword = value.trim()
        if (keyword != "inherit" && keyword != "initial" && keyword != "unset") return false
        if (key == Prop.TRANSFORM_ORIGIN) {
            transformOrigin = null
            return true
        }
        val names = if (key == Prop.MARKER) listOf(Prop.MARKER_START, Prop.MARKER_MID, Prop.MARKER_END) else listOf(key)
        for (name in names) {
            val property = AttributeOrPropertyKey.entries.firstOrNull { it.toString() == name } ?: continue
            inheritedProperties.remove(property)
            when {
                // an unset property is inherited or has its initial value, as if it was not given
                keyword == "unset" || (keyword == "inherit" && isInheritedProperty(property)) ->
                    style.properties.remove(property)
                keyword == "initial" -> style[property] = initialValue(property)
                else -> {
                    style.properties.remove(property)
                    inheritedProperties.add(property)
                }
            }
        }
        return true
    }

    open var style = Style()
    abstract fun handleAttribute(attribute: Attribute)

    // Any element can have a style attribute to pass down properties
    fun styleProperty(key: String, value: String) {
        if (cssWideKeyword(key, value)) return
        when (key) {
            Prop.STROKE -> style.stroke = SVGParse.color(value)
            Prop.STROKE_OPACITY -> style.strokeOpacity = SVGParse.number(value)
            Prop.STROKE_WIDTH -> style.strokeWeight = value.trim().let { v ->
                when {
                    v.endsWith("%") -> Length.Percent(v.dropLast(1).toDoubleOrNull() ?: 0.0)
                    else -> Length.Pixels(SVGParse.lengthValue(v, source) ?: 1.0)
                }
            }
            Prop.STROKE_DASHARRAY -> style.strokeDashArray = SVGParse.dashArray(value)
            Prop.STROKE_DASHOFFSET -> style.strokeDashOffset = SVGParse.length(value.trim())
            Prop.STROKE_MITERLIMIT -> style.miterLimit = SVGParse.number(value)
            Prop.STROKE_LINECAP -> style.lineCap = SVGParse.lineCap(value)
            Prop.STROKE_LINEJOIN -> style.lineJoin = SVGParse.lineJoin(value)
            Prop.FILL -> style.fill = SVGParse.color(value)
            Prop.FILL_OPACITY -> style.fillOpacity = SVGParse.number(value)
            Prop.FILL_RULE -> style.fillRule = when (value.trim()) {
                "evenodd" -> org.openrndr.extra.composition.FillRule.EvenOdd
                else -> org.openrndr.extra.composition.FillRule.NonZero
            }
            Prop.OPACITY -> style.opacity = SVGParse.number(value)
            Prop.COLOR -> style.color = SVGParse.color(value)
            Prop.PAINT_ORDER -> style.paintOrder = SVGParse.paintOrder(value)
            Prop.TRANSFORM_ORIGIN -> transformOrigin = value
            Prop.MARKER -> SVGParse.marker(value).let {
                style.markerStart = it
                style.markerMid = it
                style.markerEnd = it
            }
            Prop.MARKER_START -> style.markerStart = SVGParse.marker(value)
            Prop.MARKER_MID -> style.markerMid = SVGParse.marker(value)
            Prop.MARKER_END -> style.markerEnd = SVGParse.marker(value)
            // a transform in CSS or in the style attribute
            "transform" -> style.transform = SVGParse.transform(value)
            Prop.CLIP_PATH -> style.clipPath = SVGParse.clipPath(value)
            Prop.CLIP_RULE -> style.clipRule = when (value.trim()) {
                "evenodd" -> org.openrndr.extra.composition.FillRule.EvenOdd
                else -> org.openrndr.extra.composition.FillRule.NonZero
            }
            Prop.DISPLAY -> style.display = if (value.trim() == "none") Display.None else Display.Inline
            Prop.VISIBILITY -> style.visibility = when (value.trim()) {
                "hidden" -> Visibility.Hidden
                "collapse" -> Visibility.Collapse
                else -> Visibility.Visible
            }
            Prop.VECTOR_EFFECT -> style.vectorEffect =
                if (value.trim() == "non-scaling-stroke") VectorEffect.NonScalingStroke else VectorEffect.None
            Prop.FONT_FAMILY -> style.fontFamily = FontFamily(
                value.split(",").map { it.trim().trim('"', '\'') }.filter { it.isNotEmpty() }
            )
            Prop.FONT_SIZE -> style.fontSize = SVGParse.length(value.trim())
            Prop.FONT_WEIGHT -> style.fontWeight = FontWeight(
                when (val weight = value.trim()) {
                    "normal" -> 400
                    "bold", "bolder" -> 700
                    "lighter" -> 300
                    else -> weight.toIntOrNull() ?: 400
                }
            )
            Prop.FONT_STYLE -> style.fontStyle = when (value.trim()) {
                "italic" -> FontStyle.ITALIC
                "oblique" -> FontStyle.OBLIQUE
                else -> FontStyle.NORMAL
            }
            Prop.TEXT_ANCHOR -> style.textAnchor = when (value.trim()) {
                "middle" -> TextAnchor.MIDDLE
                "end" -> TextAnchor.END
                else -> TextAnchor.START
            }
            Prop.LETTER_SPACING -> style.letterSpacing = spacing(value)
            Prop.WORD_SPACING -> style.wordSpacing = spacing(value)
            // the alignment baseline of an element is taken as the baseline of its text
            Prop.DOMINANT_BASELINE, Prop.ALIGNMENT_BASELINE -> DominantBaseline.of(value)?.let { style.dominantBaseline = it }
                ?: logger.warn { "Unknown baseline: $value" }
            Prop.BASELINE_SHIFT -> style.baselineShift = when (val v = value.trim()) {
                "baseline" -> BaselineShift.NONE
                "sub" -> BaselineShift.Sub
                "super" -> BaselineShift.Super
                else -> BaselineShift.Value(
                    if (v.endsWith("%")) Length.Percent(v.dropLast(1).toDoubleOrNull() ?: 0.0)
                    else Length.Pixels(SVGParse.lengthValue(v, source) ?: 0.0)
                )
            }
            Prop.TEXT_DECORATION -> style.textDecoration = value.trim().split(Regex("\\s+")).let {
                TextDecoration(underline = "underline" in it, overline = "overline" in it, lineThrough = "line-through" in it)
            }
            Prop.MASK -> style.mask = SVGParse.mask(value)
            Prop.FILTER -> style.filter = SVGFilters.filter(value, source)
            // the color space of filters is read with the filter elements
            Prop.COLOR_INTERPOLATION_FILTERS, Prop.FLOOD_COLOR, Prop.FLOOD_OPACITY -> {}
            Prop.MIX_BLEND_MODE -> MixBlendMode.of(value)?.let { style.mixBlendMode = it }
                ?: logger.warn { "Unknown blend mode: $value" }
            Prop.ISOLATION -> style.isolation = if (value.trim() == "isolate") Isolation.ISOLATE else Isolation.AUTO
            // the type of a mask is read with the mask element
            Prop.MASK_TYPE -> {}
            Prop.SHAPE_RENDERING -> style.shapeRendering = when (value.trim()) {
                "optimizeSpeed" -> ShapeRendering.OPTIMIZE_SPEED
                "crispEdges" -> ShapeRendering.CRISP_EDGES
                "geometricPrecision" -> ShapeRendering.GEOMETRIC_PRECISION
                else -> ShapeRendering.AUTO
            }
            else -> logger.warn { "Unknown property: $key" }
        }
    }

    /** a letter or word spacing: normal, a length or a percentage of the font size */
    private fun spacing(value: String): Length = when (val v = value.trim()) {
        "normal" -> Length.Pixels(0.0)
        else -> if (v.endsWith("%")) Length.Percent(v.dropLast(1).toDoubleOrNull() ?: 0.0)
            else Length.Pixels(SVGParse.lengthValue(v, source) ?: 0.0)
    }

    /** Special case of parsing an inline style attribute. */
    fun inlineStyles(attribute: Attribute) {
        for ((name, value) in SVGStyleSheet.declarations(attribute.value)) {
            styleProperty(name, value)
        }
    }
}

/** <svg> element */
internal class SVGSVGElement(element: Element) : SVGGroup(element) {
    var documentStyle: DocumentStyle = DocumentStyle()

    init {
        documentStyle.viewBox = SVGParse.viewBox(this.element)
        documentStyle.preserveAspectRatio = SVGParse.preserveAspectRatio(this.element)
    }

    var bounds = SVGParse.bounds(this.element)
}

/** <g> element but practically works with anything that has child elements */
internal open class SVGGroup(val element: Element, val elements: MutableList<SVGElement> = mutableListOf()) :
    SVGElement(element) {
    /** the child elements that are read, all of them */
    protected open fun children(): List<Element> = element.children()


    init {
        this.element.attributes().forEach {
            if (it.key == Attr.STYLE) {
                inlineStyles(it)
            } else {
                handleAttribute(it)
            }
        }

        handleChildren()
    }

    private fun handleChildren() {
        children().forEach { child ->
            when (child.tagName()) {
                "image" -> elements.add(SVGImage(child))
                "text" -> elements.add(SVGText(child, topLevel = true))
                "defs" -> elements.add(SVGDefs(child))
                // symbols are drawn where use elements refer to them
                "symbol" -> elements.add(SVGSymbol(child))
                "use" -> elements.add(SVGUse(child))
                "switch" -> elements.add(SVGSwitch(child))
                "svg" -> elements.add(SVGNestedSVG(child))
                // gradients are read before the elements are, see SVGGradients
                // patterns are drawn where they are referred to, see SVGGradients
                // clip paths are applied where they are referred to, see SVGGradients
                // masks are applied where they are referred to, see SVGGradients
                // filters are applied where they are referred to, see SVGFilters
                "linearGradient", "radialGradient", "pattern", "clipPath", "marker", "mask", "filter" -> {}
                // style elements are applied before the elements are read, see SVGStyleSheet
                "style", "title", "desc", "metadata" -> {}
                in Tag.graphicsList -> elements.add(SVGPath(child))
                else -> elements.add(SVGGroup(child))
            }
        }
    }

    override fun handleAttribute(attribute: Attribute) {
        when (attribute.key) {
            // Attributes can also be style properties, in which case they're passed on
            in Prop.list -> styleProperty(attribute.key, attribute.value)
            Attr.TRANSFORM -> style.transform = SVGParse.transform(this.element)
        }
    }
}

internal class Command(val op: String, vararg val operands: Double) {
    fun asVectorList(): List<Vector2>? {
        return if (operands.size % 2 == 0) {
            operands.asList().chunked(2) { Vector2(it[0], it[1]) }
        } else {
            null
        }
    }
}

// For evaluating elliptical arc arguments according to the SVG spec
internal fun Double.toBoolean(): Boolean? = when (this) {
    0.0 -> false
    1.0 -> true
    else -> null
}

internal class SVGImage(val element: Element? = null) : SVGElement(element) {

    /** the value of a length attribute in pixels, or null when it is not given or auto */
    private fun lengthAttribute(name: String): Double? =
        element?.attr(name)?.trim()?.takeIf { it.isNotEmpty() && it != "auto" }?.let { SVGParse.length(it).value }

    val x = lengthAttribute("x") ?: 0.0
    val y = lengthAttribute("y") ?: 0.0

    /** the width of the viewport of the image, null when the width of the image is used */
    val width = lengthAttribute("width")

    /** the height of the viewport of the image, null when the height of the image is used */
    val height = lengthAttribute("height")

    /** the reference to the image: the href of SVG 2 or the xlink:href of SVG 1.1 */
    val href: String? = listOf("href", "xlink:href").firstNotNullOfOrNull { name ->
        element?.attr(name)?.trim()?.takeIf { it.isNotEmpty() }
    }

    /** how the image is fitted into its viewport */
    val aspectRatio: AspectRatio =
        element?.let { SVGParse.preserveAspectRatio(it) } ?: AspectRatio(Align.X_MID_Y_MID, MeetOrSlice.MEET)

    override fun handleAttribute(attribute: Attribute) {
        if (this.element is Element) {
            when (attribute.key) {
                // Attributes can also be style properties, in which case they're passed on
                in Prop.list -> styleProperty(attribute.key, attribute.value)
                Attr.TRANSFORM -> style.transform = SVGParse.transform(this.element)
            }
        }
    }

    init {
        if (element != null) {
            // style properties, like opacity, can be given as attributes or in the style attribute
            element.attributes().forEach {
                if (it.key == Attr.STYLE) {
                    inlineStyles(it)
                } else {
                    handleAttribute(it)
                }
            }
            style.transform = SVGParse.transform(this.element)
        }
    }
}


/**
 * <defs> element, its contents are not rendered but can be referenced, for example by textPath elements
 */
internal class SVGDefs(element: Element) : SVGGroup(element)

/**
 * <symbol> element, its contents are rendered where a use element refers to it, fitted into the viewport of the use
 * element by its view box
 */
internal class SVGSymbol(element: Element) : SVGGroup(element) {
    val viewBox: org.openrndr.shape.Rectangle? = (SVGParse.viewBox(element) as? ViewBox.Value)?.value
    val aspectRatio: AspectRatio = SVGParse.preserveAspectRatio(element)
}

/**
 * A nested <svg> element: its content is placed in a viewport at [x], [y] of [width] by [height], fitted by its view
 * box, and clipped to the viewport unless its overflow is visible
 */
internal class SVGNestedSVG(element: Element) : SVGGroup(element) {
    val x = SVGParse.lengthValue(element.attr("x"), element, SVGParse.LengthAxis.X) ?: 0.0
    val y = SVGParse.lengthValue(element.attr("y"), element, SVGParse.LengthAxis.Y) ?: 0.0
    val width = SVGParse.lengthValue(element.attr("width").ifBlank { "100%" }, element, SVGParse.LengthAxis.X) ?: 0.0
    val height = SVGParse.lengthValue(element.attr("height").ifBlank { "100%" }, element, SVGParse.LengthAxis.Y) ?: 0.0
    val viewBox: org.openrndr.shape.Rectangle? = (SVGParse.viewBox(element) as? ViewBox.Value)?.value
    val aspectRatio: AspectRatio = SVGParse.preserveAspectRatio(element)
    val clipped = element.attr("overflow").trim().let { it != "visible" && it != "auto" }
}

/**
 * <switch> element: only its first child element is rendered. Conditional attributes are not evaluated, the first
 * child is taken to apply.
 */
internal class SVGSwitch(element: Element) : SVGGroup(element) {
    override fun children(): List<Element> = element.children().take(1)
}

/**
 * <use> element: renders the element that [href] refers to, moved by [x] and [y]; a symbol is fitted into [width]
 * and [height]
 */
internal class SVGUse(val element: Element) : SVGElement(element) {
    val href: String? = listOf("href", "xlink:href").firstNotNullOfOrNull { name ->
        element.attr(name).trim().takeIf { it.startsWith("#") }?.removePrefix("#")
    }

    private fun length(name: String): Double? =
        element.attr(name).trim().takeIf { it.isNotEmpty() && !it.endsWith("%") }?.let { SVGParse.length(it).value }

    val x = length("x") ?: 0.0
    val y = length("y") ?: 0.0
    val width = length("width")
    val height = length("height")

    override fun handleAttribute(attribute: Attribute) {
        when (attribute.key) {
            in Prop.list -> styleProperty(attribute.key, attribute.value)
        }
    }

    init {
        element.attributes().forEach {
            if (it.key == Attr.STYLE) inlineStyles(it) else handleAttribute(it)
        }
        style.transform = SVGParse.transform(element)
    }
}

/**
 * A part of a run of characters that is placed as a whole: at [x] and [y] when they are given, otherwise after the
 * text before it, moved by [dx] and [dy], its glyphs turned by [rotate] degrees
 */
internal class SVGTextPiece(
    val text: String,
    val x: Double?,
    val y: Double?,
    val dx: Double,
    val dy: Double,
    val rotate: Double
)

/**
 * The x, y, dx, dy and rotate values of the characters of a text, tspan or textPath element, and of the elements it
 * is in. A value of an element applies to its characters in order, the values of the innermost element that has one
 * apply; the last rotation applies to all characters after it.
 */
internal class SVGTextPositions(
    private val x: List<Double>,
    private val y: List<Double>,
    private val dx: List<Double>,
    private val dy: List<Double>,
    private val rotate: List<Double>,
    private val parent: SVGTextPositions?
) {
    /** the number of characters of the element that have been given their values */
    private var index = 0

    val hasPosition: Boolean get() = x.isNotEmpty() || y.isNotEmpty()

    /** the values of the next character, every character takes values once */
    fun next(): SVGTextPiece {
        var x: Double? = null
        var y: Double? = null
        var dx: Double? = null
        var dy: Double? = null
        var rotate: Double? = null
        var level: SVGTextPositions? = this
        while (level != null) {
            val i = level.index
            if (x == null) x = level.x.getOrNull(i)
            if (y == null) y = level.y.getOrNull(i)
            if (dx == null) dx = level.dx.getOrNull(i)
            if (dy == null) dy = level.dy.getOrNull(i)
            if (rotate == null) rotate = level.rotate.getOrNull(i) ?: level.rotate.lastOrNull()
            level.index++
            level = level.parent
        }
        return SVGTextPiece("", x, y, dx ?: 0.0, dy ?: 0.0, rotate ?: 0.0)
    }
}

/**
 * A run of characters of a text element. Its [pieces] are known once the whitespace of the text element is
 * collapsed.
 * @param startsPositioned true when the run is the first of an element with an x or y position
 */
internal class SVGTextRun(var text: String, val positions: SVGTextPositions, val startsPositioned: Boolean) :
    SVGElement(null) {
    var pieces: List<SVGTextPiece> = emptyList()

    override fun handleAttribute(attribute: Attribute) {}
}

/**
 * <text>, <tspan> and <textPath> elements: runs of characters and nested tspan elements
 * @param topLevel true for a text element, which starts at its position (0, 0 by default) and of which the whitespace
 * is collapsed
 * @param parentPositions the positions of the characters of the parent element
 */
internal class SVGText(
    val element: Element,
    topLevel: Boolean,
    parentPositions: SVGTextPositions? = null
) : SVGElement(element) {
    val elements = mutableListOf<SVGElement>()

    /** for a textPath element: the reference to the path that the text is placed along */
    val pathHref: String? = if (element.tagName() == "textPath") {
        listOf("href", "xlink:href").firstNotNullOfOrNull { name ->
            element.attr(name).trim().takeIf { it.isNotEmpty() }
        } ?: ""
    } else {
        null
    }

    /** for a textPath element: the distance along the path where the text starts, a length or a percentage */
    val startOffset: String = element.attr("startOffset").trim()

    override fun handleAttribute(attribute: Attribute) {
        when (attribute.key) {
            in Prop.list -> styleProperty(attribute.key, attribute.value)
            Attr.TRANSFORM -> style.transform = SVGParse.transform(this.element)
        }
    }

    /** the values of a list attribute, x, y, dx, dy and rotate can be given for every character */
    private fun values(name: String, axis: SVGParse.LengthAxis?): List<Double> =
        // a textPath element has no positions of its own
        if (element.tagName() == "textPath") emptyList() else element.attr(name).trim().split(Regex("[\\s,]+")).filter { it.isNotEmpty() }.mapNotNull {
            if (axis == null) it.toDoubleOrNull() else SVGParse.lengthValue(it, element, axis)
        }

    private val positions = SVGTextPositions(
        values("x", SVGParse.LengthAxis.X), values("y", SVGParse.LengthAxis.Y),
        values("dx", SVGParse.LengthAxis.X), values("dy", SVGParse.LengthAxis.Y), values("rotate", null),
        parentPositions
    )

    init {
        element.attributes().forEach {
            if (it.key == Attr.STYLE) {
                inlineStyles(it)
            } else {
                handleAttribute(it)
            }
        }

        var first = true
        for (node in element.childNodes()) {
            when (node) {
                is org.jsoup.nodes.TextNode -> {
                    val text = node.wholeText.replace(Regex("\\s+"), " ")
                    if (text.isNotEmpty()) {
                        elements.add(SVGTextRun(text, positions, first && positions.hasPosition))
                        first = false
                    }
                }

                is Element -> if (node.tagName() == "tspan" || node.tagName() == "textPath") {
                    val tspan = SVGText(node, topLevel = false, parentPositions = positions)
                    if (tspan.runs().isNotEmpty()) {
                        first = false
                    }
                    elements.add(tspan)
                }
            }
        }

        if (topLevel) {
            collapseWhitespace()
            placeCharacters()
        }
    }

    /** the runs of this element and its tspan elements, in order */
    fun runs(): List<SVGTextRun> = elements.flatMap {
        when (it) {
            is SVGTextRun -> listOf(it)
            is SVGText -> it.runs()
            else -> emptyList()
        }
    }

    /**
     * Removes whitespace at the start and the end of the text, and spaces that follow a space in the previous run
     */
    private fun collapseWhitespace() {
        val runs = runs()
        for ((index, run) in runs.withIndex()) {
            if (index == 0) {
                run.text = run.text.trimStart()
            } else if (runs[index - 1].text.endsWith(" ") && !run.startsPositioned) {
                run.text = run.text.trimStart()
            }
            if (index == runs.lastIndex) {
                run.text = run.text.trimEnd()
            }
        }
    }

    /**
     * Gives the characters of the runs their values, in order, and splits the runs into pieces where a character is
     * positioned or turned differently. The text starts at (0, 0) when it has no position.
     */
    private fun placeCharacters() {
        var firstPiece = true
        for (run in runs()) {
            val pieces = mutableListOf<SVGTextPiece>()
            var current: SVGTextPiece? = null
            val text = StringBuilder()
            fun flush() {
                current?.let { pieces.add(SVGTextPiece(text.toString(), it.x, it.y, it.dx, it.dy, it.rotate)) }
                text.clear()
            }
            var offset = 0
            while (offset < run.text.length) {
                val length = Character.charCount(run.text.codePointAt(offset))
                var values = run.positions.next()
                if (firstPiece) {
                    values = SVGTextPiece("", values.x ?: 0.0, values.y ?: 0.0, values.dx, values.dy, values.rotate)
                    firstPiece = false
                }
                val c = current
                if (c == null || values.x != null || values.y != null || values.dx != 0.0 || values.dy != 0.0 ||
                    values.rotate != c.rotate
                ) {
                    flush()
                    current = values
                }
                text.append(run.text, offset, offset + length)
                offset += length
            }
            flush()
            run.pieces = pieces
        }
    }
}

internal class SVGPath(val element: Element? = null) : SVGElement(element) {
    val commands = mutableListOf<Command>()

    private fun compounds(): List<SVGPath> {
        val compounds = mutableListOf<SVGPath>()
        val compoundIndices = mutableListOf<Int>()

        commands.forEachIndexed { index, it ->
            if (it.op == "M" || it.op == "m") {
                compoundIndices.add(index)
            }
        }

        compoundIndices.forEachIndexed { index, _ ->
            val cs = compoundIndices[index]
            val ce = if (index + 1 < compoundIndices.size) (compoundIndices[index + 1]) else commands.size

            // TODO: We shouldn't be making new SVGPaths without Elements to provide.
            // Then we could make SVGPath's constructor non-nullable
            val path = SVGPath()
            path.commands.addAll(commands.subList(cs, ce))

            compounds.add(path)
        }
        return compounds
    }

    fun shape(): Shape {
        var cursor = Vector2.ZERO
        var anchor = Vector2.ZERO
        // Still problematic
        var prevCubicCtrlPoint: Vector2? = null
        var prevQuadCtrlPoint: Vector2? = null

        val contours = compounds().map { compound ->
            val segments = mutableListOf<Segment2D>()
            var closed = false
            // If an argument is invalid, an error is logged,
            // further interpreting is stopped and compound is returned as-is.
            compound.commands.forEach { command ->

                if (command.op !in listOf("z", "Z") && command.operands.isEmpty()) {
                    logger.error { "Invalid amount of arguments provided for: ${command.op}" }
                    return@forEach
                }

                val points = command.asVectorList()

                // TODO: Rethink this check
                if (points == null && command.op.lowercase() !in listOf("a", "h", "v")) {
                    logger.error { "Invalid amount of arguments provided for: ${command.op}" }
                    return@forEach
                }

                // smooth curves only reflect the control point of a preceding curve of the same kind
                if (command.op.lowercase() !in listOf("c", "s")) prevCubicCtrlPoint = null
                if (command.op.lowercase() !in listOf("q", "t")) prevQuadCtrlPoint = null

                when (command.op) {
                    "A", "a" -> {
                        // If size == step, only the last window can be partial
                        // Special case as it also has boolean values
                        val contours = command.operands.toList().windowed(7, 7, true).map m@{
                            if (it.size != 7) {
                                logger.error { "Invalid amount of arguments provided for: ${command.op}" }
                                return@forEach
                            } else {
                                val rx = it[0]
                                val ry = it[1]
                                val xAxisRot = it[2]
                                val largeArcFlag = it[3].toBoolean()
                                val sweepFlag = it[4].toBoolean()

                                if (largeArcFlag == null || sweepFlag == null || rx == 0.0 || ry == 0.0) {
                                    logger.error { "Invalid values provided for: ${command.op}" }
                                    return@forEach
                                }

                                val end = Vector2(it[5], it[6]).let { v ->
                                    if (command.op == "a") {
                                        v + cursor
                                    } else {
                                        v
                                    }
                                }

                                contour {
                                    moveTo(cursor)
                                    arcTo(rx, ry, xAxisRot, largeArcFlag, sweepFlag, end)
                                    cursor = end
                                }
                            }
                        }

                        // I don't know why we can't just have segments from the above map,
                        // but this is the only way this works.
                        segments += contours.flatMap { it.segments }
                    }

                    "M" -> {
                        // TODO: Log an error when this nulls
                        cursor = points!!.firstOrNull() ?: return@forEach
                        anchor = cursor

                        // Following points are implicit lineto arguments
                        segments += points.drop(1).map {
                            Segment2D(cursor, it).apply {
                                cursor = it
                            }
                        }
                    }

                    "m" -> {
                        // TODO: Log an error when this nulls
                        cursor += points!!.firstOrNull() ?: return@forEach
                        anchor = cursor

                        // Following points are implicit lineto arguments
                        segments += points.drop(1).map {
                            Segment2D(cursor, cursor + it).apply {
                                cursor += it
                            }
                        }
                    }

                    "L" -> {
                        segments += points!!.map {
                            Segment2D(cursor, it).apply {
                                cursor = it
                            }
                        }
                    }

                    "l" -> {
                        segments += points!!.map {
                            Segment2D(cursor, cursor + it).apply {
                                cursor += it
                            }
                        }
                    }

                    "H" -> {
                        segments += command.operands.map {
                            val target = Vector2(it, cursor.y)
                            Segment2D(cursor, target).apply {
                                cursor = target
                            }
                        }
                    }

                    "h" -> {
                        segments += command.operands.map {
                            val target = cursor + Vector2(it, 0.0)
                            Segment2D(cursor, target).apply {
                                cursor = target
                            }
                        }
                    }

                    "V" -> {
                        segments += command.operands.map {
                            val target = Vector2(cursor.x, it)
                            Segment2D(cursor, target).apply {
                                cursor = target
                            }
                        }
                    }

                    "v" -> {
                        segments += command.operands.map {
                            val target = cursor + Vector2(0.0, it)
                            Segment2D(cursor, target).apply {
                                cursor = target
                            }
                        }
                    }

                    "C" -> {
                        segments += points!!.windowed(3, 3, true).map {
                            if (it.size != 3) {
                                logger.error { "Invalid amount of arguments provided for: ${command.op}" }
                                return@forEach
                            } else {
                                val (cp1, cp2, target) = it
                                Segment2D(cursor, cp1, cp2, target).also {
                                    cursor = target
                                    prevCubicCtrlPoint = cp2
                                }
                            }
                        }
                    }

                    "c" -> {
                        segments += points!!.windowed(3, 3, true).map {
                            if (it.size != 3) {
                                logger.error { "Invalid amount of arguments provided for: ${command.op}" }
                                return@forEach
                            } else {
                                val (cp1, cp2, target) = it.map { v -> cursor + v }
                                Segment2D(cursor, cp1, cp2, target).apply {
                                    cursor = target
                                    prevCubicCtrlPoint = cp2
                                }
                            }
                        }
                    }

                    "S" -> {
                        segments += points!!.windowed(2, 2, true).map {
                            if (it.size != 2) {
                                logger.error { "Invalid amount of arguments provided for: ${command.op}" }
                                return@forEach
                            } else {
                                val cp1 = 2.0 * cursor - (prevCubicCtrlPoint ?: cursor)
                                val (cp2, target) = it
                                Segment2D(cursor, cp1, cp2, target).also {
                                    cursor = target
                                    prevCubicCtrlPoint = cp2
                                }
                            }
                        }
                    }

                    "s" -> {
                        segments += points!!.windowed(2, 2, true).map {
                            if (it.size != 2) {
                                logger.error { "Invalid amount of arguments provided for: ${command.op}" }
                                return@forEach
                            } else {
                                val cp1 = 2.0 * cursor - (prevCubicCtrlPoint ?: cursor)
                                val (cp2, target) = it.map { v -> cursor + v }
                                Segment2D(cursor, cp1, cp2, target).also {
                                    cursor = target
                                    prevCubicCtrlPoint = cp2
                                }
                            }
                        }
                    }

                    "Q" -> {
                        segments += points!!.windowed(2, 2, true).map {
                            if (it.size != 2) {
                                logger.error { "Invalid amount of arguments provided for: ${command.op}" }
                                return@forEach
                            } else {
                                val (cp, target) = it
                                Segment2D(cursor, cp, target).also {
                                    cursor = target
                                    prevQuadCtrlPoint = cp
                                }
                            }
                        }
                    }

                    "q" -> {
                        segments += points!!.windowed(2, 2, true).map {
                            if (it.size != 2) {
                                logger.error { "Invalid amount of arguments provided for: ${command.op}" }
                                return@forEach
                            } else {
                                val (cp, target) = it.map { v -> cursor + v }
                                Segment2D(cursor, cp, target).also {
                                    cursor = target
                                    prevQuadCtrlPoint = cp
                                }
                            }
                        }
                    }

                    "T" -> {
                        points!!.forEach {
                            val cp = 2.0 * cursor - (prevQuadCtrlPoint ?: cursor)
                            segments += Segment2D(cursor, cp, it)
                            cursor = it
                            prevQuadCtrlPoint = cp
                        }
                    }

                    "t" -> {
                        points!!.forEach {
                            val cp = 2.0 * cursor - (prevQuadCtrlPoint ?: cursor)
                            val target = cursor + it
                            segments += Segment2D(cursor, cp, target)
                            cursor = target
                            prevQuadCtrlPoint = cp
                        }
                    }

                    "Z", "z" -> {
                        if ((cursor - anchor).length >= 0.001) {
                            segments += Segment2D(cursor, anchor)
                        }
                        cursor = anchor
                        closed = true
                    }

                    else -> {
                        // The spec declares we should still attempt to render
                        // the path up until the erroneous command as to visually
                        // signal the user where the error occurred.
                        logger.error { "Invalid path operator: ${command.op}" }
                        return@forEach
                    }
                }
            }
            ShapeContour(segments, closed, YPolarity.CW_NEGATIVE_Y)
        }
        return Shape(contours)
    }

    override fun handleAttribute(attribute: Attribute) {
        if (this.element is Element) {
            when (attribute.key) {
                // Attributes can also be style properties, in which case they're passed on
                in Prop.list -> styleProperty(attribute.key, attribute.value)
                Attr.TRANSFORM -> style.transform = SVGParse.transform(this.element)
                Attr.PATH_LENGTH -> style.pathLength = PathLength(attribute.value.trim().toDoubleOrNull())
            }
        }
    }

    init {
        if (this.element is Element) {
            commands += when (tag) {
                Tag.PATH -> SVGParse.path(this.element)
                Tag.LINE -> SVGParse.line(this.element)
                Tag.RECT -> SVGParse.rectangle(this.element)
                Tag.ELLIPSE -> SVGParse.ellipse(this.element)
                Tag.CIRCLE -> SVGParse.circle(this.element)
                Tag.POLYGON -> SVGParse.polygon(this.element)
                Tag.POLYLINE -> SVGParse.polyline(this.element)
                else -> emptyList()
            }

            element.attributes().forEach {
                if (it.key == Attr.STYLE) {
                    inlineStyles(it)
                } else {
                    handleAttribute(it)
                }
            }
        }
    }
}