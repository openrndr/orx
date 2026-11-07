package org.openrndr.panel.test

import org.openrndr.extra.style.CssMediaEnvironment
import org.openrndr.panel.StyleTransitions
import org.openrndr.panel.elements.*
import org.openrndr.panel.layout.Layouter
import org.openrndr.panel.style.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LayoutFeaturesTest {
    private fun layouter(css: String, width: Double = 400.0, height: Double = 300.0) = Layouter().apply {
        media = CssMediaEnvironment(width, height)
        styleSheets.addAll(styleSheets(css).flatMap { it.flatten() })
    }

    private fun px(value: Any?) = (value as LinearDimension.PX).value

    @Test
    fun `media queries select style sheets by the size of the window`() {
        lateinit var box: Div
        val body = Body(null).apply { box = div("box") { } }
        val css = """
            .box { width: 10px }
            @media (min-width: 600px) { .box { width: 20px } }
        """
        layouter(css, 800.0).computeStyles(body)
        assertEquals(20.0, px(box.computedStyle.width))
        layouter(css, 400.0).computeStyles(body)
        assertEquals(10.0, px(box.computedStyle.width))
    }

    @Test
    fun `relative units are computed in pixels, calc() with percentages in the layout`() {
        lateinit var inner: Div
        lateinit var calc: Div
        val body = Body(null).apply {
            style { width = 400.px; height = 300.px; fontSize = 10.px }
            div("outer") {
                inner = div("inner") { }
                calc = div("calc") { }
            }
        }
        val l = layouter("""
            .outer { font-size: 2em; width: 50vw; height: 100px }
            .inner { width: 3em; height: 1rem; margin-left: 10vh }
            .calc { width: calc(50% - 1em); height: 20px }
        """)
        l.computeStyles(body)
        l.layout(body)
        // 2em of 10px is 20px, 3em of that is 60px, 1rem is the 10px of the root, 10vh is 30px
        assertEquals(60.0, px(inner.computedStyle.width))
        assertEquals(10.0, px(inner.computedStyle.height))
        assertEquals(30.0, px(inner.computedStyle.marginLeft))
        // 50% of the 200px of the outer element less 20px
        assertTrue(calc.computedStyle.width is LinearDimension.Calc)
        assertEquals(80.0, calc.layout.screenWidth, 1E-9)
    }

    @Test
    fun `flex basis, align-self, order and aspect ratio`() {
        lateinit var a: Div
        lateinit var b: Div
        lateinit var c: Div
        lateinit var square: Div
        val body = Body(null).apply {
            style { width = 400.px; height = 300.px }
            div("row") {
                a = div("a") { }
                b = div("b") { }
                c = div("c") { }
            }
            square = div("square") { }
        }
        val l = layouter("""
            .row { display: flex; flex-direction: row; width: 300px; height: 100px; align-items: stretch }
            .a { flex-basis: 50px; height: 20px; align-self: end }
            .b { width: 40px; order: -1 }
            .c { flex: 1; flex-basis: 0 }
            .square { width: 120px; aspect-ratio: 2 / 1 }
        """)
        l.computeStyles(body)
        l.layout(body)
        // b comes first, then a at its basis, then c takes the rest
        assertEquals(0.0, b.layout.screenX - b.parent!!.layout.screenX, 1E-9)
        assertEquals(40.0, a.layout.screenX - a.parent!!.layout.screenX, 1E-9)
        assertEquals(50.0, a.layout.screenWidth, 1E-9)
        assertEquals(210.0, c.layout.screenWidth, 1E-9)
        // a sits at the end of the line, b is stretched
        assertEquals(80.0, a.layout.screenY - a.parent!!.layout.screenY, 1E-9)
        assertEquals(100.0, b.layout.screenHeight, 1E-9)
        assertEquals(60.0, square.layout.screenHeight, 1E-9)
    }

    @Test
    fun `positions from the right and the bottom, and between sides`() {
        lateinit var corner: Div
        lateinit var stretched: Div
        val body = Body(null).apply {
            style { width = 400.px; height = 300.px }
            corner = div("corner") { }
            stretched = div("stretched") { }
        }
        val l = layouter("""
            .corner { position: absolute; right: 10px; bottom: 20px; width: 50px; height: 30px }
            .stretched { position: absolute; inset: 10px 20px; height: 40px }
        """)
        l.computeStyles(body)
        l.layout(body)
        assertEquals(340.0, corner.layout.screenX, 1E-9)
        assertEquals(250.0, corner.layout.screenY, 1E-9)
        assertEquals(20.0, stretched.layout.screenX, 1E-9)
        assertEquals(360.0, stretched.layout.screenWidth, 1E-9)
    }

    @Test
    fun `several animations, paused animations and transitions of lengths and shadows`() {
        var now = 0.0
        val transitions = StyleTransitions { now }
        lateinit var box: Div
        val body = Body(null).apply { box = div("box") { } }
        val l = layouter("""
            @keyframes fade { from { opacity: 0 } to { opacity: 1 } }
            @keyframes grow { from { width: 0px } to { width: 100px } }
            .box { transition: all 1s linear; width: 10px; box-shadow: 0 0 0 black }
            .wide { width: 30px; box-shadow: 0 10px 20px black }
            .animated { animation: fade 1s linear, grow 2s linear paused }
        """)
        fun update(): Boolean {
            for (element in transitions.animating) element.styleDirty = true
            l.updateStyles(body)
            return transitions.apply(body)
        }
        l.computeStyles(body)
        transitions.apply(body)
        box.classes.add(ElementClass("wide"))
        update()
        now = 0.5
        assertTrue(update(), "a transition of a width lays out")
        assertEquals(20.0, px(box.computedStyle.width), 1E-9)
        assertEquals(5.0, box.computedStyle.boxShadow.shadows.single().offset.y, 1E-9)
        now = 2.0
        update()
        box.classes.add(ElementClass("animated"))
        update()
        now = 2.5
        update()
        assertEquals(0.5, box.computedStyle.opacity, 1E-9)
        // the paused animation stays at its start
        assertEquals(0.0, px(box.computedStyle.width), 1E-9)
        assertEquals(listOf(AnimationPlayState.RUNNING, AnimationPlayState.PAUSED), box.computedStyle.animationPlayState)
    }

    @Test
    fun `a virtual list builds the rows in view`() {
        lateinit var list: VirtualList<Int>
        val body = Body(null).apply {
            style { width = 400.px; height = 300.px }
            list = virtualList((0 until 10000).toList(), 20.0) { item, _ -> div { } ; setAttribute("data-item", "$item") }
        }
        val l = layouter("virtual-list { height: 200px; overflow: scroll }")
        list.prepare()
        l.computeStyles(body)
        l.layout(body)
        list.prepare()
        l.computeStyles(body)
        l.layout(body)
        // 10 rows in view and 4 below
        assertEquals(0..13, list.range)
        assertEquals(16, list.children.size)
        assertEquals(10000 * 20.0, list.layout.scrollHeight, 1E-6)
        list.scrollTop = 5000.0
        list.prepare()
        l.computeStyles(body)
        l.layout(body)
        assertEquals(246..263, list.range)
        assertEquals("246", list.children[1].attribute("data-item"))
        assertEquals(246 * 20.0, list.children[0].layout.screenHeight, 1E-6)
    }
}
