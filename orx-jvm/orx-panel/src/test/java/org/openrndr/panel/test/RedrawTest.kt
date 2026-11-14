package org.openrndr.panel.test

import org.openrndr.color.ColorRGBa
import org.openrndr.panel.ControlManager
import org.openrndr.panel.elements.*
import org.openrndr.math.Vector2
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

    @Test
    fun `a text area draws the background of its style over its whole width`() {
        lateinit var area: TextArea
        val cm = panel {
            css(".reply { width: 376px; height: 60px; margin: 0; background-color: rgb(255 0 0) }")
            layout { area = textArea("reply") { } }
        }
        cm.frame(); cm.frame()
        val buffer = cm.renderTarget!!.colorBuffer(0)
        buffer.shadow.download()
        val scale = buffer.effectiveWidth / cm.program.width.toDouble()
        fun at(x: Double, y: Double) = buffer.shadow[(x * scale).toInt(), (y * scale).toInt()]
        val l = area.layout
        // past the numbers of the lines, near the right and the bottom
        for (x in listOf(l.screenX + 100.0, l.screenX + l.screenWidth - 4.0)) {
            val c = at(x, l.screenY + l.screenHeight - 4.0)
            assertTrue(c.r > 0.9 && c.g < 0.1 && c.b < 0.1, "the background at $x is $c")
        }
    }

    @Test
    fun `what follows an element with rounded corners that scrolls is drawn, in a parent that does not clip`() {
        val cm = panel(900.0, 700.0) {
            css("""
                .issue-thread { height: 200px; overflow-y: scroll; background-color: rgb(0 0 255); border-radius: 4px }
                .after { width: 300px; height: 40px; background-color: rgb(255 0 0) }
            """)
            layout { p { "behind" } }
        }
        cm.frame(); cm.frame()
        // the thread clips by its rounded box inside the rounded box of the popover; what follows it, in a div that
        // does not clip, is clipped by the box of the popover only
        lateinit var body: Div
        cm.popover({ org.openrndr.shape.Rectangle(100.0, 100.0, 14.0, 14.0) }) { body = div { } }
        body.apply {
            div("issue-thread") { repeat(3) { p { "message $it" } } }
            div("after") { }
        }
        repeat(4) { cm.frame() }
        val after = body.children.last()
        val buffer = cm.renderTarget!!.colorBuffer(0)
        buffer.shadow.download()
        val scale = buffer.effectiveWidth / cm.program.width.toDouble()
        val p = after.screenArea.center
        val c = buffer.shadow[(p.x * scale).toInt(), (p.y * scale).toInt()]
        assertTrue(c.r > 0.9 && c.g < 0.1 && c.b < 0.1, "what follows the thread at $p is $c, ${after.screenArea}")
    }

    @Test
    fun `a tinted node is drawn in its tint, which a status color overrules on the title bar`() {
        lateinit var editor: NodeEditor
        lateinit var plain: GraphNode
        lateinit var tinted: GraphNode
        val cm = panel {
            css("node-editor { width: 600px; height: 300px }")
            layout {
                editor = nodeEditor {
                    plain = add(GraphNode("plain", listOf(Port("in"))), org.openrndr.math.Vector2(20.0, 20.0))
                    tinted = add(GraphNode("tinted", listOf(Port("in"))), org.openrndr.math.Vector2(250.0, 20.0))
                }
            }
        }
        tinted.tint = ColorRGBa.fromHex("#ff7a1a")
        cm.frame(); cm.frame()
        fun pixel(n: GraphNode, dx: Double, dy: Double): ColorRGBa {
            val buffer = cm.renderTarget!!.colorBuffer(0)
            buffer.shadow.download()
            val scale = buffer.effectiveWidth / cm.program.width.toDouble()
            val p = org.openrndr.math.Vector2(editor.layout.screenX, editor.layout.screenY) + n.position + org.openrndr.math.Vector2(dx, dy)
            return buffer.shadow[(p.x * scale).toInt(), (p.y * scale).toInt()]
        }
        // the title bars, away from the titles: the tinted one is orange, the plain one gray
        val tintedTitle = pixel(tinted, tinted.width - 6.0, 4.0)
        val plainTitle = pixel(plain, plain.width - 6.0, 4.0)
        assertTrue(tintedTitle.r - tintedTitle.b > 0.15, "the tinted title bar is $tintedTitle")
        assertTrue(kotlin.math.abs(plainTitle.r - plainTitle.b) < 0.05, "the plain title bar is $plainTitle")
        // the boxes, faintly
        val tintedBox = pixel(tinted, tinted.width - 6.0, tinted.height - 4.0)
        val plainBox = pixel(plain, plain.width - 6.0, plain.height - 4.0)
        assertTrue(tintedBox.r > plainBox.r + 0.02 && tintedBox.r - tintedBox.b < 0.15, "the boxes are $tintedBox and $plainBox")
        // a status color wins on the title bar
        tinted.statusColor = ColorRGBa.fromHex("#2f80ff")
        cm.frame(); cm.frame()
        val status = pixel(tinted, tinted.width - 6.0, 4.0)
        assertTrue(status.b > status.r + 0.1, "the title bar with a status is $status")
        // compact, still tinted, and the tint follows changes
        tinted.statusColor = null
        tinted.compact = true
        tinted.tint = ColorRGBa.fromHex("#20c060")
        cm.frame(); cm.frame()
        val green = pixel(tinted, tinted.width - 6.0, 4.0)
        assertTrue(green.g > green.r + 0.1 && green.g > green.b + 0.1, "the compact title bar is $green")
    }
}
