import org.openrndr.extra.composition.*
import org.openrndr.extra.shadestyles.presets.PaintUnits
import org.openrndr.extra.svg.parseSVG
import org.openrndr.extra.svg.writeSVG
import org.openrndr.shape.Rectangle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TestSVGLayers {
    private val c = parseSVG(
        """<svg xmlns="http://www.w3.org/2000/svg" width="200" height="100">
            <defs>
                <linearGradient id="fade"><stop offset="0" stop-color="white"/><stop offset="1" stop-color="black"/></linearGradient>
                <mask id="lum"><rect id="inside" width="100" height="100" fill="url(#fade)"/></mask>
                <mask id="alpha" maskUnits="userSpaceOnUse" maskContentUnits="objectBoundingBox" x="10" y="20" width="50%" height="30"
                      style="mask-type: alpha"><rect width="1" height="1"/></mask>
            </defs>
            <rect id="a" width="10" height="10" mask="url(#lum)" style="mix-blend-mode: multiply"/>
            <g id="g" style="isolation: isolate; mask: url(#alpha)"><rect id="b" width="10" height="10"/></g>
            <rect id="c" width="10" height="10" mix-blend-mode="color-dodge"/>
        </svg>"""
    )

    private fun Composition.shape(id: String) = findShapes().first { it.id == id }

    @Test
    fun parsesMasks() {
        val lum = assertNotNull(c.shape("a").style.mask.value)
        assertEquals(MaskType.LUMINANCE, lum.type)
        assertEquals(PaintUnits.OBJECT, lum.units)
        assertEquals(PaintUnits.BOUNDS, lum.regionUnits)
        assertEquals(Rectangle(-0.1, -0.1, 1.2, 1.2), lum.region)
        assertEquals(listOf("inside"), lum.content.findShapes().map { it.id })
        // mask content is not drawn where it is defined
        assertEquals(listOf("a", "b", "c"), c.findShapes().map { it.id })

        val alpha = assertNotNull(c.root.findGroups().first { it.id == "g" }.style.mask.value)
        assertEquals(MaskType.ALPHA, alpha.type)
        assertEquals(PaintUnits.BOUNDS, alpha.units)
        assertEquals(PaintUnits.OBJECT, alpha.regionUnits)
        assertEquals(Rectangle(10.0, 20.0, 100.0, 30.0), alpha.region)
    }

    @Test
    fun parsesBlendModesAndIsolation() {
        assertEquals(MixBlendMode.MULTIPLY, c.shape("a").style.mixBlendMode)
        assertEquals(MixBlendMode.COLOR_DODGE, c.shape("c").style.mixBlendMode)
        assertEquals(MixBlendMode.NORMAL, c.shape("b").style.mixBlendMode)
        assertEquals(Isolation.ISOLATE, c.root.findGroups().first { it.id == "g" }.style.isolation)
    }

    @Test
    fun writesMasks() {
        val written = writeSVG(c)
        assertTrue("<mask id=\"lum\"" in written)
        assertTrue("mask-type=\"alpha\"" in written)
        // blend modes and isolation are not presentation attributes
        assertTrue("style=\"mix-blend-mode:multiply\"" in written)
        assertTrue("isolation:isolate" in written)
        // the gradient in the mask is written too
        assertTrue("<linearGradient id=\"fade\"" in written)
        val reread = parseSVG(written)
        val mask = assertNotNull(reread.findShapes().first { it.id == "a" }.style.mask.value)
        assertIs<Paint.Gradient>(mask.content.findShapes().single().style.fill)
        assertEquals(MixBlendMode.MULTIPLY, reread.findShapes().first { it.id == "a" }.style.mixBlendMode)
        val alpha = assertNotNull(reread.root.findGroups().first { it.id == "g" }.style.mask.value)
        assertEquals(Rectangle(10.0, 20.0, 100.0, 30.0), alpha.region)
    }
}
