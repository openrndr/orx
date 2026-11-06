import org.openrndr.extra.composition.*
import org.openrndr.extra.svg.parseSVG
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class TestSVGColorFunctions {
    /** the 8-bit sRGB components of [fill] */
    private fun rgb(fill: String): List<Int> {
        val c = parseSVG("""<svg xmlns="http://www.w3.org/2000/svg"><rect id="r" width="1" height="1" fill="$fill"/></svg>""")
        val color = (c.findShapes().single().style.fill as Paint.RGB).value.toSRGB()
        return listOf(color.r, color.g, color.b).map { (it * 255.0).roundToInt() }
    }

    private fun assertRgb(expected: List<Int>, fill: String) {
        val actual = rgb(fill)
        for (i in 0..2) assertEquals(expected[i].toDouble(), actual[i].toDouble(), 1.0, "$fill: $actual")
    }

    @Test
    fun convertsColorSpaces() {
        // the values Chrome draws
        assertRgb(listOf(237, 118, 101), "oklch(0.7 0.15 30)")
        assertRgb(listOf(0, 129, 241), "oklch(60% 0.2 250)")
        assertRgb(listOf(159, 99, 186), "oklab(0.6 0.1 -0.1)")
        assertRgb(listOf(187, 88, 70), "lab(50 40 30)")
        assertRgb(listOf(0, 166, 173), "lch(60 50 200)")
        assertRgb(listOf(51, 136, 179), "hwb(200 20% 30%)")
        assertRgb(listOf(124, 188, 89), "color(srgb-linear 0.2 0.5 0.1)")
        assertRgb(listOf(36, 155, 126), "color(display-p3 0.3 0.6 0.5)")
        assertRgb(listOf(89, 128, 155), "color(a98-rgb 0.4 0.5 0.6)")
        assertRgb(listOf(100, 121, 176), "color(prophoto-rgb 0.4 0.4 0.6)")
        assertRgb(listOf(154, 112, 87), "color(rec2020 0.5 0.4 0.3)")
        assertRgb(listOf(151, 147, 165), "color(xyz-d50 0.3 0.3 0.3)")
        // out of gamut colors are clipped
        assertRgb(listOf(255, 0, 0), "color(display-p3 1 0 0)")
    }

    @Test
    fun mixesColors() {
        assertRgb(listOf(147, 89, 114), "color-mix(in srgb, #e8574a, #3d5a99)")
        assertRgb(listOf(109, 96, 138), "color-mix(in oklab, #e8574a 25%, #3d5a99)")
        assertRgb(listOf(186, 0, 194), "color-mix(in oklch, red, blue)")
        assertRgb(listOf(0, 147, 0), "color-mix(in oklch longer hue, red, blue)")
        assertRgb(listOf(97, 173, 112), "color-mix(in lch, #f2b134 30%, #4c9e8a 70%)")
        assertRgb(listOf(188, 188, 188), "color-mix(in srgb-linear, white, black)")
        assertRgb(listOf(61, 90, 153), "light-dark(#3d5a99, #e8574a)")
    }

    @Test
    fun readsAlphaAndInvalidColors() {
        val c = parseSVG("""<svg xmlns="http://www.w3.org/2000/svg">
            <rect id="a" width="1" height="1" fill="oklch(0.7 0.1 40 / 50%)"/>
            <rect id="m" width="1" height="1" fill="color-mix(in srgb, red 30%, blue 30%)"/>
            <rect id="x" width="1" height="1" fill="color(unknown 1 0 0)"/>
        </svg>""")
        assertEquals(0.5, (c.findShapes().first { it.id == "a" }.style.fill as Paint.RGB).value.alpha, 1E-9)
        // percentages that add up to less than 100% make the mix translucent
        assertEquals(0.6, (c.findShapes().first { it.id == "m" }.style.fill as Paint.RGB).value.alpha, 1E-9)
        assertIs<Paint.None>(c.findShapes().first { it.id == "x" }.style.fill)
    }
}
