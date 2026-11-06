package org.openrndr.panel

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.isolated
import org.openrndr.math.Vector2
import org.openrndr.panel.elements.Element
import org.openrndr.panel.elements.box
import org.openrndr.panel.style.*
import org.openrndr.shape.Rectangle
import kotlin.math.max
import kotlin.math.min

/** the space between a scrollbar and the edge of its element, in pixels */
private const val scrollbarInset = 2.0

/** how long scrollbars stay after scrolling, and how long they take to fade out after that, in seconds */
internal const val scrollbarFadeDelay = 1.0
internal const val scrollbarFadeDuration = 0.4

/**
 * Smooth scrolling: elements move towards the positions they scroll to, quickly at first and slowing down near it.
 * @param clock the time in seconds, real time by default
 */
internal class SmoothScrolling(private val clock: () -> Double = { System.nanoTime() / 1E9 }) {
    private val targets = java.util.WeakHashMap<Element, Vector2>()
    private var last: Double? = null

    /** the time it takes to cover most of the distance, in seconds */
    private val timeConstant = 0.06

    /** scrolls [element] to [left] and [top] */
    fun scrollTo(element: Element, left: Double, top: Double) {
        if (targets.isEmpty()) last = clock()
        targets[element] = Vector2(left, top)
    }

    /** where [element] scrolls to, null when it is not scrolling */
    fun target(element: Element): Vector2? = targets[element]

    /** stops the scrolling of [element] where it is */
    fun cancel(element: Element) {
        targets.remove(element)
    }

    val scrolling: Boolean get() = targets.isNotEmpty()

    /** moves the elements towards their targets by the time since the last advance, they are drawn again */
    fun advance() {
        val now = clock()
        val dt = (now - (last ?: now)).coerceIn(0.0, 0.1)
        last = now
        val k = 1.0 - kotlin.math.exp(-dt / timeConstant)
        for ((element, target) in targets.entries.toList()) {
            val left = target.x.coerceIn(0.0, element.maxScrollLeft)
            val top = target.y.coerceIn(0.0, element.maxScrollTop)
            fun step(from: Double, to: Double) = if (kotlin.math.abs(to - from) < 0.5) to else from + (to - from) * k
            element.scrollLeft = step(element.scrollLeft, left)
            element.scrollTop = step(element.scrollTop, top)
            element.draw.dirty = true
            if (element.scrollLeft == left && element.scrollTop == top) targets.remove(element)
        }
    }
}

/**
 * The fading of scrollbars with [ScrollbarVisibility.Auto]: they are shown while their element scrolls, from any
 * source, and while they are hovered or dragged, and they fade out after that. They are hidden until then.
 * @param clock the time in seconds, real time by default so that the program's clock does not stop the fading
 */
internal class ScrollbarFades(private val clock: () -> Double = { System.nanoTime() / 1E9 }) {
    private class State(var top: Double, var left: Double, var activeAt: Double)

    private val states = java.util.WeakHashMap<Element, State>()

    /** the opacity of the scrollbars of [element], which are hovered or dragged when [active] */
    fun opacity(element: Element, active: Boolean): Double {
        if (element.computedStyle.scrollbarVisibility == ScrollbarVisibility.Always) return 1.0
        val now = clock()
        val state = states.getOrPut(element) {
            State(element.scrollTop, element.scrollLeft, Double.NEGATIVE_INFINITY)
        }
        if (active || state.top != element.scrollTop || state.left != element.scrollLeft) {
            state.top = element.scrollTop
            state.left = element.scrollLeft
            state.activeAt = now
        }
        val idle = now - state.activeAt
        return ((scrollbarFadeDelay + scrollbarFadeDuration - idle) / scrollbarFadeDuration).coerceIn(0.0, 1.0)
    }

    /** the elements whose scrollbars are shown or fading, they are drawn again until the scrollbars are hidden */
    fun animating(): List<Element> {
        val now = clock()
        return states.filter { (_, state) -> now - state.activeAt < scrollbarFadeDelay + scrollbarFadeDuration }.keys.toList()
    }
}

/**
 * A scrollbar of an [element] that scrolls, [vertical] or horizontal: its [track] and its [thumb] in screen
 * coordinates. The thumb is as long relative to the track as the element is relative to its content.
 */
internal class Scrollbar(val element: Element, val vertical: Boolean, val track: Rectangle, val thumb: Rectangle) {
    private val trackLength get() = if (vertical) track.height else track.width
    private val thumbLength get() = if (vertical) thumb.height else thumb.width

    /** the scroll position of the element along the axis of the scrollbar */
    var position: Double
        get() = if (vertical) element.scrollTop else element.scrollLeft
        set(value) {
            val limited = value.coerceIn(0.0, maxPosition)
            if (vertical) element.scrollTo(top = limited, smooth = false) else element.scrollTo(left = limited, smooth = false)
        }

    /** scrolls by [distance] along the axis of the scrollbar, smoothly by the scroll behavior of the element */
    fun scrollBy(distance: Double) {
        val target = element.scrollTarget
        if (vertical) element.scrollTo(top = target.y + distance) else element.scrollTo(left = target.x + distance)
    }

    val maxPosition: Double get() = if (vertical) element.maxScrollTop else element.maxScrollLeft

    /** how far the content scrolls when the thumb moves one pixel */
    val scrollPerPixel: Double
        get() = if (trackLength > thumbLength) maxPosition / (trackLength - thumbLength) else 0.0

    /** the size of the element along the axis of the scrollbar, the distance of a page */
    val page: Double get() = if (vertical) element.layout.screenHeight else element.layout.screenWidth

    /** the distance of [point] along the axis of the scrollbar */
    fun along(point: Vector2) = if (vertical) point.y else point.x

    /** true when [point] is before the thumb, above or left of it */
    fun beforeThumb(point: Vector2) = along(point) < (if (vertical) thumb.y else thumb.x)
}

/** the thickness of the scrollbars of [this] in pixels */
private val Element.scrollbarSize: Double
    get() = when (val width = computedStyle.scrollbarWidth) {
        is LinearDimension.PX -> width.value
        else -> 6.0
    }

/**
 * The scrollbars of [this]: an element has one along every axis with [Overflow.Scroll] its content overflows. They are
 * drawn over the content, inside the border.
 */
internal fun Element.scrollbars(): List<Scrollbar> {
    val cs = computedStyle
    if (cs.display == Display.NONE) return emptyList()
    val size = scrollbarSize
    if (size <= 0.0) return emptyList()
    val border = layout.borderWidth
    val inner = Rectangle(layout.screenX + border, layout.screenY + border,
        layout.screenWidth - 2.0 * border, layout.screenHeight - 2.0 * border)
    // along the axes that the user scrolls, when the content overflows
    val vertical = cs.effectiveOverflowY == Overflow.Scroll && maxScrollTop > 0.0
    val horizontal = cs.effectiveOverflowX == Overflow.Scroll && maxScrollLeft > 0.0
    if (!vertical && !horizontal) return emptyList()
    // where both are shown, they leave the corner free
    val corner = size + scrollbarInset
    val minThumb = max(2.0 * size, 16.0)
    val result = mutableListOf<Scrollbar>()

    if (vertical) {
        val track = Rectangle(inner.x + inner.width - size - scrollbarInset, inner.y + scrollbarInset,
            size, max(0.0, inner.height - 2.0 * scrollbarInset - if (horizontal) corner else 0.0))
        val length = min(track.height, max(minThumb, track.height * layout.screenHeight / layout.scrollHeight))
        val y = track.y + (track.height - length) * (scrollTop / maxScrollTop).coerceIn(0.0, 1.0)
        result.add(Scrollbar(this, true, track, Rectangle(track.x, y, size, length)))
    }
    if (horizontal) {
        val track = Rectangle(inner.x + scrollbarInset, inner.y + inner.height - size - scrollbarInset,
            max(0.0, inner.width - 2.0 * scrollbarInset - if (vertical) corner else 0.0), size)
        val length = min(track.width, max(minThumb, track.width * layout.screenWidth / layout.scrollWidth))
        val x = track.x + (track.width - length) * (scrollLeft / maxScrollLeft).coerceIn(0.0, 1.0)
        result.add(Scrollbar(this, false, track, Rectangle(x, track.y, length, size)))
    }
    return result
}

/**
 * The scrollbar at [position] of [root] or its descendants, the one of the innermost element. Elements that are not
 * displayed, and elements outside the elements that scroll them, are skipped.
 */
internal fun scrollbarAt(root: Element, position: Vector2): Scrollbar? {
    var found: Scrollbar? = null
    fun traverse(element: Element) {
        val cs = element.computedStyle
        if (cs.display == Display.NONE) return
        if (cs.clipsContent && position !in element.screenArea) return
        element.scrollbars().firstOrNull { position in it.track }?.let { found = it }
        element.children.forEach(::traverse)
    }
    traverse(root)
    return found
}

/** true when [this] and [other] are the scrollbar of the same element along the same axis */
internal fun Scrollbar?.sameAs(other: Scrollbar?): Boolean =
    if (this == null || other == null) this === other else element === other.element && vertical == other.vertical

/**
 * [color] highlighted by [amount], 0 for none to 1 for full: lighter and more opaque, so that the highlight also
 * shows on opaque and on translucent thumbs
 */
internal fun highlighted(color: ColorRGBa, amount: Double): ColorRGBa {
    val alpha = min(1.0, color.alpha * (1.0 + amount))
    return color.copy(alpha = alpha).mix(ColorRGBa(1.0, 1.0, 1.0, alpha, color.linearity), 0.35 * amount)
}

/**
 * Draws the scrollbars of [element] at [opacity]: the thumb of the scrollbar that is [hovered] is highlighted, the
 * one that is [dragged] more so
 */
internal fun drawScrollbars(drawer: Drawer, element: Element, dragged: Scrollbar?, hovered: Scrollbar?, opacity: Double = 1.0) {
    if (opacity <= 0.0) return
    val bars = element.scrollbars()
    if (bars.isEmpty()) return
    val cs = element.computedStyle
    val thumbColor = ((cs.scrollbarColor as? Color.RGBa)?.color ?: ColorRGBa.WHITE.opacify(0.35)).opacify(opacity)
    val trackColor = ((cs.scrollbarTrackColor as? Color.RGBa)?.color ?: ColorRGBa.TRANSPARENT).opacify(opacity)
    drawer.isolated {
        drawer.stroke = null
        for (bar in bars) {
            // with rounded ends
            val radius = (if (bar.vertical) bar.track.width else bar.track.height) / 2.0
            if (trackColor.alpha > 0.0) {
                drawer.fill = trackColor
                drawer.box(bar.track, radius)
            }
            drawer.fill = when {
                dragged.sameAs(bar) -> highlighted(thumbColor, 1.0)
                hovered.sameAs(bar) -> highlighted(thumbColor, 0.5)
                else -> thumbColor
            }
            drawer.box(bar.thumb, radius)
        }
    }
}
