package org.openrndr.panel.test

import org.openrndr.color.ColorRGBa
import org.openrndr.panel.ControlManager
import org.openrndr.panel.elements.*
import org.openrndr.panel.layout.Layouter
import org.openrndr.panel.style.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue

class RedrawTest : PanelTestFixture() {
    private fun ControlManager.frame() = afterDraw(program.drawer, program)

    /** the pixels of the panel */
    private fun ControlManager.pixels(): List<ColorRGBa> {
        val buffer = renderTarget!!.colorBuffer(0)
        buffer.shadow.download()
        return (0 until buffer.effectiveHeight).flatMap { y -> (0 until buffer.effectiveWidth).map { x -> buffer.shadow[x, y] } }
    }

    private fun sheets() = listOf(
        styleSheet(has class_ "a") { background = Color.RGBa(ColorRGBa.BLUE) },
        styleSheet(has class_ "wide") { width = 300.px },
        styleSheet(has class_ "red") { background = Color.RGBa(ColorRGBa.RED) },
    )

    @Test
    fun `styles are computed again for the elements that changed`() {
        lateinit var a: Div
        lateinit var b: Div
        lateinit var inner: Div
        val body = Body(null).apply {
            a = div { inner = div { } }
            b = div("a") { }
        }
        val layouter = Layouter().apply { styleSheets.addAll(sheets().flatMap { it.flatten() }) }
        layouter.computeStyles(body)
        val styleOfB = b.computedStyle
        val styleOfInner = inner.computedStyle

        // nothing changed
        assertEquals(0, layouter.updateStyles(body))
        assertSame(styleOfB, b.computedStyle)

        // a color changes, the element and its descendants are computed again, its sibling is not
        a.classes.add(ElementClass("red"))
        assertEquals(1, layouter.updateStyles(body))
        assertEquals(ColorRGBa.RED, (a.computedStyle.background as Color.RGBa).color)
        assertNotSame(styleOfInner, inner.computedStyle)
        assertSame(styleOfB, b.computedStyle)

        // a size changes
        b.classes.add(ElementClass("wide"))
        assertEquals(2, layouter.updateStyles(body))
        assertEquals(300.0, (b.computedStyle.width as LinearDimension.PX).value)

        // a change of the inline style
        a.style = StyleSheet().apply { marginLeft = 10.px }
        assertEquals(2, layouter.updateStyles(body))
        a.style!!.marginLeft = 20.px
        assertEquals(2, layouter.updateStyles(body))
        assertEquals(20.0, (a.computedStyle.marginLeft as LinearDimension.PX).value)
    }

    @Test
    fun `later siblings are computed again for sibling selectors`() {
        lateinit var a: Div
        lateinit var b: Div
        val body = Body(null).apply {
            a = div { }
            b = div { }
        }
        // the div right after a red div is blue
        val selector = CompoundSelector.parse(".red + div")!!
        val layouter = Layouter().apply {
            styleSheets.addAll((sheets() + StyleSheet(selector).apply { background = Color.RGBa(ColorRGBa.BLUE) }).flatMap { it.flatten() })
        }
        layouter.computeStyles(body)
        a.classes.add(ElementClass("red"))
        layouter.updateStyles(body)
        assertEquals(ColorRGBa.BLUE, (b.computedStyle.background as Color.RGBa).color)
    }

    @Test
    fun `changes that only paint draw only the area of the element, the same as drawing everything`() {
        lateinit var a: Div
        lateinit var b: Button
        val cm = panel {
            styleSheets(sheets())
            layout {
                a = div { style = StyleSheet().apply { width = 100.px; height = 40.px } }
                b = button { label = "b" }
            }
        }
        cm.frame()
        val full = cm.fullRedraws
        val partial = cm.partialRedraws

        // nothing changed, nothing is drawn
        cm.frame()
        assertEquals(full, cm.fullRedraws)
        assertEquals(partial, cm.partialRedraws)

        // a color changes
        a.classes.add(ElementClass("red"))
        cm.frame()
        assertEquals(full, cm.fullRedraws)
        assertEquals(partial + 1, cm.partialRedraws)
        val drawnInPart = cm.pixels()
        cm.partialRedraw = false
        cm.body!!.draw.dirty = true
        cm.frame()
        assertEquals(full + 1, cm.fullRedraws)
        assertTrue(drawnInPart == cm.pixels(), "drawing the area of the element is drawing everything")
        cm.partialRedraw = true

        // a size changes, everything is laid out and drawn
        a.style!!.width = 300.px
        cm.frame()
        assertEquals(full + 2, cm.fullRedraws)
        assertEquals(300.0, a.layout.screenWidth)

        // the label of the button changes its width
        val width = b.layout.screenWidth
        b.label = "a much longer label"
        b.requestRedraw()
        cm.frame()
        assertEquals(full + 3, cm.fullRedraws)
        assertTrue(b.layout.screenWidth > width)
    }
}
