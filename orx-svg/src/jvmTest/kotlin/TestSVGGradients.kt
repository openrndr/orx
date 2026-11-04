import org.openrndr.color.ColorRGBa
import org.openrndr.extra.composition.Paint
import org.openrndr.extra.composition.ShapeNode
import org.openrndr.extra.composition.findShapes
import org.openrndr.extra.composition.fill
import org.openrndr.extra.composition.stroke
import org.openrndr.extra.shadestyles.presets.ColorStop
import org.openrndr.extra.shadestyles.presets.GradientSpread
import org.openrndr.extra.shadestyles.presets.PaintUnits
import org.openrndr.extra.svg.parseSVG
import org.openrndr.extra.svg.toSVG
import org.openrndr.math.Matrix44
import org.openrndr.math.Vector2
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class TestSVGGradients {
    private val svg = """
        <svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" width="200" height="100">
            <rect id="forward" x="0" y="0" width="10" height="10" fill="url(#later)"/>
            <defs>
                <linearGradient id="base" x1="10%" y1="0" x2="0.9" y2="50%" spreadMethod="reflect">
                    <stop offset="0" stop-color="#ff0000"/>
                    <stop offset="50%" style="stop-color: blue; stop-opacity: 0.5"/>
                    <stop offset="0.25" stop-color="#00ff00"/>
                </linearGradient>
                <linearGradient id="child" xlink:href="#base" x2="1" gradientTransform="rotate(90)"/>
                <radialGradient id="radial" gradientUnits="userSpaceOnUse" cx="50%" cy="20" r="30" fx="40" fr="5">
                    <stop offset="0" stop-color="white"/>
                    <stop offset="1" stop-color="black"/>
                </radialGradient>
            </defs>
            <rect id="a" x="0" y="0" width="10" height="10" fill="url(#base)" stroke="url(#radial)"/>
            <rect id="b" x="0" y="0" width="10" height="10" style="fill: url('#child')"/>
            <rect id="c" x="0" y="0" width="10" height="10" fill="url(#missing) #ff0000"/>
            <linearGradient id="later"><stop offset="1" stop-color="#000000"/></linearGradient>
        </svg>
    """.trimIndent()

    private val composition = parseSVG(svg)
    private fun node(id: String) = composition.findShapes().first { it.id == id }

    @Test
    fun parsesLinearGradients() {
        val paint = node("a").style.fill
        assertIs<Paint.LinearGradient>(paint)
        assertEquals(Vector2(0.1, 0.0), paint.start)
        assertEquals(Vector2(0.9, 0.5), paint.end)
        assertEquals(PaintUnits.BOUNDS, paint.units)
        assertEquals(GradientSpread.REFLECT, paint.spread)
        // stop offsets are clamped to the previous offset, the style attribute sets color and opacity
        assertEquals(listOf(0.0, 0.5, 0.5), paint.stops.map { it.offset })
        assertEquals(0.5, paint.stops[1].color.alpha)
        assertEquals(1.0, paint.stops[1].color.b)
    }

    @Test
    fun inheritsFromReferencedGradients() {
        val paint = node("b").style.fill
        assertIs<Paint.LinearGradient>(paint)
        assertEquals(Vector2(0.1, 0.0), paint.start)
        assertEquals(Vector2(1.0, 0.5), paint.end)
        assertEquals(3, paint.stops.size)
        assertEquals(GradientSpread.REFLECT, paint.spread)
        assertTrue(paint.transform != Matrix44.IDENTITY)
    }

    @Test
    fun parsesRadialGradientsInUserSpace() {
        val paint = node("a").style.stroke
        assertIs<Paint.RadialGradient>(paint)
        assertEquals(PaintUnits.OBJECT, paint.units)
        assertEquals(Vector2(100.0, 20.0), paint.center)
        assertEquals(30.0, paint.radius)
        assertEquals(Vector2(40.0, 20.0), paint.focal)
        assertEquals(5.0, paint.focalRadius)
    }

    @Test
    fun resolvesForwardReferencesAndFallbacks() {
        assertIs<Paint.LinearGradient>(node("forward").style.fill)
        val fallback = node("c").style.fill
        assertIs<Paint.RGB>(fallback)
        assertEquals(1.0, fallback.value.r)
    }

    @Test
    fun writesGradients() {
        val written = composition.toSVG()
        assertTrue("<linearGradient id=\"base\"" in written)
        assertTrue("fill=\"url(#base)\"" in written)
        val reread = parseSVG(written)
        val paint = reread.findShapes().first { it.id == "a" }.style.fill
        assertIs<Paint.LinearGradient>(paint)
        assertEquals((node("a").style.fill as Paint.LinearGradient).stops.map { it.offset }, paint.stops.map { it.offset })
        assertEquals(Vector2(0.9, 0.5), paint.end)
    }

    @Test
    fun gradientsWithoutIdGetAnIdFromTheirValues() {
        val a = Paint.LinearGradient(Vector2.ZERO, Vector2.ONE, listOf(ColorStop(0.0, ColorRGBa.RED)))
        val b = Paint.LinearGradient(Vector2.ZERO, Vector2.ONE, listOf(ColorStop(0.0, ColorRGBa.RED)))
        val c = Paint.LinearGradient(Vector2.ZERO, Vector2.ONE, listOf(ColorStop(0.0, ColorRGBa.BLUE)))
        assertEquals(a.svgId, b.svgId)
        assertTrue(a.svgId != c.svgId)
    }
}
