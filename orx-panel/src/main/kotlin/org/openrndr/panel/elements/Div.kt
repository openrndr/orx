package org.openrndr.panel.elements

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.panel.style.Color
import org.openrndr.panel.style.Overflow
import org.openrndr.panel.style.background
import org.openrndr.panel.style.overflow
import kotlin.math.max

open class Div : TextElement(ElementType("div")) {
    init {
        mouse.pressed.listen {
            it.cancelPropagation()
        }
        mouse.scrolled.listen {
            computedStyle.let { cs ->
                if (cs.overflow != Overflow.Visible) {
                    scrollTop -= it.rotation.y * 10
                    scrollTop = max(0.0, scrollTop)
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
                drawer.stroke = null
                drawer.strokeWeight = 0.0
                //drawer.smooth(false)
                drawer.rectangle(0.0, 0.0, layout.screenWidth, layout.screenHeight)
                //drawer.smooth(true)
            }
        }
    }

    override fun toString(): String {
        return "Div(id=${id})"
    }
}