import org.openrndr.extra.composition.Paint
import org.openrndr.extra.composition.ShapeNode
import org.openrndr.extra.composition.fill
import org.openrndr.extra.composition.findShapes
import org.openrndr.extra.shadestyles.presets.PaintUnits
import org.openrndr.extra.svg.parseSVG
import org.openrndr.extra.svg.toSVG
import org.openrndr.math.Matrix44
import org.openrndr.shape.Rectangle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class TestSVGPatterns {
    private val svg = """
        <svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" width="200" height="100">
            <rect id="a" width="10" height="10" fill="url(#base)"/>
            <defs>
                <linearGradient id="g"><stop offset="0" stop-color="#ff0000"/></linearGradient>
                <pattern id="base" width="0.25" height="50%" patternContentUnits="objectBoundingBox" patternTransform="rotate(30)">
                    <rect width="1" height="1" fill="url(#g)"/>
                    <circle cx="0.5" cy="0.5" r="0.25" fill="url(#inner)"/>
                </pattern>
                <pattern id="inner" patternUnits="userSpaceOnUse" x="2" y="3" width="20" height="10" viewBox="0 0 4 2" preserveAspectRatio="none">
                    <rect width="2" height="2" fill="#000000"/>
                </pattern>
                <pattern id="child" xlink:href="#base" width="0.5"/>
            </defs>
            <rect id="b" width="10" height="10" fill="url(#child)"/>
            <pattern id="outside"><rect width="1" height="1"/></pattern>
        </svg>
    """.trimIndent()

    private val composition = parseSVG(svg)
    private fun fill(id: String) = composition.findShapes().first { it.id == id }.style.fill

    @Test
    fun parsesPatterns() {
        val pattern = fill("a")
        assertIs<Paint.Pattern>(pattern)
        assertEquals(PaintUnits.BOUNDS, pattern.units)
        assertEquals(PaintUnits.BOUNDS, pattern.contentUnits)
        assertEquals(Rectangle(0.0, 0.0, 0.25, 0.5), pattern.tile)
        assertTrue(pattern.transform != Matrix44.IDENTITY)
        assertEquals(2, pattern.content.children.size)
        // the content refers to gradients and patterns
        val contentShapes = pattern.content.findShapes()
        assertIs<Paint.LinearGradient>(contentShapes[0].style.fill)
        val inner = contentShapes[1].style.fill
        assertIs<Paint.Pattern>(inner)
        assertEquals(PaintUnits.OBJECT, inner.units)
        assertEquals(Rectangle(2.0, 3.0, 20.0, 10.0), inner.tile)
        assertEquals(Rectangle(0.0, 0.0, 4.0, 2.0), inner.viewBox)
    }

    @Test
    fun inheritsFromReferencedPatterns() {
        val child = fill("b")
        assertIs<Paint.Pattern>(child)
        assertEquals(Rectangle(0.0, 0.0, 0.5, 0.5), child.tile)
        assertEquals(PaintUnits.BOUNDS, child.contentUnits)
        assertEquals(2, child.content.children.size)
    }

    @Test
    fun patternsAreNotDrawnWhereTheyAreDefined() {
        // only the two rectangles are shapes of the document, the content of patterns is not
        assertEquals(listOf("a", "b"), composition.findShapes().map { it.id })
    }

    @Test
    fun writesPatterns() {
        val written = composition.toSVG()
        assertTrue("<pattern id=\"base\"" in written)
        assertTrue("fill=\"url(#base)\"" in written)
        val reread = parseSVG(written)
        val pattern = reread.findShapes().first { it.id == "a" }.style.fill
        assertIs<Paint.Pattern>(pattern)
        assertEquals(Rectangle(0.0, 0.0, 0.25, 0.5), pattern.tile)
        // the content is written in a group
        assertEquals(2, pattern.content.findShapes().size)
        val inner = pattern.content.findShapes()[1].style.fill
        assertIs<Paint.Pattern>(inner)
        assertNotNull(inner.viewBox)
    }
}
