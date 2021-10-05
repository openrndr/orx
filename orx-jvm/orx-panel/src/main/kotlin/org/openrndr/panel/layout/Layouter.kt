package org.openrndr.panel.layout

import org.openrndr.math.Vector2
import org.openrndr.panel.elements.Element
import org.openrndr.panel.elements.TextNode
import org.openrndr.panel.style.*
import org.openrndr.shape.Rectangle
import java.util.*
import kotlin.comparisons.compareBy
import kotlin.math.max

class Layouter {
    val styleSheets = ArrayList<StyleSheet>()
    val blockLike = setOf(Display.BLOCK, Display.FLEX)
    val manualPosition = setOf(Position.FIXED, Position.ABSOLUTE)

    fun positionChildren(element: Element, knownWidth:Double? = null): Rectangle {

        return element.computedStyle.let { cs ->
            var y = element.layout.screenY - element.scrollTop + element.computedStyle.effectivePaddingTop

            when (cs.display) {
                Display.FLEX -> {
                    when (cs.flexDirection) {
                        FlexDirection.Row -> {
                            var maxHeight = 0.0
                            var x = element.layout.screenX + element.computedStyle.effectivePaddingLeft

                            val totalWidth = element.children.filter { it.computedStyle.display in blockLike && it.computedStyle.position !in manualPosition }.map { width(it) }.sum()
                            val remainder = (knownWidth?: element.layout.screenWidth) - totalWidth
                            val totalGrow = element.children.filter { it.computedStyle.display in blockLike && it.computedStyle.position !in manualPosition }.map { (it.computedStyle.flexGrow as FlexGrow.Ratio).value }.sum()
                            val totalShrink = element.children.filter { it.computedStyle.display in blockLike && it.computedStyle.position !in manualPosition }.map { (it.computedStyle.flexShrink as FlexGrow.Ratio).value }.sum()


                            element.children.filter { it.computedStyle.display in blockLike && it.computedStyle.position !in manualPosition }.forEach { child ->
                                val elementGrow = (child.computedStyle.flexGrow as FlexGrow.Ratio).value
                                val elementShrink = (child.computedStyle.flexShrink as FlexGrow.Ratio).value
                                val growWidth = if (totalGrow > 0) (elementGrow / totalGrow) * remainder else 0.0
                                val shrinkWidth = if (totalShrink > 0) (elementShrink / totalShrink) * remainder else 0.0

                                child.layout.screenY = y + ((child.computedStyle.marginTop as? LinearDimension.PX)?.value
                                        ?: 0.0)
                                child.layout.screenX = x + ((child.computedStyle.marginLeft as? LinearDimension.PX)?.value
                                        ?: 0.0)

                                child.layout.growWidth = if (remainder > 0) growWidth else shrinkWidth

                                val effectiveWidth = width(child) + (if (remainder > 0) growWidth else shrinkWidth)
                                x += effectiveWidth
                                maxHeight = max(height(child, effectiveWidth), maxHeight)
                            }
                            Rectangle(Vector2(x, y), x - element.layout.screenX, maxHeight)
                        }
                        FlexDirection.Column -> {
                            var maxWidth = 0.0
                            var ly = element.layout.screenY + element.computedStyle.effectivePaddingTop
                            val lx = element.layout.screenX + element.computedStyle.effectivePaddingLeft

                            val verticalPadding = element.computedStyle.effectivePaddingTop + element.computedStyle.effectivePaddingBottom
                            val totalHeight = element.children
                                    .filter { it.computedStyle.display in blockLike && it.computedStyle.position !in manualPosition }
                                    .sumOf { height(it, width(it)) }
                            val remainder = ((element.layout.screenHeight - verticalPadding) - totalHeight)
                            val totalGrow = element.children
                                    .filter { it.computedStyle.display in blockLike && it.computedStyle.position !in manualPosition }
                                    .sumOf { (it.computedStyle.flexGrow as FlexGrow.Ratio).value }

                            element.children.filter { it.computedStyle.display in blockLike && it.computedStyle.position !in manualPosition }.forEach { child ->
                                val elementGrow = (child.computedStyle.flexGrow as FlexGrow.Ratio).value
                                val growHeight = if (totalGrow > 0) (elementGrow / totalGrow) * remainder else 0.0

                                child.layout.screenY = ly + ((child.computedStyle.marginTop as? LinearDimension.PX)?.value
                                        ?: 0.0)
                                child.layout.screenX = lx + ((child.computedStyle.marginLeft as? LinearDimension.PX)?.value
                                        ?: 0.0)

                                child.layout.growHeight = growHeight

                                val effectHeight = height(child) + growHeight
                                ly += effectHeight
                                maxWidth = max(width(child), maxWidth)
                            }

                            Rectangle(Vector2(lx, ly), maxWidth, ly - element.layout.screenY)
                        }
                        else -> Rectangle(Vector2(element.layout.screenX, element.layout.screenY), 0.0, 0.0)
                    }
                }
                else -> {
                    val x = element.layout.screenX + element.computedStyle.effectivePaddingLeft
                    var maxWidth = 0.0
                    element.children.forEach {
                        if (it.computedStyle.display in blockLike && it.computedStyle.position !in manualPosition) {
                            it.layout.screenY = y + ((it.computedStyle.marginTop as? LinearDimension.PX)?.value ?: 0.0)
                            it.layout.screenX = x + ((it.computedStyle.marginLeft as? LinearDimension.PX)?.value ?: 0.0)
                            val effectiveWidth =  width(it)
                            maxWidth = max(effectiveWidth, maxWidth)
                            y += height(it, effectiveWidth)
                        } else if (it.computedStyle.position == Position.ABSOLUTE) {
                            it.layout.screenX = element.layout.screenX + ((it.computedStyle.left as? LinearDimension.PX)?.value
                                    ?: 0.0)
                            it.layout.screenY = element.layout.screenY + ((it.computedStyle.top as? LinearDimension.PX)?.value
                                    ?: 0.0)
                        }
                    }
                    Rectangle(Vector2(element.layout.screenX, element.layout.screenY), maxWidth, y - element.layout.screenY)
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
                            .sortedWith(compareBy({ it.precedence.component1() },
                                    { it.precedence.component2() },
                                    { it.precedence.component3() },
                                    { it.precedence.component4() }))
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

    fun width(element: Element, height: Double? = null, includeMargins: Boolean = true): Double = element.computedStyle.let {
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
            element.layout.screenWidth = result - if (includeMargins) marginLeft(element) + marginRight(element) else 0.0
        result
    }

    fun layout(element: Element) {
        element.computedStyle.also { cs ->
            cs.display.let { if (it == Display.NONE) return }
            element.layout.screenWidth = width(element, includeMargins = false)
            element.layout.screenWidth += element.layout.growWidth
            element.layout.screenHeight = height(element, element.layout.screenWidth, includeMargins = false)
            element.layout.screenHeight += element.layout.growHeight

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
            val result = positionChildren(element)
        }
        element.children.forEach { layout(it) }
    }
}