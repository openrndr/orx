package org.openrndr.panel.layout

import io.github.oshai.kotlinlogging.KotlinLogging
import org.openrndr.math.Vector2
import org.openrndr.panel.elements.Element
import org.openrndr.panel.elements.TextNode
import org.openrndr.panel.style.*
import org.openrndr.shape.Rectangle
import java.util.*
import kotlin.comparisons.compareBy
import kotlin.math.max

private val logger = KotlinLogging.logger {}

class Layouter {
    val styleSheets = ArrayList<StyleSheet>()
    val blockLike = setOf(Display.BLOCK, Display.FLEX)
    val manualPosition = setOf(Position.FIXED, Position.ABSOLUTE)

    /**
     * Positions the children of the given element based on its computed style and layout properties.
     * This method calculates the layout and screen positions of the element's children,
     * taking into account alignment, spacing, flex, and other layout properties.
     *
     * @param element the parent element whose children will be positioned.
     * @param knownWidth an optional specified width for the element; if null, the element's screen width will be used.
     * @return a Rectangle representing the bounding area of the positioned children.
     */
    fun positionChildren(element: Element, knownWidth: Double? = null): Rectangle {

        return element.computedStyle.let { cs ->
            var y = element.layout.screenY - element.scrollTop + element.computedStyle.effectivePaddingTop

            when (cs.display) {
                Display.FLEX -> {
                    when (cs.flexDirection) {
                        FlexDirection.Row -> {
                            var maxHeight = 0.0
                            var x = element.layout.screenX + element.computedStyle.effectivePaddingLeft

                            val affectedElements = element.children
                                .filter { it.computedStyle.display in blockLike && it.computedStyle.position !in manualPosition }

                            val gapWidth = element.computedStyle.columnGap.inPixels(element.layout.screenHeight)
                            val totalGapWidth = max(0, affectedElements.size - 1) * gapWidth

                            val totalWidth = totalGapWidth +
                                    affectedElements
                                        .map { width(it) }.sum()
                            val remainder = (knownWidth ?: element.layout.screenWidth) - totalWidth
                            val totalGrow =
                                element.children.filter { it.computedStyle.display in blockLike && it.computedStyle.position !in manualPosition }
                                    .map { (it.computedStyle.flexGrow as FlexGrow.Ratio).value }.sum()
                            val totalShrink =
                                element.children.filter { it.computedStyle.display in blockLike && it.computedStyle.position !in manualPosition }
                                    .map { (it.computedStyle.flexShrink as FlexGrow.Ratio).value }.sum()


                            element.children.filter { it.computedStyle.display in blockLike && it.computedStyle.position !in manualPosition }
                                .forEach { child ->
                                    val elementGrow = (child.computedStyle.flexGrow as FlexGrow.Ratio).value
                                    val elementShrink = (child.computedStyle.flexShrink as FlexGrow.Ratio).value
                                    val growWidth = if (totalGrow > 0) (elementGrow / totalGrow) * remainder else 0.0
                                    val shrinkWidth =
                                        if (totalShrink > 0) (elementShrink / totalShrink) * remainder else 0.0

                                    child.layout.screenY =
                                        y + ((child.computedStyle.marginTop as? LinearDimension.PX)?.value
                                            ?: 0.0)
                                    child.layout.screenX =
                                        x + ((child.computedStyle.marginLeft as? LinearDimension.PX)?.value
                                            ?: 0.0)

                                    child.layout.growWidth = if (remainder > 0) growWidth else shrinkWidth

                                    val effectiveWidth = width(child) + (if (remainder > 0) growWidth else shrinkWidth)
                                    x += effectiveWidth
                                    x += gapWidth
                                    maxHeight = max(height(child, effectiveWidth), maxHeight)
                                }
                            Rectangle(Vector2(x, y), x - element.layout.screenX, maxHeight)
                        }

                        FlexDirection.Column -> {
                            var maxWidth = 0.0
                            var ly = element.layout.screenY + element.computedStyle.effectivePaddingTop
                            val lx = element.layout.screenX + element.computedStyle.effectivePaddingLeft

                            val verticalPadding =
                                element.computedStyle.effectivePaddingTop + element.computedStyle.effectivePaddingBottom

                            val affectedElements = element.children
                                .filter { it.computedStyle.display in blockLike && it.computedStyle.position !in manualPosition }

                            val gapHeight = element.computedStyle.rowGap.inPixels(element.layout.screenHeight)
                            val totalGapHeight = max(0, affectedElements.size - 1) * gapHeight
                            val totalHeight = totalGapHeight +
                                    affectedElements
                                        .sumOf { height(it, width(it)) }
                            val remainder = ((element.layout.screenHeight - verticalPadding) - totalHeight)
                            val totalGrow = element.children
                                .filter { it.computedStyle.display in blockLike && it.computedStyle.position !in manualPosition }
                                .sumOf { (it.computedStyle.flexGrow as FlexGrow.Ratio).value }

                            element.children.filter { it.computedStyle.display in blockLike && it.computedStyle.position !in manualPosition }
                                .forEach { child ->
                                    val elementGrow = (child.computedStyle.flexGrow as FlexGrow.Ratio).value
                                    val growHeight = if (totalGrow > 0) (elementGrow / totalGrow) * remainder else 0.0

                                    child.layout.screenY =
                                        ly + ((child.computedStyle.marginTop as? LinearDimension.PX)?.value
                                            ?: 0.0)
                                    child.layout.screenX =
                                        lx + ((child.computedStyle.marginLeft as? LinearDimension.PX)?.value
                                            ?: 0.0)

                                    child.layout.growHeight = growHeight

                                    val effectHeight = height(child) + growHeight
                                    ly += effectHeight
                                    ly += gapHeight
                                    maxWidth = max(width(child), maxWidth)
                                }

                            Rectangle(Vector2(lx, ly), maxWidth, ly - element.layout.screenY)
                        }

                        else -> Rectangle(Vector2(element.layout.screenX, element.layout.screenY), 0.0, 0.0)
                    }
                }

                Display.GRID -> {

                    fun resolveGridTemplate(template: GridTemplate, gap: Double, length: Double): List<Double> {
                        require(template is GridTemplate.LengthList) {
                            "${element.computedStyle.gridTemplateColumns} is not a valid grid template"
                        }
                        val constraints = template.value.toMutableList()

                        val totalLength = length - gap * (constraints.size - 1)
                        var availableLength = totalLength
                        var assignedLength = 0.0
                        var totalFraction = 0.0

                        val result = DoubleArray(constraints.size) { 0.0 }
                        val resolved = BooleanArray(constraints.size) { false }

                        for (i in constraints.indices) {
                            val constraint = constraints[i]

                            when (constraint) {
                                is LinearDimension.PX -> {
                                    result[i] = constraint.value
                                    availableLength -= constraint.value
                                    assignedLength += constraint.value
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

                                is LinearDimension.Fraction -> {
                                    totalFraction += constraint.value
                                }

                                else -> {}
                            }
                        }

                        var totalFraction2 = 0.0

                        for (i in constraints.indices) {
                            val constraint = constraints[i]
                            when (constraint) {
                                is LinearDimension.MinMax -> {
                                    val max =
                                        ((constraint.max as LinearDimension.Fraction).value / totalFraction) * availableLength
                                    if (max < result[i]) {
                                        resolved[i] = true

                                    } else {
                                        totalFraction2 += constraint.max.value
                                    }
                                }

                                is LinearDimension.Fraction -> {
                                    totalFraction2 += constraint.value
                                }

                                else -> {}
                            }
                        }

                        var availableLength2 = totalLength
                        for (i in constraints.indices) {
                            if (resolved[i]) {
                                availableLength2 -= result[i]
                            }
                        }
                        availableLength2 = max(0.0, availableLength2)

                        for (i in constraints.indices) {
                            if (!resolved[i]) {
                                when (val constraint = constraints[i]) {
                                    is LinearDimension.MinMax -> {
                                        val max =
                                            ((constraint.max as LinearDimension.Fraction).value / totalFraction2) * availableLength2
                                        result[i] = max
                                        resolved[i] = true
                                    }

                                    is LinearDimension.Fraction -> {
                                        result[i] = (constraint.value / totalFraction2) * availableLength2
                                        resolved[i] = true
                                    }

                                    else -> {}
                                }
                            }
                        }
                        require(resolved.all { it }) { "unable to resolve grid template" }
                        return result.toList()

                    }

                    val contentBounds = element.layout.contentBounds

                    val rowGap = element.computedStyle.rowGap.inPixels(element.layout.screenHeight)
                    val columnGap = element.computedStyle.columnGap.inPixels(element.layout.screenWidth)





                    val columnWidths =
                        resolveGridTemplate(element.computedStyle.gridTemplateColumns, columnGap, contentBounds.width)
                    val columns = columnWidths.size

                    val rowHeights =
                        resolveGridTemplate(element.computedStyle.gridTemplateRows, rowGap, contentBounds.height)

                    var x = 0
                    var y = 0
                    for ((index, child) in element.children.withIndex()) {
                        var sx = when (val c = child.computedStyle.gridColumn) {
                            is GridPopulation.Auto, is GridPopulation.Length -> x
                            is GridPopulation.Position -> c.value
                            is GridPopulation.Span -> c.value.first
                            else -> throw RuntimeException("not supported")
                        }
                        var ex = when (val c = child.computedStyle.gridColumn) {
                            is GridPopulation.Auto, is GridPopulation.Position -> sx
                            is GridPopulation.Length -> sx + (c.value - 1)
                            is GridPopulation.Span -> c.value.last
                            else -> throw RuntimeException("not supported")
                        }

                        var sy = when (val c = child.computedStyle.gridRow) {
                            is GridPopulation.Auto, is GridPopulation.Length -> y
                            is GridPopulation.Position -> c.value
                            is GridPopulation.Span -> c.value.first
                            else -> throw RuntimeException("not supported")
                        }
                        var ey = when (val c = child.computedStyle.gridRow) {
                            is GridPopulation.Auto, is GridPopulation.Position -> sy
                            is GridPopulation.Length -> sy + (c.value - 1)
                            is GridPopulation.Span -> c.value.last
                            else -> throw RuntimeException("not supported")
                        }

                        if (sx > 0 && ex >= columns) {
                            ex = ex - sx
                            sx = 0
                            sy++
                            ey++
                        }
                        var cellX = contentBounds.x
                        var cellY = contentBounds.y
                        for (x in 0 until sx) {
                            cellX += (columnWidths.getOrNull(x) ?: columnWidths.last()) + columnGap
                        }
                        for (y in 0 until sy) {
                            cellY += (rowHeights.getOrNull(y) ?: rowHeights.last()) + rowGap
                        }

                        var cellWidth = 0.0
                        for (x in sx..ex) {
                            if (x > sx) {
                                cellWidth += columnGap
                            }
                            cellWidth += columnWidths.getOrNull(x) ?: columnWidths.last()
                        }

                        var cellHeight = 0.0
                        for (y in sy..ey) {
                            if (y > sy) {
                                cellHeight += rowGap
                            }
                            cellHeight += rowHeights.getOrNull(y) ?: rowHeights.last()
                        }

                        child.layout.screenX = cellX + child.computedStyle.marginLeft.inPixels(cellWidth)
                        child.layout.screenY = cellY + child.computedStyle.marginTop.inPixels(cellWidth) - element.scrollTop
                        if (child.computedStyle.width == LinearDimension.Auto) {
                            child.layout.screenWidth = cellWidth - child.computedStyle.marginLeft.inPixels(cellWidth) - child.computedStyle.marginRight.inPixels(cellWidth)
                            child.layout.widthSetByParent = true
                        }
                        if (child.computedStyle.height == LinearDimension.Auto) {
                            child.layout.screenHeight = cellHeight - child.computedStyle.marginTop.inPixels(cellWidth) - child.computedStyle.marginBottom.inPixels(cellWidth)
                            child.layout.heightSetByParent = true
                        }
                        x += (ex - sx) + 1
                        if (x >= columns) {
                            x = 0
                            y++
                        }

                    }
                    return contentBounds
                }

                else -> {
                    val x = element.layout.screenX + element.computedStyle.effectivePaddingLeft
                    var maxWidth = 0.0
                    element.children.forEach {
                        if (it.computedStyle.display in blockLike && it.computedStyle.position !in manualPosition) {
                            it.layout.screenY = y + ((it.computedStyle.marginTop as? LinearDimension.PX)?.value ?: 0.0)
                            it.layout.screenX = x + ((it.computedStyle.marginLeft as? LinearDimension.PX)?.value ?: 0.0)
                            val effectiveWidth = width(it)
                            maxWidth = max(effectiveWidth, maxWidth)
                            y += height(it, effectiveWidth)
                        } else if (it.computedStyle.position == Position.ABSOLUTE) {
                            it.layout.screenX =
                                element.layout.screenX + ((it.computedStyle.left as? LinearDimension.PX)?.value
                                    ?: 0.0)
                            it.layout.screenY =
                                element.layout.screenY + ((it.computedStyle.top as? LinearDimension.PX)?.value
                                    ?: 0.0)
                        }
                    }
                    Rectangle(
                        Vector2(element.layout.screenX, element.layout.screenY),
                        maxWidth,
                        y - element.layout.screenY
                    )
                }
            }
        }
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

    fun margin(element: Element, f: (StyleSheet) -> LinearDimension): Double {
        val value = f(element.computedStyle)
        return when (value) {
            is LinearDimension.PX -> value.value
            else -> 0.0
        }
    }

    fun padding(element: Element?, f: (StyleSheet) -> LinearDimension): Double {
        return if (element != null) {
            val value = f(element.computedStyle)
            when (value) {
                is LinearDimension.PX -> value.value
                else -> 0.0
            }
        } else 0.0
    }

    fun marginTop(element: Element) = margin(element, StyleSheet::marginTop)
    fun marginBottom(element: Element) = margin(element, StyleSheet::marginBottom)
    fun marginLeft(element: Element) = margin(element, StyleSheet::marginLeft)
    fun marginRight(element: Element) = margin(element, StyleSheet::marginRight)

    fun paddingTop(element: Element?) = padding(element, StyleSheet::paddingTop)
    fun paddingBottom(element: Element?) = padding(element, StyleSheet::paddingBottom)
    fun paddingLeft(element: Element?) = padding(element, StyleSheet::paddingLeft)
    fun paddingRight(element: Element?) = padding(element, StyleSheet::paddingRight)

    fun height(element: Element, width: Double? = null, includeMargins: Boolean = true): Double {
        if (element.computedStyle.display == Display.NONE) {
            return 0.0
        }

        if (element is TextNode) {
            return element.sizeHint().height + if (includeMargins) marginBottom(element) + marginTop(element) else 0.0
        }

        return element.computedStyle.let {
            it.height.let { ld ->
                when (val it = ld) {
                    is LinearDimension.PX -> it.value
                    is LinearDimension.Percent -> {
                        val parentHeight = element.parent?.layout?.screenHeight ?: 0.0
                        val parentPadding = element.parent?.computedStyle?.effectivePaddingHeight ?: 0.0
                        val margins = marginTop(element) + marginBottom(element)
                        val effectiveHeight = (parentHeight - parentPadding) * (it.value / 100.0) - margins
                        effectiveHeight
                    }

                    is LinearDimension.Auto -> {
                        val padding = paddingTop(element) + paddingBottom(element)
                        (element.heightHint ?: positionChildren(element, width).height) + padding
                    }

                    is LinearDimension.Calculate -> {
                        val context = CalculateContext(width, null)
                        it.function(context)

                    }

                    else -> throw RuntimeException("not supported")
                }
            } + if (includeMargins) ((it.marginTop as? LinearDimension.PX)?.value
                ?: 0.0) + ((it.marginBottom as? LinearDimension.PX)?.value ?: 0.0) else 0.0
        }
    }

    fun width(element: Element, height: Double? = null, includeMargins: Boolean = true): Double =
        element.computedStyle.let {
            if (element.computedStyle.display == Display.NONE) {
                return 0.0
            }
            val result =
                it.width.let {
                    when (it) {
                        is LinearDimension.PX -> it.value
                        is LinearDimension.Percent -> {
                            val parentWidth = element.parent?.layout?.screenWidth ?: 0.0
                            val parentPadding = element.parent?.computedStyle?.effectivePaddingWidth ?: 0.0
                            val margins = marginLeft(element) + marginRight(element)
                            val effectiveWidth = (parentWidth - parentPadding) * (it.value / 100.0) - margins
                            effectiveWidth
                        }
//                        is LinearDimension.Calculate -> {
//                            val context = CalculateContext(null, height)
//                            it.function(context)
//
//                        }
                        is LinearDimension.Auto -> (element.widthHint ?: positionChildren(element).width) +
                                paddingRight(element) + paddingLeft(element)

                        else -> throw RuntimeException("not supported")
                    }
                } + if (includeMargins) marginLeft(element) + marginRight(element) else 0.0

            // TODO: find out why this hack is needed, I added this because somewhere in the layout process
            // this information is lost
            element.layout.screenWidth =
                result - if (includeMargins) marginLeft(element) + marginRight(element) else 0.0
            result
        }

    fun layout(element: Element) {

        for (child in element.children) {
            child.layout.widthSetByParent = false
            child.layout.heightSetByParent = false
        }

        element.computedStyle.also { cs ->
            cs.display.let { if (it == Display.NONE) return }
            if (!element.layout.widthSetByParent) {
                element.layout.screenWidth = width(element, includeMargins = false)
                element.layout.screenWidth += element.layout.growWidth
            }
            if (!element.layout.heightSetByParent) {
                element.layout.screenHeight = height(element, element.layout.screenWidth, includeMargins = false)
                element.layout.screenHeight += element.layout.growHeight
            }

            element.layout.paddingLeft = element.computedStyle.effectivePaddingLeft
            element.layout.paddingTop = element.computedStyle.effectivePaddingTop
            element.layout.paddingRight = element.computedStyle.effectivePaddingRight
            element.layout.paddingBottom = element.computedStyle.effectivePaddingBottom

            when (cs.position) {
                Position.FIXED -> {
                    element.layout.screenX = (cs.left as? LinearDimension.PX)?.value ?: 0.0
                    element.layout.screenY = (cs.top as? LinearDimension.PX)?.value ?: 0.0
                }

                else -> {
                }
            }
            val lzi = cs.zIndex
            element.layout.zIndex = when (lzi) {
                is ZIndex.Value -> lzi.value
                is ZIndex.Auto -> element.parent?.layout?.zIndex ?: 0
                is ZIndex.Inherit -> element.parent?.layout?.zIndex ?: 0
            }

            positionChildren(element)
        }
        element.children.forEach { layout(it) }
    }
}