package org.openrndr.panel.elements

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.panel.style.Color
import org.openrndr.panel.style.Display
import org.openrndr.panel.style.FlexDirection
import org.openrndr.panel.style.LinearDimension
import org.openrndr.panel.style.Overflow
import org.openrndr.panel.style.Position
import org.openrndr.panel.style.ZIndex
import org.openrndr.panel.style.background
import org.openrndr.panel.style.borderColor
import org.openrndr.panel.style.borderWidth
import org.openrndr.panel.style.color
import org.openrndr.panel.style.columnGap
import org.openrndr.panel.style.display
import org.openrndr.panel.style.effectiveBorderColor
import org.openrndr.panel.style.effectiveBorderWidth
import org.openrndr.panel.style.flexDirection
import org.openrndr.panel.style.height
import org.openrndr.panel.style.left
import org.openrndr.panel.style.length
import org.openrndr.panel.style.overflow
import org.openrndr.panel.style.padding
import org.openrndr.panel.style.paddingBottom
import org.openrndr.panel.style.position
import org.openrndr.panel.style.rowGap
import org.openrndr.panel.style.top
import org.openrndr.panel.style.width
import org.openrndr.panel.style.zIndex
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

class SlideOut(val x: Double, val y: Double, val width: Double, val height: Double) : Element(ElementType("slide-out")) {

    var dragging = false
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
                dispose()
            }
        }
    }

    override fun draw(drawer: Drawer) {
        //(root() as Body).controlManager?.keyboardInput?.requestFocus(children[0])
        computedStyle.let { style ->
            style.background.let {
                drawer.fill = ((it as? Color.RGBa)?.color ?: ColorRGBa.BLACK)
                drawer.stroke = computedStyle.effectiveBorderColor
                drawer.strokeWeight = computedStyle.effectiveBorderWidth
                drawer.rectangle(0.0, 0.0, layout.screenWidth, layout.screenHeight)
            }
        }
    }

    fun dispose() {
        parent?.remove(this)
        close()
    }
}

@OptIn(ExperimentalContracts::class)
fun Element.slideOut(x: Double, y: Double, width: Double, height: Double, vararg classes: String, init: SlideOut.() -> Unit): SlideOut {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val slideOut = SlideOut(x, y, width, height)
    initElement(classes, slideOut, init)
    return slideOut
}