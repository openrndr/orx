package org.openrndr.panel.elements

import org.openrndr.Program
import org.openrndr.draw.Drawer
import org.openrndr.draw.isolated
import org.openrndr.extra.viewbox.ViewBox
import org.openrndr.shape.Rectangle

class ViewBoxElement(val viewBox: ViewBox): Element(ElementType("view-box")) {

    override fun draw(drawer: Drawer) {
        drawer.isolated {
            drawer.defaults()
            viewBox.clientArea = layout.contentBounds
            viewBox.draw()
        }
    }
}

fun Element.viewBox(viewBox: ViewBox) = append(ViewBoxElement(viewBox))

fun Element.viewBox(program: Program, f: ViewBox.() -> Unit): ViewBoxElement {
    val viewBox = ViewBox(program, Rectangle(0.0, 0.0, 100.0, 100.0))
    viewBox.f()
    val vbe =  ViewBoxElement(viewBox)
    append(vbe)
    return vbe
}

