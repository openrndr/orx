import org.openrndr.extra.composition.*
import org.openrndr.extra.svg.parseSVG
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotSame
import kotlin.test.assertTrue

class TestSVGFeatures {
    private fun parse(content: String) = parseSVG(
        """<svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" width="100" height="100">$content</svg>"""
    )

    private fun Composition.shape(id: String) = findShapes().first { it.id == id }
    private fun Composition.fill(id: String) = (shape(id).effectiveStyle.fill as Paint.RGB).value

    @Test
    fun parsesColors() {
        val c = parse(
            """
            <rect id="rgba" width="1" height="1" fill="#f008"/>
            <rect id="rrggbbaa" width="1" height="1" fill="#00ff0080"/>
            <rect id="rgbaFunction" width="1" height="1" fill="rgba(255, 0, 0, 0.25)"/>
            <rect id="spaces" width="1" height="1" fill="rgb(100% 50% 0% / 50%)"/>
            <rect id="hsl" width="1" height="1" fill="hsl(120, 100%, 50%)"/>
            <rect id="hsla" width="1" height="1" fill="hsla(240deg 100% 50% / 0.5)"/>
            <rect id="transparent" width="1" height="1" fill="transparent"/>
            <rect id="current" width="1" height="1" fill="currentColor"/>
            """
        )
        assertEquals(0x88 / 255.0, c.fill("rgba").alpha, 1e-9)
        assertEquals(1.0, c.fill("rrggbbaa").g, 1e-9)
        assertEquals(0x80 / 255.0, c.fill("rrggbbaa").alpha, 1e-9)
        assertEquals(0.25, c.fill("rgbaFunction").alpha, 1e-9)
        assertEquals(0.5, c.fill("spaces").g, 1e-9)
        assertEquals(0.5, c.fill("spaces").alpha, 1e-9)
        assertEquals(1.0, c.fill("hsl").g, 1e-9)
        assertEquals(0.0, c.fill("hsl").r, 1e-9)
        assertEquals(1.0, c.fill("hsla").b, 1e-9)
        assertEquals(0.5, c.fill("hsla").alpha, 1e-9)
        assertEquals(0.0, c.fill("transparent").alpha, 1e-9)
        assertIs<Paint.CurrentColor>(c.shape("current").effectiveStyle.fill)
    }

    @Test
    fun appliesStyleSheets() {
        val c = parse(
            """
            <style>
                /* a comment */
                .a { fill: #ff0000; stroke: #000000 }
                rect.a { stroke: none }
                #b { fill: #0000ff }
                g .c { fill: #00ff00 }
                @media print { .a { fill: #000000 } }
            </style>
            <rect id="a" class="a" width="1" height="1" stroke="#ffffff"/>
            <rect id="b" class="a" width="1" height="1"/>
            <g><rect id="c" class="c" width="1" height="1"/></g>
            <rect id="inline" class="a" width="1" height="1" style="fill: #ffff00"/>
            """
        )
        assertEquals(1.0, c.fill("a").r)
        // a more specific rule overrides, CSS overrides presentation attributes
        assertIs<Paint.None>(c.shape("a").effectiveStyle.stroke)
        assertEquals(1.0, c.fill("b").b)
        assertEquals(1.0, c.fill("c").g)
        // the style attribute overrides CSS
        assertEquals(1.0, c.fill("inline").g)
        assertEquals(1.0, c.fill("inline").r)
    }

    @Test
    fun readsDisplayVisibilityAndVectorEffect() {
        val c = parse(
            """
            <rect id="none" width="1" height="1" display="none"/>
            <g visibility="hidden"><rect id="hidden" width="1" height="1"/></g>
            <rect id="scaling" width="1" height="1" style="vector-effect: non-scaling-stroke"/>
            """
        )
        assertEquals(Display.None, c.shape("none").style.display)
        assertEquals(Visibility.Hidden, c.shape("hidden").effectiveStyle.visibility)
        assertEquals(VectorEffect.NonScalingStroke, c.shape("scaling").style.vectorEffect)
    }

    @Test
    fun usesElementsAndSymbols() {
        val c = parse(
            """
            <defs>
                <rect id="r" width="10" height="10"/>
                <symbol id="s" viewBox="0 0 10 10"><rect id="inSymbol" width="10" height="10"/></symbol>
            </defs>
            <use xlink:href="#r" x="5" y="6"/>
            <use href="#r" x="20"/>
            <use href="#s" x="40" width="20" height="20"/>
            <use href="#missing"/>
            """
        )
        val copies = c.findShapes().filter { it.id == "r" }
        assertEquals(2, copies.size)
        // copies have their own styles
        assertNotSame(copies[0].style, copies[1].style)
        assertEquals(5.0, copies[0].effectiveShape.bounds.x, 1e-9)
        assertEquals(6.0, copies[0].effectiveShape.bounds.y, 1e-9)
        val symbol = c.findShapes().first { it.id == "inSymbol" }
        // the symbol is fitted into 20 by 20 at x 40
        assertEquals(40.0, symbol.effectiveShape.bounds.x, 1e-9)
        assertEquals(20.0, symbol.effectiveShape.bounds.width, 1e-9)
        // the symbol itself is not drawn where it is defined, only where it is used
        assertEquals(1, c.findShapes().count { it.id == "inSymbol" })
    }

    @Test
    fun drawsTheFirstChildOfSwitches() {
        val c = parse("""<switch><rect id="first" width="1" height="1"/><rect id="second" width="1" height="1"/></switch>""")
        assertTrue(c.findShapes().any { it.id == "first" })
        assertTrue(c.findShapes().none { it.id == "second" })
    }
}
