package org.openrndr.panel.test

import org.openrndr.CharacterEvent
import org.openrndr.math.Vector2
import org.openrndr.panel.Tooltips
import org.openrndr.panel.elementAt
import org.openrndr.panel.elements.*
import org.openrndr.panel.layout.Layouter
import org.openrndr.panel.style.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class WidgetsTest {
    @Test
    fun `tooltips show after the pointer rests`() {
        var now = 0.0
        val tooltips = Tooltips { now }
        val element = Div().apply { tooltip = "help" }
        tooltips.moved(element, Vector2.ZERO)
        assertTrue(tooltips.waiting)
        assertFalse(tooltips.showing)
        now = 0.7
        assertTrue(tooltips.showing)
        // moving within the element keeps it, moving off hides it
        tooltips.moved(element, Vector2(5.0, 5.0))
        assertTrue(tooltips.showing)
        tooltips.moved(null, Vector2(100.0, 100.0))
        assertFalse(tooltips.showing)
        tooltips.moved(element, Vector2.ZERO)
        assertFalse(tooltips.showing)
        tooltips.hide()
        now = 2.0
        assertFalse(tooltips.showing)
    }

    @Test
    fun `tabs show one page`() {
        lateinit var tabs: Tabs
        val body = Body(null).apply {
            style { width = 400.px; height = 300.px }
            tabs = tabs {
                tab("one") {}
                tab("two") {}
            }
        }
        // laying out text needs a font manager, the styles of the pages are checked
        assertEquals(listOf<Display>(Display.BLOCK, Display.NONE), tabs.pages.map { it.style!!.display })
        val selected = mutableListOf<Int>()
        tabs.changed.listen { selected.add(it.index) }
        tabs.selected = 1
        assertEquals(listOf<Display>(Display.NONE, Display.BLOCK), tabs.pages.map { it.style!!.display })
        assertTrue(ElementPseudoClass("selected") in tabs.bar.children[1].pseudoClasses)
        assertFalse(ElementPseudoClass("selected") in tabs.bar.children[0].pseudoClasses)
        tabs.selected = 5
        assertEquals(listOf(1), selected)
    }

    @Test
    fun `number fields stay in range and take typed values`() {
        val field = NumberField().apply { range = 0.0..10.0 }
        field.value = 20.0
        assertEquals(10.0, field.value)
        field.value = 2.5
        field.keyboard.character.trigger(CharacterEvent('7', emptySet()))
        assertEquals("7", field.editing)
        field.keyboard.character.trigger(CharacterEvent('x', emptySet()))
        assertEquals("7", field.editing)
        field.accept()
        assertEquals(7.0, field.value)
        assertNull(field.editing)
        field.edit("abc")
        field.accept()
        assertEquals(7.0, field.value)
        field.edit("3")
        field.cancel()
        assertEquals(7.0, field.value)
    }

    @Test
    fun `the innermost element is found by position`() {
        lateinit var outer: Div
        lateinit var inner: Div
        val body = Body(null).apply {
            style { width = 400.px; height = 300.px }
            outer = div { style { width = 200.px; height = 200.px }; tooltip = "outer"
                inner = div { style { width = 100.px; height = 100.px }; tooltip = "inner" }
            }
        }
        Layouter().apply { computeStyles(body); layout(body) }
        assertSame(inner, elementAt(body, Vector2(50.0, 50.0)) { it.tooltip != null })
        assertSame(outer, elementAt(body, Vector2(150.0, 150.0)) { it.tooltip != null })
        assertNull(elementAt(body, Vector2(300.0, 250.0)) { it.tooltip != null })
    }
}
