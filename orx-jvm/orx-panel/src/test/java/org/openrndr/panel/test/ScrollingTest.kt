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
import org.openrndr.panel.SmoothScrolling
import org.openrndr.panel.StyleTransitions
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

    @Test
    fun `overflow can differ per axis`() {
        val div = scroller().apply { style { width = 100.px; height = 100.px; overflow = Overflow.Scroll; overflowX = Overflow.Hidden } }
        relayout(div)
        assertEquals(200.0, div.maxScrollTop)
        // the wheel scrolls vertically only, the scrollbar is vertical only; the hidden axis is scrolled by the program
        scroll(div, -3.0, -3.0)
        assertEquals(0.0, div.scrollLeft)
        assertEquals(30.0, div.scrollTop)
        assertEquals(listOf(true), div.scrollbars().map { it.vertical })
        div.scrollLeft = 50.0
        relayout(div)
        assertEquals(50.0, div.scrollLeft)

        // along a visible axis the content does not scroll at all
        div.style { overflowX = Overflow.Visible }
        relayout(div)
        assertEquals(0.0, div.scrollLeft)
        assertEquals(0.0, div.maxScrollLeft)
    }

    @Test
    fun `elements are scrolled into view`() {
        val body = Body(null).apply { style { width = 400.px; height = 300.px } }
        val outer = Div().apply { style { width = 200.px; height = 200.px; overflow = Overflow.Scroll } }
        val inner = Div().apply { style { width = 200.px; height = 100.px; marginTop = 250.px; overflow = Overflow.Scroll } }
        body.append(outer)
        outer.append(inner)
        val items = List(6) { Div().apply { style { width = 100.px; height = 50.px } }.also { inner.append(it) } }
        relayout(body)

        // the inner element scrolls the item to its bottom, the outer one scrolls the inner element to its bottom
        items[3].scrollIntoView()
        assertEquals(100.0, inner.scrollTop)
        assertEquals(150.0, outer.scrollTop)
        relayout(body)
        assertEquals(200.0 - 50.0, items[3].layout.screenY, 1E-9)

        // back to the top of the inner element, which the outer element already shows
        items[0].scrollIntoView(margin = 4.0)
        assertEquals(0.0, inner.scrollTop)
        assertEquals(150.0, outer.scrollTop)
        relayout(body)
        // the inner element ends at the bottom of the outer one, the margin below it is beyond the content
        inner.scrollIntoView(margin = 4.0)
        assertEquals(150.0, outer.scrollTop)
    }

    @Test
    fun `smooth scrolling slows down towards its target`() {
        var now = 0.0
        val scrolling = SmoothScrolling { now }
        val div = scroller(contentWidth = 100.0)
        scrolling.scrollTo(div, 0.0, 150.0)
        val positions = (1..30).map { now += 1.0 / 60.0; scrolling.advance(); div.scrollTop }
        // moving towards the target, in steps that get smaller
        assertTrue(positions.zipWithNext().all { (a, b) -> b >= a })
        assertTrue(positions[1] - positions[0] > positions[6] - positions[5])
        assertEquals(150.0, positions.last())
        assertTrue(!scrolling.scrolling)
        // targets beyond the content stop at its end
        scrolling.scrollTo(div, 0.0, 1E6)
        repeat(60) { now += 1.0 / 60.0; scrolling.advance() }
        assertEquals(200.0, div.scrollTop)
    }

    @Test
    fun `sticky elements stay in view within their parent`() {
        val body = Body(null).apply { style { width = 400.px; height = 300.px } }
        val scroller = Div().apply { style { width = 100.px; height = 100.px; overflow = Overflow.Scroll } }
        body.append(scroller)
        val sections = List(2) {
            Div().also { section ->
                scroller.append(section)
                section.append(Div().apply { style { height = 20.px; position = Position.STICKY } })
                section.append(Div().apply { style { height = 180.px } })
            }
        }
        relayout(body)
        val (first, second) = sections.map { it.children.first() }
        assertEquals(0.0, first.layout.screenY)

        // the header stays at the top while its section scrolls by
        scroller.scrollTop = 50.0
        relayout(body)
        assertEquals(0.0, first.layout.screenY)
        assertEquals(150.0, second.layout.screenY)
        // and leaves with the end of its section, the next header takes its place
        scroller.scrollTop = 190.0
        relayout(body)
        assertEquals(-10.0, first.layout.screenY)
        assertEquals(10.0, second.layout.screenY)
        scroller.scrollTop = 250.0
        relayout(body)
        assertEquals(0.0, second.layout.screenY)
    }

    @Test
    fun `colors and opacity transition`() {
        var now = 0.0
        val transitions = StyleTransitions { now }
        val body = Body(null).apply { style { width = 400.px; height = 300.px } }
        val button = Div().apply {
            style {
                background = Color.RGBa(ColorRGBa.BLACK)
                transitionDuration = 1.0
            }
        }
        body.append(button)
        fun update() {
            Layouter().apply { computeStyles(body); layout(body) }
            transitions.apply(body)
        }
        update()
        button.style { background = Color.RGBa(ColorRGBa.WHITE); opacity = 0.0 }
        update()
        assertEquals(0.0, (button.computedStyle.background as Color.RGBa).color.r, 1E-9)
        now = 0.5
        update()
        assertEquals(0.5, (button.computedStyle.background as Color.RGBa).color.r, 1E-9)
        assertEquals(0.5, button.computedStyle.opacity, 1E-9)
        assertEquals(listOf<Element>(button), transitions.animating)
        // reversing starts from the value that is shown
        button.style { background = Color.RGBa(ColorRGBa.BLACK); opacity = 1.0 }
        update()
        now = 1.0
        update()
        assertEquals(0.25, (button.computedStyle.background as Color.RGBa).color.r, 1E-9)
        now = 2.0
        update()
        assertEquals(0.0, (button.computedStyle.background as Color.RGBa).color.r, 1E-9)
        assertTrue(transitions.animating.isEmpty())
    }
}
