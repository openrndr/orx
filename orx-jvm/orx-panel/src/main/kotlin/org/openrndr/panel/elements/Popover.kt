package org.openrndr.panel.elements

import org.openrndr.events.Event
import org.openrndr.math.Vector2
import org.openrndr.panel.ControlManager
import org.openrndr.panel.style.*
import org.openrndr.shape.Rectangle

/** the side of its anchor that a [Popover] goes to, when it fits there */
enum class PopoverSide { BELOW, ABOVE, RIGHT, LEFT }

/**
 * A box that floats next to an anchor, see [ControlManager.popover]: not modal, the rest of the panel keeps working.
 * It follows its anchor, and hides while there is none.
 */
class Popover internal constructor(
    private val controlManager: ControlManager,
    /** the area of the window it is next to, read in every frame; null hides it */
    val anchor: () -> Rectangle?,
    val side: PopoverSide,
    /** true when a press outside it and its anchor, or escape, closes it */
    val dismissable: Boolean,
    /** opening a popover of the same group closes this one, none for no group */
    val group: String?
) {
    /** the box with the content, a `popover` */
    val box: Div = PopoverBox(this)

    /** happens when the popover is closed */
    val closed = Event<Popover>("popover-closed")

    /** true while the popover is shown */
    var open = true
        private set

    /** closes the popover; the keyboard focus leaves it */
    fun close() {
        if (!open) return
        open = false
        controlManager.closePopover(this)
        box.parent?.remove(box)
        box.close()
        closed.trigger(this)
    }

    /** the area of the anchor in the last frame, null when there was none */
    internal var anchorArea: Rectangle? = null
        private set

    /** where the box was placed last, null while it is hidden */
    private var placedAt: Vector2? = null

    /** the distance from the anchor, and from the edges of the window */
    private val gap = 6.0
    private val margin = 4.0

    /** places the box next to the anchor, inside the window */
    internal fun place() {
        val area = anchor()
        anchorArea = area
        val body = box.parent ?: return
        if (area == null) {
            if (placedAt != null) {
                placedAt = null
                box.style { display = Display.NONE }
                box.requestLayout()
            }
            return
        }
        val w = box.layout.screenWidth
        val h = box.layout.screenHeight
        val bounds = Rectangle(body.layout.screenX, body.layout.screenY, body.layout.screenWidth, body.layout.screenHeight)
        fun fitsX(x: Double) = x >= bounds.x + margin && x + w <= bounds.x + bounds.width - margin
        fun fitsY(y: Double) = y >= bounds.y + margin && y + h <= bounds.y + bounds.height - margin
        val belowY = area.y + area.height + gap
        val aboveY = area.y - gap - h
        val rightX = area.x + area.width + gap
        val leftX = area.x - gap - w
        // on the side asked for, on the other side when it does not fit there and does there
        val p = when (side) {
            PopoverSide.BELOW -> Vector2(area.x, if (fitsY(belowY) || !fitsY(aboveY)) belowY else aboveY)
            PopoverSide.ABOVE -> Vector2(area.x, if (fitsY(aboveY) || !fitsY(belowY)) aboveY else belowY)
            PopoverSide.RIGHT -> Vector2(if (fitsX(rightX) || !fitsX(leftX)) rightX else leftX, area.y)
            PopoverSide.LEFT -> Vector2(if (fitsX(leftX) || !fitsX(rightX)) leftX else rightX, area.y)
        }
        // shifted along the side to stay in the window
        val x = p.x.coerceAtMost(bounds.x + bounds.width - margin - w).coerceAtLeast(bounds.x + margin)
        val y = p.y.coerceAtMost(bounds.y + bounds.height - margin - h).coerceAtLeast(bounds.y + margin)
        if (placedAt != Vector2(x, y)) {
            placedAt = Vector2(x, y)
            box.style {
                display = Display.BLOCK
                left = x.px
                top = y.px
            }
            box.requestLayout()
        }
    }
}

/** the box of a [Popover], which places itself next to the anchor in every frame */
private class PopoverBox(val popover: Popover) : Div() {
    override fun prepare() {
        popover.place()
    }
}

/**
 * Shows a popover that [init] builds, next to the area of the window that [anchor] gives in every frame, on [side] of
 * it, or on the other side when it does not fit there, and moved along the side to stay inside the window; while
 * [anchor] gives null it is hidden. It is not modal: the rest of the panel keeps working. When it is [dismissable] a
 * press outside it and outside the anchor, or escape, closes it. Opening a popover of a [group] closes the other one
 * of that group. With [focus], the keyboard focus moves to its first control; the focus leaves it when it closes.
 *
 * The popover is a `popover` with [classes]; its content scrolls when it is higher than its `max-height`.
 */
fun ControlManager.popover(
    anchor: () -> Rectangle?,
    vararg classes: String,
    side: PopoverSide = PopoverSide.BELOW,
    dismissable: Boolean = true,
    group: String? = null,
    focus: Boolean = false,
    init: Div.() -> Unit
): Popover {
    val body = body ?: error("no body")
    val popover = Popover(this, anchor, side, dismissable, group)
    popover.box.classes.add(ElementClass("popover"))
    classes.forEach { popover.box.classes.add(ElementClass(it)) }
    popover.box.init()
    // where the anchor is, until it is placed again once its size is known
    popover.box.style {
        anchor()?.let { left = it.x.px; top = (it.y + it.height).px } ?: run { display = Display.NONE }
    }
    body.append(popover.box)
    openPopover(popover, focus)
    return popover
}

/** a popover next to [anchor], a point of the window, see the [popover] with an anchor that moves */
fun ControlManager.popover(
    anchor: Vector2,
    vararg classes: String,
    side: PopoverSide = PopoverSide.BELOW,
    dismissable: Boolean = true,
    group: String? = null,
    focus: Boolean = false,
    init: Div.() -> Unit
): Popover = popover({ Rectangle(anchor, 0.0, 0.0) }, *classes, side = side, dismissable = dismissable, group = group, focus = focus, init = init)

/**
 * a popover next to [anchor], which it follows, and hidden while the element is not in the panel; see the [popover]
 * with an anchor that moves
 */
fun ControlManager.popover(
    anchor: Element,
    vararg classes: String,
    side: PopoverSide = PopoverSide.BELOW,
    dismissable: Boolean = true,
    group: String? = null,
    focus: Boolean = false,
    init: Div.() -> Unit
): Popover = popover({ anchor.takeIf { !it.disposed && it.root() === body }?.screenArea }, *classes,
    side = side, dismissable = dismissable, group = group, focus = focus, init = init)
