package org.openrndr.panel.layout

import org.openrndr.panel.elements.Element
import org.openrndr.panel.elements.TextNode
import org.openrndr.panel.style.*
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

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
 * The padding and border of an element in pixels.
 */
private class Frame(val left: Double, val top: Double, val right: Double, val bottom: Double, val border: Double) {
    val width: Double get() = left + right + 2.0 * border
    val height: Double get() = top + bottom + 2.0 * border
}

/**
 * An item of a flex container, with its sizes along the main and cross axes of the container.
 */
private class FlexItem(
    val element: Element,
    /** the size before growing or shrinking */
    val base: Double,
    var main: Double,
    var cross: Double,
    val mainMarginStart: Double,
    val mainMarginEnd: Double,
    val crossMarginStart: Double,
    val crossMarginEnd: Double,
    val minMain: Double,
    val maxMain: Double
) {
    val outerMain: Double get() = mainMarginStart + main + mainMarginEnd
    val outerCross: Double get() = crossMarginStart + cross + crossMarginEnd
}

private class FlexLine(val items: List<FlexItem>, var cross: Double)

/**
 * The placement of an item in the cells of a grid.
 */
private class GridPlacement(val element: Element, val columns: IntRange, val rows: IntRange)

/**
 * The placements and track sizes of a grid.
 */
private class GridGeometry(
    val placements: List<GridPlacement>,
    val columnWidths: List<Double>,
    val rowHeights: List<Double>,
    val columnGap: Double,
    val rowGap: Double
) {
    fun columnStart(column: Int) = (0 until column).sumOf { columnWidths[it] + columnGap }
    fun rowStart(row: Int) = (0 until row).sumOf { rowHeights[it] + rowGap }
    fun width(columns: IntRange) = columns.sumOf { columnWidths[it] } + columnGap * (columns.count() - 1)
    fun height(rows: IntRange) = rows.sumOf { rowHeights[it] } + rowGap * (rows.count() - 1)
    val totalHeight: Double get() = if (rowHeights.isEmpty()) 0.0 else height(rowHeights.indices)
}

/**
 * Computes the styles of a tree of elements and lays it out.
 *
 * Layout happens in two kinds of steps. Measuring determines the size an element wants for a given containing size,
 * it does not change any state, its results are cached for the duration of a [layout] pass. Arranging sets the
 * sizes and positions of the children of an element, using the measured sizes. Every element in the flow is sized and
 * positioned by its parent, fixed and absolute elements size and position themselves.
 *
 * Sizes are border-box sizes: they include the padding and the border, but not the margins of an element.
 * Percentages are taken of the content size of the containing element; margins and padding are always taken of its
 * width.
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

    /** the width inside the padding and border of an arranged element */
    private val Element.contentWidth: Double
        get() = layout.contentBoundsAtOrigin.width

    /** the height inside the padding and border of an arranged element */
    private val Element.contentHeight: Double
        get() = layout.contentBoundsAtOrigin.height

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

    /** the padding and border of [element], percentages are taken of [containerWidth] */
    private fun frame(element: Element, containerWidth: Double): Frame = element.computedStyle.let {
        Frame(
            it.paddingLeft.resolve(containerWidth),
            it.paddingTop.resolve(containerWidth),
            it.paddingRight.resolve(containerWidth),
            it.paddingBottom.resolve(containerWidth),
            it.borderWidth.resolve(containerWidth)
        )
    }

    /** stores the resolved padding and border of [element] in its layout */
    private fun applyFrame(element: Element, containerWidth: Double) {
        val frame = frame(element, containerWidth)
        element.layout.paddingLeft = frame.left
        element.layout.paddingTop = frame.top
        element.layout.paddingRight = frame.right
        element.layout.paddingBottom = frame.bottom
        element.layout.borderWidth = frame.border
    }

    private fun minWidth(element: Element, containerWidth: Double) =
        element.computedStyle.minWidth.resolve(containerWidth)

    private fun maxWidth(element: Element, containerWidth: Double) =
        element.computedStyle.maxWidth.let { if (it == LinearDimension.Auto) Double.POSITIVE_INFINITY else it.resolve(containerWidth) }

    private fun minHeight(element: Element, containerHeight: Double) =
        element.computedStyle.minHeight.resolve(containerHeight)

    private fun maxHeight(element: Element, containerHeight: Double) =
        element.computedStyle.maxHeight.let { if (it == LinearDimension.Auto) Double.POSITIVE_INFINITY else it.resolve(containerHeight) }

    /** [width] limited by the minimum and maximum width of [element]; the minimum wins */
    private fun clampWidth(element: Element, width: Double, containerWidth: Double) =
        max(min(width, maxWidth(element, containerWidth)), minWidth(element, containerWidth))

    /** [height] limited by the minimum and maximum height of [element]; the minimum wins */
    private fun clampHeight(element: Element, height: Double, containerHeight: Double) =
        max(min(height, maxHeight(element, containerHeight)), minHeight(element, containerHeight))

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
            } else {
                val frame = frame(element, containerWidth)
                val width = when (val width = cs.width) {
                    is LinearDimension.PX -> width.value
                    is LinearDimension.Percent ->
                        containerWidth * (width.value / 100.0) - horizontalMargins(element, containerWidth)

                    is LinearDimension.Auto -> {
                        val available = containerWidth - horizontalMargins(element, containerWidth) - frame.width
                        val contentWidth = when (element) {
                            // text takes its natural width, it wraps when that is not available. The text writer
                            // only keeps words on a line that end before its edge, hence the extra pixel.
                            is TextNode -> min(ceil(element.textWidth()) + 1.0, max(0.0, available))
                            else -> element.widthHint ?: naturalContentWidth(element, available)
                        }
                        contentWidth + frame.width
                    }

                    // the height of an element depends on its width, it is not known here
                    is LinearDimension.Calculate -> width.function(CalculateContext(null, null))
                    else -> error("width $width is not supported")
                }
                clampWidth(element, width, containerWidth)
            }
        }

    /**
     * The height of [element] when it is [width] wide.
     * @param containerWidth the content width of the containing element, the reference for margins and padding
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
            } else {
                val frame = frame(element, containerWidth)
                val height = when (val height = cs.height) {
                    is LinearDimension.PX -> height.value
                    is LinearDimension.Percent ->
                        containerHeight * (height.value / 100.0) - verticalMargins(element, containerWidth)

                    is LinearDimension.Auto -> {
                        val available = containerHeight - verticalMargins(element, containerWidth) - frame.height
                        (element.heightHint ?: naturalContentHeight(element, width - frame.width, available)) +
                                frame.height
                    }

                    is LinearDimension.Calculate -> height.function(CalculateContext(width, null))
                    else -> error("height $height is not supported")
                }
                clampHeight(element, height, containerHeight)
            }
        }

    private fun outerWidth(element: Element, containerWidth: Double) =
        measureWidth(element, containerWidth) + horizontalMargins(element, containerWidth)

    private fun outerHeight(element: Element, width: Double, containerWidth: Double, containerHeight: Double) =
        measureHeight(element, width, containerWidth, containerHeight) + verticalMargins(element, containerWidth)

    private val StyleSheet.isRowFlex: Boolean
        get() = flexDirection == FlexDirection.Row || flexDirection == FlexDirection.RowReverse

    private val StyleSheet.isColumnFlex: Boolean
        get() = flexDirection == FlexDirection.Column || flexDirection == FlexDirection.ColumnReverse

    /**
     * The width the in-flow children of [element] need when [contentWidth] is available.
     */
    private fun naturalContentWidth(element: Element, contentWidth: Double): Double {
        val cs = element.computedStyle
        return when {
            cs.display == Display.GRID -> contentWidth
            cs.display == Display.FLEX && cs.isRowFlex -> {
                val lines = flexLines(element, true, contentWidth, Double.POSITIVE_INFINITY, null, null)
                lines.maxOfOrNull { lineExtent(element, it, true, contentWidth, Double.POSITIVE_INFINITY) } ?: 0.0
            }

            cs.display == Display.FLEX && cs.isColumnFlex -> {
                val lines = flexLines(element, false, contentWidth, Double.POSITIVE_INFINITY, null, null)
                crossExtent(element, lines, false, contentWidth, Double.POSITIVE_INFINITY)
            }

            else -> inFlowChildren(element).maxOfOrNull { outerWidth(it, contentWidth) } ?: 0.0
        }
    }

    /**
     * The height the in-flow children of [element] need when the element's content is [contentWidth] wide and
     * [contentHeight] is available.
     */
    private fun naturalContentHeight(element: Element, contentWidth: Double, contentHeight: Double): Double {
        val cs = element.computedStyle
        return when {
            cs.display == Display.GRID -> gridGeometry(element, contentWidth, null).totalHeight
            cs.display == Display.FLEX && cs.isRowFlex -> {
                val lines = flexLines(element, true, contentWidth, contentHeight, contentWidth, null)
                crossExtent(element, lines, true, contentWidth, contentHeight)
            }

            cs.display == Display.FLEX && cs.isColumnFlex -> {
                val lines = flexLines(element, false, contentWidth, contentHeight, null, contentWidth)
                lines.maxOfOrNull { lineExtent(element, it, false, contentWidth, contentHeight) } ?: 0.0
            }

            else -> inFlowChildren(element).sumOf {
                outerHeight(it, measureWidth(it, contentWidth), contentWidth, contentHeight)
            }
        }
    }
    //</editor-fold>

    //<editor-fold desc="Flex">
    private fun mainGap(element: Element, horizontal: Boolean, contentWidth: Double, contentHeight: Double) =
        if (horizontal) element.computedStyle.columnGap.resolve(contentWidth)
        else element.computedStyle.rowGap.resolve(contentHeight)

    private fun crossGap(element: Element, horizontal: Boolean, contentWidth: Double, contentHeight: Double) =
        if (horizontal) element.computedStyle.rowGap.resolve(contentHeight)
        else element.computedStyle.columnGap.resolve(contentWidth)

    /** the size of a line along the main axis, including the margins of its items and the gaps between them */
    private fun lineExtent(element: Element, line: FlexLine, horizontal: Boolean, contentWidth: Double, contentHeight: Double) =
        line.items.sumOf { it.outerMain } +
                mainGap(element, horizontal, contentWidth, contentHeight) * max(0, line.items.size - 1)

    /** the size of the lines across the main axis, including the gaps between them */
    private fun crossExtent(element: Element, lines: List<FlexLine>, horizontal: Boolean, contentWidth: Double, contentHeight: Double) =
        lines.sumOf { it.cross } + crossGap(element, horizontal, contentWidth, contentHeight) * max(0, lines.size - 1)

    /**
     * Breaks the in-flow children of the flex container [element] into lines and sizes them.
     *
     * @param horizontal true for a row, false for a column
     * @param contentWidth the content width of [element], the reference for percentages
     * @param contentHeight the content height of [element], the reference for percentages
     * @param mainSize the size of the container along the main axis to grow or shrink the items to, or null to keep
     * their natural sizes
     * @param crossSize the size of the container across the main axis: the size of a single line, or null to size
     * lines to their items
     */
    private fun flexLines(
        element: Element,
        horizontal: Boolean,
        contentWidth: Double,
        contentHeight: Double,
        mainSize: Double?,
        crossSize: Double?
    ): List<FlexLine> {
        val cs = element.computedStyle
        val wrap = cs.flexWrap == FlexWrap.Wrap
        val stretch = cs.alignItems == AlignItems.Stretch
        val gap = mainGap(element, horizontal, contentWidth, contentHeight)

        val items = inFlowChildren(element).map { child ->
            val ccs = child.computedStyle
            if (horizontal) {
                val width = measureWidth(child, contentWidth)
                FlexItem(
                    child, width, width, 0.0,
                    ccs.marginLeft.resolve(contentWidth), ccs.marginRight.resolve(contentWidth),
                    ccs.marginTop.resolve(contentWidth), ccs.marginBottom.resolve(contentWidth),
                    minWidth(child, contentWidth), maxWidth(child, contentWidth)
                )
            } else {
                // a stretched item in a single line takes the width of the line before its height is measured
                val width = if (stretch && !wrap && crossSize != null && ccs.width == LinearDimension.Auto) {
                    clampWidth(child, crossSize - horizontalMargins(child, contentWidth), contentWidth)
                } else {
                    measureWidth(child, contentWidth)
                }
                val height = measureHeight(child, width, contentWidth, contentHeight)
                FlexItem(
                    child, height, height, width,
                    ccs.marginTop.resolve(contentWidth), ccs.marginBottom.resolve(contentWidth),
                    ccs.marginLeft.resolve(contentWidth), ccs.marginRight.resolve(contentWidth),
                    minHeight(child, contentHeight), maxHeight(child, contentHeight)
                )
            }
        }

        // break the items into lines where they do not fit the main size
        val wrapAt = mainSize ?: (if (horizontal) contentWidth else contentHeight)
        val lines = mutableListOf<MutableList<FlexItem>>()
        var extent = 0.0
        for (item in items) {
            val outer = item.mainMarginStart + item.base + item.mainMarginEnd
            if (lines.isEmpty() || (wrap && lines.last().isNotEmpty() && extent + gap + outer > wrapAt)) {
                lines.add(mutableListOf())
                extent = outer
            } else {
                extent += gap + outer
            }
            lines.last().add(item)
        }

        return lines.map { lineItems ->
            if (mainSize != null) {
                val free = mainSize - lineItems.sumOf { it.mainMarginStart + it.mainMarginEnd } -
                        gap * max(0, lineItems.size - 1)
                val sizes = flexSizes(lineItems, free)
                for ((item, size) in lineItems.zip(sizes)) {
                    item.main = size
                }
            }
            for (item in lineItems) {
                if (horizontal) {
                    item.cross = measureHeight(item.element, item.main, contentWidth, contentHeight)
                }
            }
            val line = FlexLine(lineItems, lineItems.maxOfOrNull { it.outerCross } ?: 0.0)
            // a single line fills the container
            if (!wrap && crossSize != null) {
                line.cross = crossSize
            }
            if (stretch) {
                for (item in lineItems) {
                    val ccs = item.element.computedStyle
                    val autoCross = if (horizontal) ccs.height == LinearDimension.Auto else ccs.width == LinearDimension.Auto
                    if (autoCross) {
                        val stretched = line.cross - item.crossMarginStart - item.crossMarginEnd
                        item.cross = if (horizontal) clampHeight(item.element, stretched, contentHeight)
                        else clampWidth(item.element, stretched, contentWidth)
                    }
                }
            }
            line
        }
    }

    /**
     * The sizes of the items of a flex line along the main axis: their sizes grown to fill [free] using flex-grow,
     * or shrunk to fit using flex-shrink weighted by their sizes, within their minimum and maximum sizes. Items that
     * reach a limit are frozen, the space is then shared among the others.
     * @param free the space available to the items, excluding their margins and the gaps between them
     */
    private fun flexSizes(items: List<FlexItem>, free: Double): List<Double> {
        val growing = items.sumOf { it.base } < free
        fun factor(item: FlexItem) = item.element.computedStyle.let { if (growing) it.grow else it.shrink }

        // the base sizes are already within the limits of the items
        val sizes = items.map { it.base }.toMutableList()
        val frozen = BooleanArray(items.size) { factor(items[it]) == 0.0 }
        while (!frozen.all { it }) {
            val unfrozen = items.indices.filter { !frozen[it] }
            val remaining = free - items.indices.filter { frozen[it] }.sumOf { sizes[it] } -
                    unfrozen.sumOf { items[it].base }
            // shrinking is weighted by size, so that small items do not shrink to nothing first
            val weights = unfrozen.map { if (growing) factor(items[it]) else factor(items[it]) * items[it].base }
            val totalWeight = weights.sum()
            if (totalWeight <= 0.0) {
                unfrozen.forEach { frozen[it] = true }
                break
            }
            val violations = DoubleArray(items.size)
            for ((k, i) in unfrozen.withIndex()) {
                val target = items[i].base + (weights[k] / totalWeight) * remaining
                val limited = max(min(target, items[i].maxMain), items[i].minMain)
                violations[i] = limited - target
                sizes[i] = limited
            }
            // freeze the items that reached a limit, the others share the space again; when the limits even out
            // all items are done
            val totalViolation = unfrozen.sumOf { violations[it] }
            for (i in unfrozen) {
                frozen[i] = when {
                    abs(totalViolation) < 1E-9 -> true
                    totalViolation > 0.0 -> violations[i] > 0.0
                    else -> violations[i] < 0.0
                }
            }
        }
        return sizes
    }

    /**
     * Places the in-flow children of a flex container along its main axis, in lines across it.
     * @param horizontal true for a row, false for a column
     * @param reverse true to place the items from the end of the main axis
     */
    private fun arrangeFlex(element: Element, horizontal: Boolean, reverse: Boolean) {
        val cs = element.computedStyle
        val content = element.layout.contentBounds
        val contentWidth = content.width
        val contentHeight = content.height
        val mainSize = if (horizontal) contentWidth else contentHeight
        val crossSize = if (horizontal) contentHeight else contentWidth
        val lines = flexLines(element, horizontal, contentWidth, contentHeight, mainSize, crossSize)
        val gap = mainGap(element, horizontal, contentWidth, contentHeight)
        val lineGap = crossGap(element, horizontal, contentWidth, contentHeight)

        // the start of the content along both axes, scrolled vertically
        val mainStart = if (horizontal) content.x else content.y - element.scrollTop
        var crossPosition = if (horizontal) content.y - element.scrollTop else content.x

        for (line in lines) {
            val free = mainSize - lineExtent(element, line, horizontal, contentWidth, contentHeight)
            val spacing = max(0.0, free)
            val n = line.items.size
            val (offset, between) = when (cs.justifyContent) {
                JustifyContent.End -> free to 0.0
                JustifyContent.Center -> free / 2.0 to 0.0
                JustifyContent.SpaceBetween -> 0.0 to if (n > 1) spacing / (n - 1) else 0.0
                JustifyContent.SpaceAround -> spacing / n / 2.0 to spacing / n
                JustifyContent.SpaceEvenly -> spacing / (n + 1) to spacing / (n + 1)
                else -> 0.0 to 0.0
            }

            var position = offset
            for (item in line.items) {
                // the distance of the item's border box from the main start, or from the main end when reversed
                val leading = if (reverse) item.mainMarginEnd else item.mainMarginStart
                val distance = position + leading
                val mainPosition = if (reverse) mainStart + mainSize - distance - item.main else mainStart + distance
                position += item.outerMain + gap + between

                val crossOffset = when (cs.alignItems) {
                    AlignItems.Center -> (line.cross - item.outerCross) / 2.0
                    AlignItems.End -> line.cross - item.outerCross
                    else -> 0.0
                } + item.crossMarginStart

                val layout = item.element.layout
                applyFrame(item.element, contentWidth)
                if (horizontal) {
                    layout.screenX = mainPosition
                    layout.screenY = crossPosition + crossOffset
                    layout.screenWidth = item.main
                    layout.screenHeight = item.cross
                    layout.growWidth = item.main - item.base
                } else {
                    layout.screenX = crossPosition + crossOffset
                    layout.screenY = mainPosition
                    layout.screenWidth = item.cross
                    layout.screenHeight = item.main
                    layout.growHeight = item.main - item.base
                }
            }
            crossPosition += line.cross + lineGap
        }
    }
    //</editor-fold>

    //<editor-fold desc="Grid">
    private fun gridTemplate(template: GridTemplate, name: String): List<LinearDimension> {
        require(template is GridTemplate.LengthList && template.value.isNotEmpty()) { "$template is not a valid $name" }
        return template.value
    }

    /**
     * Places the in-flow children of a grid in its cells. Items with a row and column are placed there, the other
     * items are placed in the first free cells after the previously placed item.
     */
    private fun placeGridItems(element: Element, columns: Int): List<GridPlacement> {
        val occupied = HashSet<Pair<Int, Int>>()
        fun isFree(columnRange: IntRange, rowRange: IntRange) =
            rowRange.all { row -> columnRange.all { column -> (row to column) !in occupied } }

        var cursorRow = 0
        var cursorColumn = 0
        return inFlowChildren(element).map { child ->
            val ccs = child.computedStyle
            val fixedColumns = when (val c = ccs.gridColumn) {
                is GridPopulation.Position -> c.value..c.value
                is GridPopulation.Span -> c.value
                else -> null
            }
            val fixedRows = when (val r = ccs.gridRow) {
                is GridPopulation.Position -> r.value..r.value
                is GridPopulation.Span -> r.value
                else -> null
            }
            val columnSpan = (ccs.gridColumn as? GridPopulation.Length)?.value?.coerceIn(1, columns) ?: 1
            val rowSpan = (ccs.gridRow as? GridPopulation.Length)?.value?.coerceAtLeast(1) ?: 1

            val placement = when {
                fixedColumns != null && fixedRows != null -> GridPlacement(child, fixedColumns, fixedRows)
                fixedRows != null -> {
                    val column = (0..columns - columnSpan).firstOrNull {
                        isFree(it until it + columnSpan, fixedRows)
                    } ?: 0
                    GridPlacement(child, column until column + columnSpan, fixedRows)
                }

                fixedColumns != null -> {
                    var row = if (fixedColumns.first < cursorColumn) cursorRow + 1 else cursorRow
                    while (!isFree(fixedColumns, row until row + rowSpan)) row++
                    GridPlacement(child, fixedColumns, row until row + rowSpan)
                }

                else -> {
                    var row = cursorRow
                    var column = cursorColumn
                    while (true) {
                        if (column + columnSpan > columns) {
                            row++
                            column = 0
                        }
                        if (isFree(column until column + columnSpan, row until row + rowSpan)) break
                        column++
                    }
                    GridPlacement(child, column until column + columnSpan, row until row + rowSpan)
                }
            }
            for (row in placement.rows) for (column in placement.columns) occupied.add(row to column)
            // items placed in a row of their own choosing move the cursor past them
            if (fixedRows == null) {
                cursorRow = placement.rows.first
                cursorColumn = placement.columns.last + 1
            }
            placement
        }
    }

    /**
     * Resolves the sizes of the tracks of a grid along one axis.
     * @param template the track sizes; tracks beyond the template repeat the last track
     * @param count the number of tracks
     * @param contentSize the size of the content of a track, for auto tracks
     * @param length the length available to the tracks, or null when it is not known
     */
    private fun resolveTracks(
        template: List<LinearDimension>,
        count: Int,
        contentSize: (Int) -> Double,
        gap: Double,
        length: Double?
    ): List<Double> {
        val explicit = template.size
        val sizes = DoubleArray(explicit)
        val flexible = mutableListOf<Int>()
        for ((i, track) in template.withIndex()) {
            when (track) {
                is LinearDimension.PX -> sizes[i] = track.value
                is LinearDimension.Percent -> sizes[i] = (length ?: 0.0) * (track.value / 100.0)
                is LinearDimension.Auto -> sizes[i] = contentSize(i)
                is LinearDimension.MinMax -> {
                    sizes[i] = (track.min as? LinearDimension.PX)?.value ?: 0.0
                    flexible.add(i)
                }

                is LinearDimension.Fraction -> flexible.add(i)
                else -> error("grid track $track is not supported")
            }
        }
        fun fraction(i: Int) = when (val track = template[i]) {
            is LinearDimension.MinMax -> (track.max as? LinearDimension.Fraction)?.value ?: 0.0
            is LinearDimension.Fraction -> track.value
            else -> 0.0
        }

        // fractional tracks share the remaining length; a minmax track whose share is smaller than its minimum
        // keeps its minimum, and the others share what is left
        if (length != null) {
            val fixed = (0 until explicit).filter { it !in flexible }.sumOf { sizes[it] }
            val available = length - gap * (count - 1) - fixed
            val sharing = flexible.toMutableList()
            while (true) {
                val share = max(0.0, available - flexible.filter { it !in sharing }.sumOf { sizes[it] })
                val totalFraction = sharing.sumOf { fraction(it) }
                val keepMinimum = sharing.filter {
                    totalFraction > 0.0 && fraction(it) / totalFraction * share < sizes[it]
                }
                if (keepMinimum.isEmpty()) {
                    for (i in sharing) {
                        sizes[i] = if (totalFraction > 0.0) fraction(i) / totalFraction * share else 0.0
                    }
                    break
                }
                sharing.removeAll(keepMinimum)
            }
        }

        val last = template.last()
        return List(count) { i ->
            when {
                i < explicit -> sizes[i]
                last == LinearDimension.Auto -> contentSize(i)
                else -> sizes[explicit - 1]
            }
        }
    }

    /**
     * Places the items of the grid [element] and sizes its columns and rows.
     * @param contentHeight the content height of the grid, or null when it is not known
     */
    private fun gridGeometry(element: Element, contentWidth: Double, contentHeight: Double?): GridGeometry {
        val cs = element.computedStyle
        val columnTemplate = gridTemplate(cs.gridTemplateColumns, "gridTemplateColumns")
        val rowTemplate = gridTemplate(cs.gridTemplateRows, "gridTemplateRows")
        val placements = placeGridItems(element, columnTemplate.size)

        val columnGap = cs.columnGap.resolve(contentWidth)
        val rowGap = cs.rowGap.resolve(contentHeight ?: 0.0)
        val columnCount = max(columnTemplate.size, (placements.maxOfOrNull { it.columns.last } ?: -1) + 1)
        val rowCount = max(rowTemplate.size, (placements.maxOfOrNull { it.rows.last } ?: -1) + 1)

        // auto tracks take the size of the items that span only them
        val columnWidths = resolveTracks(columnTemplate, columnCount, { column ->
            placements.filter { it.columns == column..column }.maxOfOrNull { outerWidth(it.element, contentWidth) } ?: 0.0
        }, columnGap, contentWidth)

        fun cellWidth(placement: GridPlacement) =
            placement.columns.sumOf { columnWidths[it] } + columnGap * (placement.columns.count() - 1)

        val rowHeights = resolveTracks(rowTemplate, rowCount, { row ->
            placements.filter { it.rows == row..row }.maxOfOrNull {
                val cellWidth = cellWidth(it)
                val width = gridItemWidth(it.element, cellWidth)
                outerHeight(it.element, width, cellWidth, contentHeight ?: 0.0)
            } ?: 0.0
        }, rowGap, contentHeight)

        return GridGeometry(placements, columnWidths, rowHeights, columnGap, rowGap)
    }

    /** the width of a grid item in a cell [cellWidth] wide: items with an auto width fill their cells */
    private fun gridItemWidth(element: Element, cellWidth: Double) =
        if (element.computedStyle.width == LinearDimension.Auto) {
            clampWidth(element, cellWidth - horizontalMargins(element, cellWidth), cellWidth)
        } else {
            measureWidth(element, cellWidth)
        }

    /**
     * Places the in-flow children of [element] in the cells of its grid. A cell is the containing area of its child,
     * children with an auto width or height fill their cells.
     */
    private fun arrangeGrid(element: Element) {
        val content = element.layout.contentBounds
        val geometry = gridGeometry(element, content.width, content.height)
        for (placement in geometry.placements) {
            val child = placement.element
            val ccs = child.computedStyle
            val cellX = content.x + geometry.columnStart(placement.columns.first)
            val cellY = content.y + geometry.rowStart(placement.rows.first) - element.scrollTop
            val cellWidth = geometry.width(placement.columns)
            val cellHeight = geometry.height(placement.rows)

            val layout = child.layout
            applyFrame(child, cellWidth)
            layout.screenX = cellX + ccs.marginLeft.resolve(cellWidth)
            layout.screenY = cellY + ccs.marginTop.resolve(cellWidth)
            layout.screenWidth = gridItemWidth(child, cellWidth)
            layout.screenHeight = if (ccs.height == LinearDimension.Auto) {
                clampHeight(child, cellHeight - verticalMargins(child, cellWidth), cellHeight)
            } else {
                measureHeight(child, layout.screenWidth, cellWidth, cellHeight)
            }
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
                FlexDirection.Row -> arrangeFlex(element, horizontal = true, reverse = false)
                FlexDirection.RowReverse -> arrangeFlex(element, horizontal = true, reverse = true)
                FlexDirection.Column -> arrangeFlex(element, horizontal = false, reverse = false)
                FlexDirection.ColumnReverse -> arrangeFlex(element, horizontal = false, reverse = true)
                else -> {}
            }

            Display.GRID -> arrangeGrid(element)
            else -> arrangeBlock(element)
        }
    }

    /**
     * Places the in-flow children of [element] below each other.
     */
    private fun arrangeBlock(element: Element) {
        val content = element.layout.contentBounds
        var y = content.y - element.scrollTop
        for (child in inFlowChildren(element)) {
            val layout = child.layout
            applyFrame(child, content.width)
            layout.screenX = content.x + child.computedStyle.marginLeft.resolve(content.width)
            layout.screenY = y + child.computedStyle.marginTop.resolve(content.width)
            layout.screenWidth = measureWidth(child, content.width)
            layout.screenHeight = measureHeight(child, layout.screenWidth, content.width, content.height)
            y += layout.screenHeight + verticalMargins(child, content.width)
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
            applyFrame(element, element.containerWidth)
            layout.screenWidth = measureWidth(element, element.containerWidth)
            layout.screenHeight =
                measureHeight(element, layout.screenWidth, element.containerWidth, element.containerHeight)
            // fixed elements are placed relative to the root, absolute elements relative to their parent
            val origin = if (cs.position == Position.FIXED || parent == null) element.root() else parent
            layout.screenX = origin.layout.screenX + cs.left.resolve(origin.layout.screenWidth)
            layout.screenY = origin.layout.screenY + cs.top.resolve(origin.layout.screenHeight)
        }
        element.sized()

        // an element that scrolls its content can not scroll beyond it, also when the content has shrunk
        if (cs.overflow != Overflow.Visible) {
            val frameHeight = layout.screenHeight - element.contentHeight
            layout.scrollHeight = naturalContentHeight(element, element.contentWidth, element.contentHeight) + frameHeight
            element.scrollTop = element.scrollTop.coerceIn(0.0, element.maxScrollTop)
        }

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
            applyFrame(element, element.containerWidth)
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

    /**
     * The style of a text node: the text takes the font, color and alignment of its parent and fills the parent's
     * content box, or sizes to the text where the parent sizes to its content. The box properties of the parent
     * (margins, padding, border and positioning) apply to the parent only.
     */
    private fun textNodeStyle(parentStyle: StyleSheet?): StyleSheet {
        val style = parentStyle?.cascadeOnto(StyleSheet(CompoundSelector.DUMMY)) ?: StyleSheet(CompoundSelector.DUMMY)
        if (parentStyle != null) {
            style.width = if (parentStyle.width == LinearDimension.Auto) LinearDimension.Auto else 100.percent
            style.height = if (parentStyle.height == LinearDimension.Auto) LinearDimension.Auto else 100.percent
        }
        style.minWidth = LinearDimension.Auto
        style.maxWidth = LinearDimension.Auto
        style.minHeight = LinearDimension.Auto
        style.maxHeight = LinearDimension.Auto
        style.margins(0.px)
        style.padding(0.px)
        style.borderWidth = 0.px
        style.position = Position.STATIC
        style.left = 0.px
        style.top = 0.px
        return style
    }

    fun computeStyles(element: Element) {
        val matcher = Matcher()

        if (element is TextNode) {
            element.computedStyle = textNodeStyle(element.parent?.computedStyle)
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
