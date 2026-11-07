package org.openrndr.panel.test

import org.openrndr.panel.elements.*
import org.openrndr.panel.style.width
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class IncrementalLayoutTest : PanelTestFixture() {
    @Test
    fun `changes in boxes of fixed sizes lay out only the boxes`() {
        lateinit var box: Div
        lateinit var item: Div
        lateinit var other: Div
        val cm = panel {
            css("""
                .box { width: 200px; height: 100px }
                .item { height: 20px }
                .other { height: 30px }
                .tall { height: 40px }
            """)
            layout {
                box = div("box") { item = div("item") { }; div("item") { } }
                other = div("other") { }
            }
        }
        repeat(2) { cm.afterDraw(program.drawer, program) }
        val document = cm.document!!
        val otherY = other.layout.screenY
        val secondY = box.children[1].layout.screenY

        // a size changes inside the box, the box is laid out
        item.classes.add(ElementClass("tall"))
        cm.afterDraw(program.drawer, program)
        assertEquals(listOf<Element>(box), document.laidOut)
        assertEquals(secondY + 20.0, box.children[1].layout.screenY, 1E-9)
        assertEquals(otherY, other.layout.screenY, 1E-9)

        // a child is added to the box
        box.div("item") { }
        cm.afterDraw(program.drawer, program)
        assertEquals(listOf<Element>(box), document.laidOut)

        // a box without a fixed size in the body: everything is laid out
        other.classes.add(ElementClass("tall"))
        cm.afterDraw(program.drawer, program)
        assertNull(document.laidOut)
        assertEquals(40.0, other.layout.screenHeight, 1E-9)

        // a resize lays out everything
        cm.body!!.style { width = org.openrndr.panel.style.LinearDimension.PX(300.0) }
        cm.document!!.layoutDirty = true
        cm.afterDraw(program.drawer, program)
        assertNull(document.laidOut)
    }
}
