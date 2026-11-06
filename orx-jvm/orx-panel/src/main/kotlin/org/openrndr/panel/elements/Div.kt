package org.openrndr.panel.elements

import org.openrndr.KeyModifier
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.StrokeAlignment
import org.openrndr.panel.style.*

/** a box that holds other elements, of [type] `div` unless elements built on it give another */
open class Div(type: ElementType = ElementType("div")) : TextElement(type) {
    init {
        mouse.pressed.listen {
            if ((computedStyle.effectiveBackground?.alpha ?: 0.0) > 0.0) {
                it.cancelPropagation()
            }
        }
        mouse.scrolled.listen {
            computedStyle.let { cs ->
                if (cs.clipsContent) {
                    // with shift held a vertical wheel scrolls horizontally, for mice without a horizontal wheel
                    // so does it for an element that only scrolls horizontally; the wheel scrolls the axes with
                    // Overflow.Scroll, the others only scroll by the program
                    val userX = cs.effectiveOverflowX == Overflow.Scroll && maxScrollLeft > 0.0
                    val userY = cs.effectiveOverflowY == Overflow.Scroll && maxScrollTop > 0.0
                    val onlyHorizontal = userX && !userY
                    val shifted = (KeyModifier.SHIFT in it.modifiers || onlyHorizontal) && it.rotation.x == 0.0
                    val dx = if (!userX) 0.0 else if (shifted) it.rotation.y else it.rotation.x
                    val dy = if (!userY || shifted) 0.0 else it.rotation.y
                    // from where the content scrolls to, so that steps of the wheel add up
                    val target = scrollTarget
                    scrollTo(
                        (target.x - dx * 10).coerceIn(0.0, maxScrollLeft),
                        (target.y - dy * 10).coerceIn(0.0, maxScrollTop)
                    )
                    it.cancelPropagation()
                }
            }
        }
    }

    override fun draw(drawer: Drawer) {
        computedStyle.let { style ->
            style.background.let {
                drawer.fill = ((it as? Color.RGBa)?.color ?: ColorRGBa.BLACK)
                drawer.stroke = computedStyle.effectiveBorderColor
                drawer.strokeWeight = layout.borderWidth
                // the background covers the box, the border is drawn inside it
                drawer.strokeAlignment = StrokeAlignment.INSIDE
                drawer.box(layout.boundsAtOrigin, cornerRadius)
                //drawer.smooth(true)
            }
        }
    }

    override fun toString(): String {
        return "Div(id=${id})"
    }
}