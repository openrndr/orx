package org.openrndr.panel.elements

import org.openrndr.Program
import org.openrndr.draw.Drawer
import org.openrndr.draw.isolated
import org.openrndr.extra.viewbox.ViewBox
import org.openrndr.shape.Rectangle

class ViewBoxElement(val viewBox: ViewBox): Element(ElementType("view-box")) {

    override fun draw(drawer: Drawer) {
        drawer.isolated {
            // the view box draws in the coordinates of the window, within the clipping of the elements around it
            val clip = drawer.drawStyle.clip
            val clipStyle = drawer.shadeStyle
            drawer.defaults()
            drawer.drawStyle.clip = clip
            drawer.shadeStyle = clipStyle
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

