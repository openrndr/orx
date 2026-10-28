package org.openrndr.extra.svg

import io.github.oshai.kotlinlogging.KotlinLogging
import org.jsoup.nodes.*
import org.openrndr.extra.composition.*
import org.openrndr.math.*
import org.openrndr.shape.*

private val logger = KotlinLogging.logger {}

internal sealed class SVGElement(element: Element?) {
    var tag: String = element?.tagName() ?: ""
    var id: String = element?.id() ?: ""

    open var style = Style()
    abstract fun handleAttribute(attribute: Attribute)

    // Any element can have a style attribute to pass down properties
    fun styleProperty(key: String, value: String) {
        when (key) {
            Prop.STROKE -> style.stroke = SVGParse.color(value)
            Prop.STROKE_OPACITY -> style.strokeOpacity = SVGParse.number(value)
            Prop.STROKE_WIDTH -> style.strokeWeight = SVGParse.length(value)
            Prop.STROKE_MITERLIMIT -> style.miterLimit = SVGParse.number(value)
            Prop.STROKE_LINECAP -> style.lineCap = SVGParse.lineCap(value)
            Prop.STROKE_LINEJOIN -> style.lineJoin = SVGParse.lineJoin(value)
            Prop.FILL -> style.fill = SVGParse.color(value)
            Prop.FILL_OPACITY -> style.fillOpacity = SVGParse.number(value)
            Prop.OPACITY -> style.opacity = SVGParse.number(value)
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
            else -> logger.warn { "Unknown property: $key" }
        }
    }

    /** Special case of parsing an inline style attribute. */
    fun inlineStyles(attribute: Attribute) {
        attribute.value.split(";").forEach {
            val result = it.split(":").map { s -> s.trim() }

            if (result.size >= 2) {
                styleProperty(result[0], result[1])
            }
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
        this.element.children().forEach { child ->
            when (child.tagName()) {
                "image" -> elements.add(SVGImage(child))
                "text" -> elements.add(SVGText(child, topLevel = true))
                "defs" -> elements.add(SVGDefs(child))
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
 * A run of characters of a text element. A run without a position continues after the previous run.
 */
internal class SVGTextRun(var text: String, val x: Double?, val y: Double?, val dx: Double, val dy: Double) :
    SVGElement(null) {
    override fun handleAttribute(attribute: Attribute) {}
}

/**
 * The position that the first run of a text element starts at
 */
internal class SVGTextPosition(val x: Double?, val y: Double?, val dx: Double, val dy: Double)

/**
 * <text> and <tspan> elements: runs of characters and nested tspan elements
 * @param topLevel true for a text element, which starts at its position (0, 0 by default) and of which the whitespace
 * is collapsed
 * @param inheritedPosition the position of the parent element when it has no characters before this element
 */
internal class SVGText(
    val element: Element,
    topLevel: Boolean,
    inheritedPosition: SVGTextPosition? = null
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

    /** the first value of a list of lengths, x, y, dx and dy can be given for every character */
    private fun firstLength(name: String): Double? =
        element.attr(name).trim().split(Regex("[\\s,]+")).firstOrNull()?.takeIf { it.isNotEmpty() }
            ?.let { SVGParse.length(it).value }

    init {
        element.attributes().forEach {
            if (it.key == Attr.STYLE) {
                inlineStyles(it)
            } else {
                handleAttribute(it)
            }
        }

        val own = SVGTextPosition(firstLength("x"), firstLength("y"), firstLength("dx") ?: 0.0, firstLength("dy") ?: 0.0)
        var pending: SVGTextPosition? = when {
            topLevel -> SVGTextPosition(own.x ?: 0.0, own.y ?: 0.0, own.dx, own.dy)
            own.x != null || own.y != null || own.dx != 0.0 || own.dy != 0.0 -> SVGTextPosition(
                own.x ?: inheritedPosition?.x, own.y ?: inheritedPosition?.y, own.dx, own.dy
            )
            else -> inheritedPosition
        }

        for (node in element.childNodes()) {
            when (node) {
                is org.jsoup.nodes.TextNode -> {
                    val text = node.wholeText.replace(Regex("\\s+"), " ")
                    if (text.isNotEmpty()) {
                        val position = pending
                        elements.add(
                            SVGTextRun(text, position?.x, position?.y, position?.dx ?: 0.0, position?.dy ?: 0.0)
                        )
                        pending = null
                    }
                }

                is Element -> if (node.tagName() == "tspan" || node.tagName() == "textPath") {
                    val tspan = SVGText(node, topLevel = false, inheritedPosition = pending)
                    if (tspan.runs().isNotEmpty()) {
                        pending = null
                    }
                    elements.add(tspan)
                }
            }
        }

        if (topLevel) {
            collapseWhitespace()
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
            } else if (runs[index - 1].text.endsWith(" ") && run.x == null && run.y == null) {
                run.text = run.text.trimStart()
            }
            if (index == runs.lastIndex) {
                run.text = run.text.trimEnd()
            }
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
                            Segment2D(cursor, cp, it).also { _ ->
                                cursor = it
                                prevQuadCtrlPoint = cp
                            }
                        }
                    }

                    "t" -> {
                        points!!.forEach {
                            val cp = 2.0 * cursor - (prevQuadCtrlPoint ?: cursor)
                            Segment2D(cursor, cp, cursor + it).also { _ ->
                                cursor = it
                                prevQuadCtrlPoint = cp
                            }
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