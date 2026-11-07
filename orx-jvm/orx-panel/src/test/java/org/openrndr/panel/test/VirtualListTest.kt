package org.openrndr.panel.test

import org.openrndr.MouseButton
import org.openrndr.MouseEvent
import org.openrndr.MouseEventType
import org.openrndr.math.Vector2
import org.openrndr.panel.elements.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VirtualListTest : PanelTestFixture() {
    @Test
    fun `a virtual list scrolls with the wheel and builds the rows that come into view`() {
        lateinit var list: VirtualList<Int>
        val cm = panel {
            css("virtual-list { height: 200px }")
            layout { list = virtualList((1..1000).toList(), 20.0) { item, _ -> p { "Row $item" } } }
        }
        repeat(3) { cm.afterDraw(program.drawer, program) }
        assertEquals(0..13, list.range)
        assertEquals(19800.0, list.maxScrollTop, 1E-6)
        cm.mouseInput.scroll(MouseEvent(list.screenArea.center, Vector2(0.0, -10.0), Vector2.ZERO, MouseEventType.SCROLLED, MouseButton.NONE, emptySet()))
        // it scrolls smoothly over a few frames
        repeat(40) { cm.afterDraw(program.drawer, program); Thread.sleep(10) }
        assertEquals(100.0, list.scrollTop, 1E-6)
        assertEquals(1..18, list.range)
        assertTrue(list.children.size == list.range.count() + 2)
    }
}
