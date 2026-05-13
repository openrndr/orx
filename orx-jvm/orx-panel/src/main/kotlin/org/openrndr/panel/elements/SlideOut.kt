package org.openrndr.panel.elements

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.panel.style.*
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

class SlideOut(val x: Double, val y: Double, val width: Double, val height: Double) :
    Element(ElementType("slide-out")) {

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
fun Element.slideOut(
    x: Double,
    y: Double,
    width: Double,
    height: Double,
    vararg classes: String,
    init: SlideOut.() -> Unit
): SlideOut {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val slideOut = SlideOut(x, y, width, height)
    initElement(classes, slideOut, init)
    return slideOut
}