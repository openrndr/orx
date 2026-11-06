package org.openrndr.panel.test

import org.openrndr.KeyModifier
import org.openrndr.MouseButton
import org.openrndr.MouseEvent
import org.openrndr.MouseEventType
import org.openrndr.math.Vector2
import org.openrndr.panel.elements.Body
import org.openrndr.panel.elements.Element
import org.openrndr.panel.elements.Div
import org.openrndr.panel.elements.style
import org.openrndr.panel.layout.Layouter
import org.openrndr.color.ColorRGBa
import org.openrndr.panel.ScrollbarFades
import org.openrndr.panel.highlighted
import org.openrndr.panel.sameAs
import org.openrndr.panel.scrollbarAt
import org.openrndr.panel.scrollbars
import org.openrndr.panel.style.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ScrollingTest {
    /** a 100 by 100 div that scrolls content of [contentWidth] by [contentHeight] */
    private fun scroller(contentWidth: Double = 300.0, contentHeight: Double = 300.0): Div {
        val body = Body(null).apply { style { width = 400.px; height = 300.px } }
        val scroller = Div().apply { style { width = 100.px; height = 100.px; overflow = Overflow.Scroll } }
        body.append(scroller)
        scroller.append(Div().apply { style { width = contentWidth.px; height = contentHeight.px } })
        relayout(scroller)
        return scroller
    }

    private fun relayout(element: Element) {
        val root = generateSequence(element) { it.parent }.last()
        Layouter().apply { computeStyles(root); layout(root) }
    }

    private fun scroll(div: Div, dx: Double, dy: Double, vararg modifiers: KeyModifier) =
        div.mouse.scrolled.trigger(
            MouseEvent(Vector2(50.0, 50.0), Vector2(dx, dy), Vector2.ZERO, MouseEventType.SCROLLED, MouseButton.NONE, modifiers.toSet())
        )

    @Test
    fun `wheels scroll both ways within the content`() {
        val div = scroller()
        scroll(div, -3.0, -5.0)
        assertEquals(30.0, div.scrollLeft)
        assertEquals(50.0, div.scrollTop)
        scroll(div, -100.0, -100.0)
        assertEquals(200.0, div.scrollLeft)
        assertEquals(200.0, div.scrollTop)
        scroll(div, 100.0, 100.0)
        assertEquals(0.0, div.scrollLeft)
        assertEquals(0.0, div.scrollTop)
    }

    @Test
    fun `a vertical wheel scrolls an element that only scrolls horizontally`() {
        val div = scroller(contentHeight = 100.0)
        scroll(div, 0.0, -4.0)
        assertEquals(40.0, div.scrollLeft)
        // a horizontal wheel too
        scroll(div, 2.0, 0.0)
        assertEquals(20.0, div.scrollLeft)
    }

    @Test
    fun `a vertical wheel scrolls horizontally with shift`() {
        val div = scroller()
        scroll(div, 0.0, -4.0, KeyModifier.SHIFT)
        assertEquals(40.0, div.scrollLeft)
        assertEquals(0.0, div.scrollTop)
    }

    @Test
    fun `scrollbars show along the axes that overflow`() {
        assertEquals(listOf(true), scroller(contentWidth = 100.0).scrollbars().map { it.vertical })
        assertEquals(listOf(false), scroller(contentHeight = 100.0).scrollbars().map { it.vertical })
        assertTrue(scroller(100.0, 100.0).scrollbars().isEmpty())
        val hidden = scroller().apply { style { overflow = Overflow.Hidden } }
        relayout(hidden)
        assertTrue(hidden.scrollbars().isEmpty())
    }

    @Test
    fun `thumbs are sized and placed by the scroll position`() {
        val div = scroller(contentWidth = 100.0, contentHeight = 400.0)
        val bar = div.scrollbars().single()
        // a track inset by 2 pixels at the right, a thumb as long as the visible part of the content
        assertEquals(92.0, bar.track.x)
        assertEquals(96.0, bar.track.height)
        assertEquals(24.0, bar.thumb.height)
        assertEquals(2.0, bar.thumb.y)
        div.scrollTop = div.maxScrollTop
        val end = div.scrollbars().single()
        assertEquals(end.track.y + end.track.height, end.thumb.y + end.thumb.height, 1E-9)
        // a pixel of the thumb scrolls the content by its share
        assertEquals(300.0 / 72.0, end.scrollPerPixel, 1E-9)
        end.position = 1E6
        assertEquals(300.0, div.scrollTop)
    }

    @Test
    fun `scrollbars leave the corner free and are found by position`() {
        val div = scroller()
        val (vertical, horizontal) = div.scrollbars()
        assertEquals(88.0, vertical.track.height)
        assertEquals(88.0, horizontal.track.width)
        val root = generateSequence<Element>(div) { it.parent }.last()
        assertSame(div, scrollbarAt(root, Vector2(95.0, 10.0))?.element)
        assertEquals(false, scrollbarAt(root, Vector2(10.0, 95.0))?.vertical)
        assertEquals(null, scrollbarAt(root, Vector2(50.0, 50.0)))
    }

    @Test
    fun `scrollbars follow the style`() {
        val div = scroller(contentWidth = 100.0).apply { style { scrollbarWidth = 0.px } }
        relayout(div)
        assertTrue(div.scrollbars().isEmpty())
    }

    @Test
    fun `highlights are lighter and more opaque`() {
        val translucent = ColorRGBa(1.0, 1.0, 1.0, 0.35)
        assertEquals(0.525, highlighted(translucent, 0.5).alpha, 1E-9)
        assertEquals(0.7, highlighted(translucent, 1.0).alpha, 1E-9)
        // an opaque color gets lighter
        val orange = ColorRGBa(0.9, 0.6, 0.2, 1.0)
        val hovered = highlighted(orange, 0.5)
        val dragged = highlighted(orange, 1.0)
        assertEquals(1.0, hovered.alpha, 1E-9)
        assertTrue(orange.b < hovered.b && hovered.b < dragged.b)
        assertEquals(orange, highlighted(orange, 0.0))
    }

    @Test
    fun `scrollbars are the same when they are of the same element and axis`() {
        val div = scroller()
        val (vertical, horizontal) = div.scrollbars()
        val again = div.scrollbars()
        assertTrue(vertical.sameAs(again[0]))
        assertTrue(!vertical.sameAs(horizontal))
        assertTrue(!vertical.sameAs(null))
        assertTrue(null.sameAs(null))
    }

    @Test
    fun `scrollbars fade out when the element does not scroll`() {
        var now = 10.0
        val fades = ScrollbarFades { now }
        val div = scroller()
        // hidden until the element scrolls
        assertEquals(0.0, fades.opacity(div, false))
        assertTrue(fades.animating().isEmpty())
        div.scrollTop = 20.0
        assertEquals(1.0, fades.opacity(div, false))
        now += 1.0
        assertEquals(1.0, fades.opacity(div, false), 1E-9)
        now += 0.2
        assertEquals(0.5, fades.opacity(div, false), 1E-9)
        assertEquals(listOf<Element>(div), fades.animating())
        now += 0.3
        assertEquals(0.0, fades.opacity(div, false))
        assertTrue(fades.animating().isEmpty())
        // hovering or dragging shows them again
        assertEquals(1.0, fades.opacity(div, true))
        now += 1.2
        assertEquals(0.5, fades.opacity(div, false), 1E-9)
    }

    @Test
    fun `scrollbars can always be shown`() {
        val fades = ScrollbarFades { 0.0 }
        val div = scroller().apply { style { scrollbarVisibility = ScrollbarVisibility.Always } }
        relayout(div)
        assertEquals(1.0, fades.opacity(div, false))
    }
}
