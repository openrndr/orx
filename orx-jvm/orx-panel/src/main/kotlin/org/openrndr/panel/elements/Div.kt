package org.openrndr.panel.elements

import org.openrndr.KeyModifier
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.StrokeAlignment
import org.openrndr.panel.style.*

open class Div : TextElement(ElementType("div")) {
    init {
        mouse.pressed.listen {
            if ((computedStyle.effectiveBackground?.alpha ?: 0.0) > 0.0) {
                it.cancelPropagation()
            }
        }
        mouse.scrolled.listen {
            computedStyle.let { cs ->
                if (cs.overflow != Overflow.Visible) {
                    // with shift held a vertical wheel scrolls horizontally, for mice without a horizontal wheel
                    // so does it for an element that only scrolls horizontally
                    val onlyHorizontal = maxScrollTop <= 0.0 && maxScrollLeft > 0.0
                    val shifted = (KeyModifier.SHIFT in it.modifiers || onlyHorizontal) && it.rotation.x == 0.0
                    val dx = if (shifted) it.rotation.y else it.rotation.x
                    val dy = if (shifted) 0.0 else it.rotation.y
                    scrollTop = (scrollTop - dy * 10).coerceIn(0.0, maxScrollTop)
                    scrollLeft = (scrollLeft - dx * 10).coerceIn(0.0, maxScrollLeft)
                    draw.dirty = true
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
                drawer.rectangle(layout.boundsAtOrigin)
                //drawer.smooth(true)
            }
        }
    }

    override fun toString(): String {
        return "Div(id=${id})"
    }
}