import org.openrndr.color.ColorRGBa
import org.openrndr.extra.composition.*
import org.openrndr.extra.svg.parseSVG
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class TestSVGCSS {
    private fun parse(content: String) = parseSVG(
        """<svg xmlns="http://www.w3.org/2000/svg" width="200" height="100">$content</svg>"""
    )

    private fun Composition.shape(id: String) = findShapes().first { it.id == id }
    private fun Composition.fill(id: String) = (shape(id).effectiveStyle.fill as Paint.RGB).value

    private fun assertColor(expected: ColorRGBa, actual: ColorRGBa) {
        assertEquals(expected.r, actual.r, 1E-3)
        assertEquals(expected.g, actual.g, 1E-3)
        assertEquals(expected.b, actual.b, 1E-3)
    }

    @Test
    fun resolvesCustomProperties() {
        val c = parse(
            """
            <style>
                svg { --accent: #ff0000; --size: 10px }
                .a { fill: var(--accent) }
                .b { --accent: #0000ff }
                .c { fill: var(--missing, var(--accent)) }
            </style>
            <rect id="a" class="a" width="1" height="1"/>
            <g class="b"><rect id="b" class="a" width="1" height="1"/></g>
            <rect id="fallback" class="c" width="1" height="1"/>
            <rect id="undefined" fill="#00ff00" style="fill: var(--missing)" width="1" height="1"/>
            <rect id="attribute" fill="var(--accent)" width="1" height="1"/>
            <rect id="calc" width="calc(10px * 3 + 10%)" height="1"/>
            """
        )
        assertColor(ColorRGBa.RED, c.fill("a"))
        assertColor(ColorRGBa.BLUE, c.fill("b"))
        assertColor(ColorRGBa.RED, c.fill("fallback"))
        // a declaration with a custom property that is not defined is unset, the fill is inherited
        assertColor(ColorRGBa.BLACK, c.fill("undefined"))
        assertColor(ColorRGBa.RED, c.fill("attribute"))
        assertEquals(50.0, c.shape("calc").shape.bounds.width, 1E-9)
    }

    @Test
    fun appliesGeometryProperties() {
        val c = parse(
            """
            <style>
                :root { --size: 30px }
                rect.wide { width: calc(var(--size) * 2); x: 5px }
                circle { r: 7px }
            </style>
            <rect id="r" class="wide" width="10" height="var(--size)"/>
            <circle id="c" r="2"/>
            """
        )
        val r = c.shape("r").shape.bounds
        assertEquals(5.0, r.x, 1E-9)
        assertEquals(60.0, r.width, 1E-9)
        assertEquals(30.0, r.height, 1E-9)
        assertEquals(14.0, c.shape("c").shape.bounds.width, 1E-9)
    }

    @Test
    fun ordersByImportance() {
        val c = parse(
            """
            <style>
                .important { fill: #ff0000 !important }
                #id { fill: #00ff00 }
            </style>
            <rect id="id" class="important" style="fill: #0000ff" width="1" height="1"/>
            <rect id="inline" style="fill: #ff0000 !important; fill: #0000ff" width="1" height="1"/>
            <rect id="attribute" fill="#ff0000" style="fill: #0000ff" width="1" height="1"/>
            """
        )
        assertColor(ColorRGBa.RED, c.fill("id"))
        assertColor(ColorRGBa.RED, c.fill("inline"))
        // the style attribute overrides presentation attributes, even when it comes first
        val reordered = parseSVG(
            """<svg xmlns="http://www.w3.org/2000/svg"><rect id="r" style="fill: #0000ff" fill="#ff0000" width="1" height="1"/></svg>"""
        )
        assertColor(ColorRGBa.BLUE, reordered.fill("r"))
        assertColor(ColorRGBa.BLUE, c.fill("attribute"))
    }

    @Test
    fun appliesKeywords() {
        val c = parse(
            """
            <g fill="#0000ff" stroke="#ff0000" opacity="0.5">
                <rect id="inherit" fill="#00ff00" style="fill: inherit; opacity: inherit" width="1" height="1"/>
                <rect id="initial" style="fill: initial; stroke: initial" width="1" height="1"/>
                <rect id="unset" fill="#00ff00" opacity="0.2" style="fill: unset; opacity: unset" width="1" height="1"/>
            </g>
            """
        )
        assertColor(ColorRGBa.BLUE, c.fill("inherit"))
        assertEquals(0.5, (c.shape("inherit").style.opacity as Numeric.Rational).value, 1E-9)
        assertColor(ColorRGBa.BLACK, c.fill("initial"))
        assertIs<Paint.None>(c.shape("initial").effectiveStyle.stroke)
        assertColor(ColorRGBa.BLUE, c.fill("unset"))
        assertEquals(1.0, (c.shape("unset").style.opacity as Numeric.Rational).value, 1E-9)
    }

    @Test
    fun evaluatesCalc() {
        val c = parse(
            """
            <rect id="a" x="calc(10px + 5px)" width="calc((100% - 100px) / 2)" height="calc(2 * 1em)" font-size="10"
                  opacity="calc(0.25 * 2)"/>
            """
        )
        val bounds = c.shape("a").shape.bounds
        assertEquals(15.0, bounds.x, 1E-9)
        assertEquals(50.0, bounds.width, 1E-9)
        assertEquals(20.0, bounds.height, 1E-9)
        assertEquals(0.5, (c.shape("a").style.opacity as Numeric.Rational).value, 1E-9)
    }
}
