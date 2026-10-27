package org.openrndr.panel.elements

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
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
                    scrollTop = (scrollTop - it.rotation.y * 10).coerceIn(0.0, maxScrollTop)
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
                drawer.rectangle(layout.borderBoundsAtOrigin)
                //drawer.smooth(true)
            }
        }
    }

    override fun toString(): String {
        return "Div(id=${id})"
    }
}