package org.openrndr.panel.elements

import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.StrokeAlignment
import org.openrndr.launch
import org.openrndr.panel.style.*
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * A panel that slides out of its parent element, positioned at ([x], [y]) relative to the parent.
 *
 * @param aboveY when set, the slide-out is placed with its bottom edge at [aboveY] relative to the parent when it does
 * not fit between [y] and the bottom of the window, but does fit above [aboveY]
 * @param closeDelayMillis how long the slide-out stays open after the cursor has moved out of it; moving the cursor
 * back in keeps it open
 */
class SlideOut(
    val x: Double,
    val y: Double,
    val width: Double,
    val height: Double,
    val aboveY: Double? = null,
    val closeDelayMillis: Long = 0L
) : Element(ElementType("slide-out")) {

    var dragging = false

    /** the pending close after the cursor moved out */
    private var closing: Job? = null

    init {
        style {
            position = Position.ABSOLUTE
            left = length { x }
            top = length { y }
            width = length { this@SlideOut.width }
            height = LinearDimension.Auto
            overflow = Overflow.Scroll
            zIndex = ZIndex.Value(1000)
            background = color { ColorRGBa.GRAY.shade(0.75) }
            padding(length { 5 })
            paddingBottom = length { 10 }
            borderWidth = length { 1.0 }
            borderColor = color { ColorRGBa.BLACK }
            display = Display.FLEX
            flexDirection = FlexDirection.Column
            rowGap = length { 5 }
        }

        mouse.dragged.listen {
            dragging = true
        }

        mouse.exited.listen {
            if (!dragging) {
                val program = (root() as? Body)?.controlManager?.program
                if (closeDelayMillis <= 0L || program == null) {
                    dispose()
                } else {
                    closing?.cancel()
                    closing = program.launch {
                        delay(closeDelayMillis)
                        dispose()
                    }
                }
            }
        }

        mouse.entered.listen {
            closing?.cancel()
            closing = null
        }
    }

    override fun sized() {
        aboveY?.let { placeAboveWhenNoSpaceBelow(it) }
    }

    override fun draw(drawer: Drawer) {
        //(root() as Body).controlManager?.keyboardInput?.requestFocus(children[0])
        computedStyle.let { style ->
            style.background.let {
                drawer.fill = ((it as? Color.RGBa)?.color ?: ColorRGBa.BLACK)
                drawer.stroke = computedStyle.effectiveBorderColor
                drawer.strokeWeight = layout.borderWidth
                // the background covers the box, the border is drawn inside it
                drawer.strokeAlignment = StrokeAlignment.INSIDE
                drawer.rectangle(layout.boundsAtOrigin)
            }
        }
    }

    fun dispose() {
        closing?.cancel()
        closing = null
        val parent = parent ?: return
        val controlManager = (root() as? Body)?.controlManager
        parent.remove(this)
        // closing after a delay is not caused by an event that redraws the panel
        parent.draw.dirty = true
        controlManager?.checkForManualRedraw()
        close()
    }
}

@OptIn(ExperimentalContracts::class)
fun Element.slideOut(
    x: Double,
    y: Double,
    width: Double,
    height: Double,
    vararg classes: String,
    aboveY: Double? = null,
    closeDelayMillis: Long = 0L,
    init: SlideOut.() -> Unit
): SlideOut {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val slideOut = SlideOut(x, y, width, height, aboveY, closeDelayMillis)
    initElement(classes, slideOut, init)
    return slideOut
}

/**
 * Move an element that does not fit between its position and the bottom of the window up, such that its bottom edge is
 * at [aboveY] relative to its parent. The element is only moved when it fits above [aboveY]. To be called from
 * [Element.sized].
 */
internal fun Element.placeAboveWhenNoSpaceBelow(aboveY: Double) {
    val parent = parent ?: return
    val bottom = root().layout.screenHeight
    if (layout.screenY + layout.screenHeight > bottom) {
        val top = parent.layout.screenY + aboveY - layout.screenHeight
        if (top >= 0.0) {
            layout.screenY = top
        }
    }
}
