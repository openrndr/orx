import org.openrndr.extra.composition.*
import org.openrndr.extra.svg.parseSVG
import org.openrndr.extra.svg.writeSVG
import org.openrndr.math.Vector2
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TestSVGMarkers {
    private val c = parseSVG(
        """<svg xmlns="http://www.w3.org/2000/svg" width="200" height="100">
            <style>.dots { marker: url(#dot) }</style>
            <defs>
                <marker id="arrow" viewBox="0 0 10 10" refX="10" refY="center" markerWidth="6" markerHeight="4"
                        orient="auto-start-reverse">
                    <path id="head" d="M 0 0 L 10 5 L 0 10 z"/>
                </marker>
                <marker id="dot" markerUnits="userSpaceOnUse" orient="45deg" overflow="visible">
                    <circle cx="0" cy="0" r="2"/>
                </marker>
            </defs>
            <path id="a" d="M 0 0 L 100 0" stroke="black" marker-start="url(#arrow)" marker-end="url(#arrow)"/>
            <polyline id="b" class="dots" points="0,0 10,10 20,0" stroke="black"/>
            <path id="c" d="M 0 0 L 10 0" marker-start="url(#missing)"/>
        </svg>"""
    )

    private fun Composition.shape(id: String) = findShapes().first { it.id == id }

    @Test
    fun parsesMarkers() {
        val style = c.shape("a").effectiveStyle
        val arrow = assertNotNull(style.markerStart.value)
        assertEquals(arrow, style.markerEnd.value)
        assertNull(style.markerMid.value)
        assertEquals("arrow", arrow.id)
        assertEquals(Vector2(10.0, 5.0), arrow.reference)
        assertEquals(Vector2(6.0, 4.0), arrow.size)
        assertTrue(arrow.strokeWidthUnits)
        assertIs<MarkerOrient.AutoStartReverse>(arrow.orient)
        // clipped to its viewport by default
        assertIs<ClipPath.Shapes>(arrow.content.style.clipPath)

        val dot = assertNotNull(c.shape("b").effectiveStyle.markerMid.value)
        assertEquals(dot, c.shape("b").effectiveStyle.markerStart.value)
        assertEquals(Vector2(3.0, 3.0), dot.size)
        assertEquals(false, dot.strokeWidthUnits)
        assertIs<ClipPath.None>(dot.content.style.clipPath)
        assertEquals(45.0, (dot.orient as MarkerOrient.Angle).degrees, 1E-9)

        assertNull(c.shape("c").effectiveStyle.markerStart.value)
        // marker content is not drawn where it is defined
        assertEquals(listOf("a", "b", "c"), c.findShapes().map { it.id })
    }

    @Test
    fun fitsContent() {
        val arrow = c.shape("a").effectiveStyle.markerStart.value!!
        // the view box 10x10 fits in 6x4 with a scale of 0.4, centered horizontally; the reference is the origin
        val tip = (arrow.contentTransform * Vector2(10.0, 5.0).xy01).xy
        assertEquals(0.0, tip.x, 1E-9)
        assertEquals(0.0, tip.y, 1E-9)
        val corner = (arrow.contentTransform * Vector2(0.0, 0.0).xy01).xy
        assertEquals(-4.0, corner.x, 1E-9)
        assertEquals(-2.0, corner.y, 1E-9)
    }

    @Test
    fun writesMarkers() {
        val written = writeSVG(c)
        assertTrue("<marker id=\"arrow\"" in written)
        assertTrue("marker-start=\"url(#arrow)\"" in written)
        val reread = parseSVG(written)
        val arrow = assertNotNull(reread.findShapes().first { it.id == "a" }.effectiveStyle.markerEnd.value)
        assertEquals(Vector2(10.0, 5.0), arrow.reference)
        assertIs<MarkerOrient.AutoStartReverse>(arrow.orient)
    }

    @Test
    fun parsesSmoothQuadratics() {
        val s = parseSVG(
            """<svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
                <path id="t" d="M 0 0 Q 10 10 20 0 T 40 0 t 20 0"/>
                <path id="lt" d="M 0 0 L 10 0 T 20 0"/>
            </svg>"""
        )
        val t = s.findShapes().first { it.id == "t" }.shape.contours.single().segments
        assertEquals(3, t.size)
        assertEquals(Vector2(30.0, -10.0), t[1].control.single())
        assertEquals(Vector2(60.0, 0.0), t[2].end)
        assertEquals(Vector2(50.0, 10.0), t[2].control.single())
        // without a preceding quadratic the control point is the current point
        val lt = s.findShapes().first { it.id == "lt" }.shape.contours.single().segments
        assertEquals(Vector2(10.0, 0.0), lt[1].control.single())
    }
}
