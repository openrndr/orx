package org.openrndr.panel.layout

import org.openrndr.panel.elements.Element
import org.openrndr.panel.elements.TextNode
import org.openrndr.panel.style.*
import org.openrndr.shape.Rectangle
import kotlin.math.ceil
import kotlin.math.max

/** the length in pixels, with percentages taken of [reference]; 0 for other kinds of lengths */
private fun LinearDimension.resolve(reference: Double): Double = when (this) {
    is LinearDimension.PX -> value
    is LinearDimension.Percent -> reference * (value / 100.0)
    else -> 0.0
}

private val StyleSheet.grow: Double
    get() = (flexGrow as FlexGrow.Ratio).value

private val StyleSheet.shrink: Double
    get() = (flexShrink as FlexGrow.Ratio).value

/**
 * Computes the styles of a tree of elements and lays it out.
 *
 * Layout happens in two kinds of steps. Measuring determines the size an element wants for a given containing size,
 * it does not change any state, its results are cached for the duration of a [layout] pass. Arranging sets the
 * sizes and positions of the children of an element, using the measured sizes. Every element in the flow is sized and
 * positioned by its parent, fixed and absolute elements size and position themselves.
 *
 * Sizes are border-box sizes: they include the padding but not the margins of an element. Percentages are taken of
 * the content size of the containing element, margins are always taken of its width.
 */
class Layouter {
    val styleSheets = ArrayList<StyleSheet>()
    val blockLike = setOf(Display.BLOCK, Display.FLEX, Display.GRID)
    val manualPosition = setOf(Position.FIXED, Position.ABSOLUTE)

    private data class WidthKey(val element: Element, val containerWidth: Double)
    private data class HeightKey(
        val element: Element,
        val width: Double,
        val containerWidth: Double,
        val containerHeight: Double
    )

    private val widthCache = HashMap<WidthKey, Double>()
    private val heightCache = HashMap<HeightKey, Double>()

    /**
     * The children of [element] that take part in its flow layout: displayed as block, flex or grid, and not
     * positioned manually.
     */
    private fun inFlowChildren(element: Element): List<Element> = element.children.filter {
        it.computedStyle.display in blockLike && it.computedStyle.position !in manualPosition
    }

    private val Element.contentWidth: Double
        get() = layout.screenWidth - computedStyle.effectivePaddingWidth

    private val Element.contentHeight: Double
        get() = layout.screenHeight - computedStyle.effectivePaddingHeight

    private val Element.containerWidth: Double
        get() = parent?.contentWidth ?: 0.0

    private val Element.containerHeight: Double
        get() = parent?.contentHeight ?: 0.0

    fun marginTop(element: Element) = element.computedStyle.marginTop.resolve(element.containerWidth)
    fun marginBottom(element: Element) = element.computedStyle.marginBottom.resolve(element.containerWidth)
    fun marginLeft(element: Element) = element.computedStyle.marginLeft.resolve(element.containerWidth)
    fun marginRight(element: Element) = element.computedStyle.marginRight.resolve(element.containerWidth)

    private fun horizontalMargins(element: Element, containerWidth: Double) =
        element.computedStyle.let { it.marginLeft.resolve(containerWidth) + it.marginRight.resolve(containerWidth) }

    private fun verticalMargins(element: Element, containerWidth: Double) =
        element.computedStyle.let { it.marginTop.resolve(containerWidth) + it.marginBottom.resolve(containerWidth) }

    //<editor-fold desc="Measuring">
    /**
     * The width of [element].
     * @param containerWidth the content width of the containing element: the reference for percentages, and the
     * width available to elements with an auto width
     */
    private fun measureWidth(element: Element, containerWidth: Double): Double =
        widthCache.getOrPut(WidthKey(element, containerWidth)) {
            val cs = element.computedStyle
            if (cs.display == Display.NONE) {
                0.0
            } else when (val width = cs.width) {
                is LinearDimension.PX -> width.value
                is LinearDimension.Percent ->
                    containerWidth * (width.value / 100.0) - horizontalMargins(element, containerWidth)

                is LinearDimension.Auto -> {
                    val available = containerWidth - horizontalMargins(element, containerWidth) - cs.effectivePaddingWidth
                    val contentWidth = when (element) {
                        // text takes its natural width, it wraps when that is not available. The text writer
                        // only keeps words on a line that end before its edge, hence the extra pixel.
                        is TextNode -> minOf(ceil(element.textWidth()) + 1.0, max(0.0, available))
                        else -> element.widthHint ?: naturalContentWidth(element, available)
                    }
                    contentWidth + cs.effectivePaddingWidth
                }

                // the height of an element depends on its width, it is not known here
                is LinearDimension.Calculate -> width.function(CalculateContext(null, null))
                else -> error("width $width is not supported")
            }
        }

    /**
     * The height of [element] when it is [width] wide.
     * @param containerWidth the content width of the containing element, the reference for margins
     * @param containerHeight the content height of the containing element: the reference for percentages, and the
     * height available to elements with an auto height
     */
    private fun measureHeight(element: Element, width: Double, containerWidth: Double, containerHeight: Double): Double =
        heightCache.getOrPut(HeightKey(element, width, containerWidth, containerHeight)) {
            val cs = element.computedStyle
            if (cs.display == Display.NONE) {
                0.0
            } else if (element is TextNode) {
                element.sizeHint(width).height
            } else when (val height = cs.height) {
                is LinearDimension.PX -> height.value
                is LinearDimension.Percent ->
                    containerHeight * (height.value / 100.0) - verticalMargins(element, containerWidth)

                is LinearDimension.Auto -> {
                    val available = containerHeight - verticalMargins(element, containerWidth) - cs.effectivePaddingHeight
                    (element.heightHint ?: naturalContentHeight(element, width - cs.effectivePaddingWidth, available)) +
                            cs.effectivePaddingHeight
                }

                is LinearDimension.Calculate -> height.function(CalculateContext(width, null))
                else -> error("height $height is not supported")
            }
        }

    private fun outerWidth(element: Element, containerWidth: Double) =
        measureWidth(element, containerWidth) + horizontalMargins(element, containerWidth)

    private fun outerHeight(element: Element, width: Double, containerWidth: Double, containerHeight: Double) =
        measureHeight(element, width, containerWidth, containerHeight) + verticalMargins(element, containerWidth)

    /**
     * The width the in-flow children of [element] need when [contentWidth] is available.
     */
    private fun naturalContentWidth(element: Element, contentWidth: Double): Double {
        val cs = element.computedStyle
        val children = inFlowChildren(element)
        return when {
            cs.display == Display.GRID -> contentWidth
            cs.display == Display.FLEX && cs.flexDirection == FlexDirection.Row ->
                children.sumOf { outerWidth(it, contentWidth) } + gaps(children, cs.columnGap.inPixels(contentWidth))

            else -> children.maxOfOrNull { outerWidth(it, contentWidth) } ?: 0.0
        }
    }

    /**
     * The height the in-flow children of [element] need when the element's content is [contentWidth] wide.
     */
    private fun naturalContentHeight(element: Element, contentWidth: Double, contentHeight: Double): Double {
        val cs = element.computedStyle
        val children = inFlowChildren(element)
        return when {
            cs.display == Display.GRID -> {
                val gap = cs.rowGap.inPixels(contentHeight)
                // fractional rows take no space when the height is not known
                resolveGridTemplate(cs.gridTemplateRows, "gridTemplateRows", gap, 0.0).sum() +
                        gap * max(0, (cs.gridTemplateRows as GridTemplate.LengthList).value.size - 1)
            }

            cs.display == Display.FLEX && cs.flexDirection == FlexDirection.Row -> {
                val widths = flexSizes(element, children, true, contentWidth, contentHeight)
                children.withIndex().maxOfOrNull { (i, child) ->
                    outerHeight(child, widths[i], contentWidth, contentHeight)
                } ?: 0.0
            }

            else -> {
                val gap = if (cs.display == Display.FLEX) cs.rowGap.inPixels(contentHeight) else 0.0
                children.sumOf { outerHeight(it, measureWidth(it, contentWidth), contentWidth, contentHeight) } +
                        gaps(children, gap)
            }
        }
    }

    private fun gaps(children: List<Element>, gap: Double) = gap * max(0, children.size - 1)

    /**
     * The sizes of the [children] of the flex container [element] along its main axis: their measured sizes, grown
     * to fill the remaining space using flex-grow, or shrunk to fit using flex-shrink weighted by their sizes.
     * @param horizontal true for a row, false for a column
     * @param contentWidth the content width of [element]
     * @param contentHeight the content height of [element]
     */
    private fun flexSizes(
        element: Element,
        children: List<Element>,
        horizontal: Boolean,
        contentWidth: Double,
        contentHeight: Double
    ): List<Double> {
        val cs = element.computedStyle
        val sizes = children.map {
            if (horizontal) {
                measureWidth(it, contentWidth)
            } else {
                measureHeight(it, measureWidth(it, contentWidth), contentWidth, contentHeight)
            }
        }
        val available = if (horizontal) contentWidth else contentHeight
        val gap = if (horizontal) cs.columnGap.inPixels(contentWidth) else cs.rowGap.inPixels(contentHeight)
        val margins = children.sumOf {
            if (horizontal) horizontalMargins(it, contentWidth) else verticalMargins(it, contentWidth)
        }
        val remainder = available - sizes.sum() - margins - gaps(children, gap)

        val weights = when {
            remainder > 0.0 -> children.map { it.computedStyle.grow }
            remainder < 0.0 -> children.mapIndexed { i, child -> child.computedStyle.shrink * sizes[i] }
            else -> return sizes
        }
        val totalWeight = weights.sum()
        return if (totalWeight > 0.0) {
            sizes.mapIndexed { i, size -> size + (weights[i] / totalWeight) * remainder }
        } else {
            sizes
        }
    }
    //</editor-fold>

    //<editor-fold desc="Arranging">
    /**
     * Sizes and positions the in-flow children of [element] inside its content area.
     */
    private fun arrangeChildren(element: Element) {
        val cs = element.computedStyle
        when (cs.display) {
            Display.FLEX -> when (cs.flexDirection) {
                FlexDirection.Row -> arrangeFlex(element, horizontal = true)
                FlexDirection.Column -> arrangeFlex(element, horizontal = false)
                else -> {}
            }

            Display.GRID -> arrangeGrid(element)
            else -> arrangeBlock(element)
        }
    }

    /**
     * Places the in-flow children of a flex container next to each other along its main axis.
     * @param horizontal true for a row, false for a column
     */
    private fun arrangeFlex(element: Element, horizontal: Boolean) {
        val cs = element.computedStyle
        val children = inFlowChildren(element)
        val contentWidth = element.contentWidth
        val contentHeight = element.contentHeight
        val sizes = flexSizes(element, children, horizontal, contentWidth, contentHeight)
        val gap = if (horizontal) cs.columnGap.inPixels(contentWidth) else cs.rowGap.inPixels(contentHeight)

        // the position along the main axis
        var position = if (horizontal) {
            element.layout.screenX + cs.effectivePaddingLeft
        } else {
            element.layout.screenY + cs.effectivePaddingTop - element.scrollTop
        }
        for ((i, child) in children.withIndex()) {
            val layout = child.layout
            val ccs = child.computedStyle
            val marginLeft = ccs.marginLeft.resolve(contentWidth)
            val marginTop = ccs.marginTop.resolve(contentWidth)
            if (horizontal) {
                layout.screenX = position + marginLeft
                layout.screenY = element.layout.screenY + cs.effectivePaddingTop - element.scrollTop + marginTop
                layout.screenWidth = sizes[i]
                layout.screenHeight = measureHeight(child, sizes[i], contentWidth, contentHeight)
                layout.growWidth = sizes[i] - measureWidth(child, contentWidth)
                position += sizes[i] + horizontalMargins(child, contentWidth) + gap
            } else {
                layout.screenX = element.layout.screenX + cs.effectivePaddingLeft + marginLeft
                layout.screenY = position + marginTop
                layout.screenWidth = measureWidth(child, contentWidth)
                layout.screenHeight = sizes[i]
                layout.growHeight = sizes[i] - measureHeight(child, layout.screenWidth, contentWidth, contentHeight)
                position += sizes[i] + verticalMargins(child, contentWidth) + gap
            }
        }
    }

    /**
     * Places the in-flow children of [element] below each other.
     */
    private fun arrangeBlock(element: Element) {
        val cs = element.computedStyle
        val contentWidth = element.contentWidth
        val contentHeight = element.contentHeight
        val x = element.layout.screenX + cs.effectivePaddingLeft
        var y = element.layout.screenY + cs.effectivePaddingTop - element.scrollTop
        for (child in inFlowChildren(element)) {
            val layout = child.layout
            layout.screenX = x + child.computedStyle.marginLeft.resolve(contentWidth)
            layout.screenY = y + child.computedStyle.marginTop.resolve(contentWidth)
            layout.screenWidth = measureWidth(child, contentWidth)
            layout.screenHeight = measureHeight(child, layout.screenWidth, contentWidth, contentHeight)
            y += layout.screenHeight + verticalMargins(child, contentWidth)
        }
    }

    /**
     * Resolves the lengths of the columns or rows of a grid.
     * @param name the name of the template property, for error messages
     */
    private fun resolveGridTemplate(template: GridTemplate, name: String, gap: Double, length: Double): List<Double> {
        require(template is GridTemplate.LengthList) { "$template is not a valid $name" }
        val constraints = template.value

        val totalLength = length - gap * (constraints.size - 1)
        var availableLength = totalLength
        var totalFraction = 0.0

        val result = DoubleArray(constraints.size) { 0.0 }
        val resolved = BooleanArray(constraints.size) { false }

        for ((i, constraint) in constraints.withIndex()) {
            when (constraint) {
                is LinearDimension.PX -> {
                    result[i] = constraint.value
                    availableLength -= constraint.value
                    resolved[i] = true
                }

                is LinearDimension.MinMax -> {
                    if (constraint.min is LinearDimension.PX) {
                        result[i] = constraint.min.value
                    }
                    if (constraint.max is LinearDimension.Fraction) {
                        totalFraction += constraint.max.value
                    }
                }

                is LinearDimension.Fraction -> totalFraction += constraint.value
                else -> {}
            }
        }

        // a minmax track whose fraction is smaller than its minimum keeps its minimum
        var unresolvedFraction = 0.0
        for ((i, constraint) in constraints.withIndex()) {
            when (constraint) {
                is LinearDimension.MinMax -> {
                    val max = ((constraint.max as LinearDimension.Fraction).value / totalFraction) * availableLength
                    if (max < result[i]) {
                        resolved[i] = true
                    } else {
                        unresolvedFraction += constraint.max.value
                    }
                }

                is LinearDimension.Fraction -> unresolvedFraction += constraint.value
                else -> {}
            }
        }

        val remainingLength = max(0.0, totalLength - constraints.indices.filter { resolved[it] }.sumOf { result[it] })
        for ((i, constraint) in constraints.withIndex()) {
            if (!resolved[i]) {
                val fraction = when (constraint) {
                    is LinearDimension.MinMax -> (constraint.max as LinearDimension.Fraction).value
                    is LinearDimension.Fraction -> constraint.value
                    else -> continue
                }
                result[i] = (fraction / unresolvedFraction) * remainingLength
                resolved[i] = true
            }
        }
        require(resolved.all { it }) { "unable to resolve $name" }
        return result.toList()
    }

    /**
     * The cells a grid item covers along one axis.
     * @param cursor the next free cell along the axis
     */
    private fun GridPopulation.cells(cursor: Int): IntRange = when (this) {
        is GridPopulation.Auto -> cursor..cursor
        is GridPopulation.Length -> cursor until cursor + value
        is GridPopulation.Position -> value..value
        is GridPopulation.Span -> value
        else -> error("$this is not supported")
    }

    /**
     * Places the in-flow children of [element] in the cells of its grid. A cell is the containing area of its child,
     * children with an auto width or height fill their cells.
     */
    private fun arrangeGrid(element: Element) {
        val cs = element.computedStyle
        val contentBounds = element.layout.contentBounds

        val rowGap = cs.rowGap.inPixels(element.layout.screenHeight)
        val columnGap = cs.columnGap.inPixels(element.layout.screenWidth)

        val columnWidths = resolveGridTemplate(cs.gridTemplateColumns, "gridTemplateColumns", columnGap, contentBounds.width)
        val rowHeights = resolveGridTemplate(cs.gridTemplateRows, "gridTemplateRows", rowGap, contentBounds.height)
        val columns = columnWidths.size

        // tracks beyond the template repeat the last track
        fun columnWidth(column: Int) = columnWidths.getOrNull(column) ?: columnWidths.last()
        fun rowHeight(row: Int) = rowHeights.getOrNull(row) ?: rowHeights.last()

        var x = 0
        var y = 0
        for (child in inFlowChildren(element)) {
            var columnCells = child.computedStyle.gridColumn.cells(x)
            var rowCells = child.computedStyle.gridRow.cells(y)

            // an item that does not fit on the current row moves to the start of the next row
            if (columnCells.first > 0 && columnCells.last >= columns) {
                columnCells = 0..(columnCells.last - columnCells.first)
                rowCells = (rowCells.first + 1)..(rowCells.last + 1)
            }

            val cellX = contentBounds.x + (0 until columnCells.first).sumOf { columnWidth(it) + columnGap }
            val cellY = contentBounds.y + (0 until rowCells.first).sumOf { rowHeight(it) + rowGap }
            val cellWidth = columnCells.sumOf { columnWidth(it) } + columnGap * (columnCells.count() - 1)
            val cellHeight = rowCells.sumOf { rowHeight(it) } + rowGap * (rowCells.count() - 1)

            val ccs = child.computedStyle
            val layout = child.layout
            layout.screenX = cellX + ccs.marginLeft.resolve(cellWidth)
            layout.screenY = cellY + ccs.marginTop.resolve(cellWidth) - element.scrollTop
            layout.screenWidth = if (ccs.width == LinearDimension.Auto) {
                cellWidth - horizontalMargins(child, cellWidth)
            } else {
                measureWidth(child, cellWidth)
            }
            layout.screenHeight = if (ccs.height == LinearDimension.Auto) {
                cellHeight - verticalMargins(child, cellWidth)
            } else {
                measureHeight(child, layout.screenWidth, cellWidth, cellHeight)
            }
            x += columnCells.count()
            if (x >= columns) {
                x = 0
                y++
            }
        }
    }

    /**
     * Arranges [element] and its descendants. The size and position of an element in the flow have been set by
     * its parent, fixed and absolute elements are sized and positioned here.
     */
    private fun arrange(element: Element) {
        val cs = element.computedStyle
        if (cs.display == Display.NONE) {
            return
        }
        val layout = element.layout
        if (cs.position in manualPosition) {
            val parent = element.parent
            layout.screenWidth = measureWidth(element, element.containerWidth)
            layout.screenHeight =
                measureHeight(element, layout.screenWidth, element.containerWidth, element.containerHeight)
            // fixed elements are placed relative to the root, absolute elements relative to their parent
            val origin = if (cs.position == Position.FIXED || parent == null) element.root() else parent
            layout.screenX = origin.layout.screenX + cs.left.resolve(origin.layout.screenWidth)
            layout.screenY = origin.layout.screenY + cs.top.resolve(origin.layout.screenHeight)
        }

        layout.paddingLeft = cs.effectivePaddingLeft
        layout.paddingTop = cs.effectivePaddingTop
        layout.paddingRight = cs.effectivePaddingRight
        layout.paddingBottom = cs.effectivePaddingBottom
        element.sized()

        layout.zIndex = when (val zIndex = cs.zIndex) {
            is ZIndex.Value -> zIndex.value
            is ZIndex.Auto, is ZIndex.Inherit -> element.parent?.layout?.zIndex ?: 0
        }

        arrangeChildren(element)
        element.children.forEach { arrange(it) }
    }
    //</editor-fold>

    /**
     * Lays out [element] and its descendants. The element is sized relative to its parent, keeping its position
     * unless it is fixed or absolute.
     */
    fun layout(element: Element) {
        clearCaches()
        if (element.computedStyle.position !in manualPosition) {
            element.layout.screenWidth = measureWidth(element, element.containerWidth)
            element.layout.screenHeight =
                measureHeight(element, element.layout.screenWidth, element.containerWidth, element.containerHeight)
        }
        arrange(element)
    }

    private fun clearCaches() {
        widthCache.clear()
        heightCache.clear()
    }

    /**
     * The width [element] wants inside its parent, including its margins when [includeMargins] is set.
     */
    fun width(element: Element, includeMargins: Boolean = true): Double {
        clearCaches()
        return measureWidth(element, element.containerWidth) +
                if (includeMargins) horizontalMargins(element, element.containerWidth) else 0.0
    }

    /**
     * The height [element] wants inside its parent when it is [width] wide, including its margins when
     * [includeMargins] is set.
     */
    fun height(element: Element, width: Double? = null, includeMargins: Boolean = true): Double {
        clearCaches()
        val elementWidth = width ?: measureWidth(element, element.containerWidth)
        return measureHeight(element, elementWidth, element.containerWidth, element.containerHeight) +
                if (includeMargins) verticalMargins(element, element.containerWidth) else 0.0
    }

    fun computeStyles(element: Element) {
        val matcher = Matcher()

        if (element is TextNode) {
            // TODO: figure out why this is needed
            element.computedStyle = element.parent?.computedStyle?.cascadeOnto(StyleSheet(CompoundSelector.DUMMY))
                ?: StyleSheet(CompoundSelector.DUMMY)
        } else {
            element.computedStyle =
                styleSheets
                    .filter {
                        it.selector.let {
                            matcher.matches(it, element)
                        }
                    }
                    .sortedWith(
                        compareBy(
                            { it.precedence.component1() },
                            { it.precedence.component2() },
                            { it.precedence.component3() },
                            { it.precedence.component4() })
                    )
                    .reversed()
                    .fold(StyleSheet(CompoundSelector.DUMMY), { a, b -> a.cascadeOnto(b) })

            element.style?.let {
                element.computedStyle = it.cascadeOnto(element.computedStyle)
            }
        }

        element.computedStyle.let { cs ->

            element.parent?.let { p ->
                cs.properties.forEach { (k, v) ->
                    if ((v.value as? PropertyValue)?.inherit == true) {
                        cs.properties[k] = p.computedStyle.getProperty(k) ?: v
                    }
                }
                PropertyBehaviours.behaviours.forEach { (k, v) ->
                    if (v.inheritance == PropertyInheritance.INHERIT && k !in cs.properties) {
                        if (k in p.computedStyle.properties) {
                            cs.properties[k] = p.computedStyle.getProperty(k)!!
                        }
                    }
                }
            }
        }

        element.children.forEach { computeStyles(it) }
    }

}
