import org.openrndr.extra.composition.*
import org.openrndr.extra.svg.parseSVG
import org.openrndr.extra.svg.writeSVG
import org.openrndr.math.Vector2
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TestSVGHitTest {
    private val c = parseSVG(
        """<svg xmlns="http://www.w3.org/2000/svg" width="200" height="100" viewBox="0 0 400 200">
            <rect id="back" width="400" height="200" fill="white"/>
            <a href="https://openrndr.org">
                <g transform="translate(100 0)">
                    <rect id="linked" x="0" y="0" width="40" height="40" fill="red"/>
                </g>
            </a>
            <path id="ring" d="M 200 20 h 60 v 60 h -60 z M 210 30 h 40 v 40 h -40 z" fill="blue" fill-rule="evenodd"/>
            <line id="line" x1="300" y1="0" x2="300" y2="100" stroke="black" stroke-width="10"/>
            <rect id="hidden" x="0" y="100" width="40" height="40" visibility="hidden"/>
            <rect id="none" x="0" y="100" width="40" height="40" fill="none"/>
            <rect id="anyway" x="50" y="100" width="40" height="40" fill="none" pointer-events="all"/>
            <rect id="ghost" x="100" y="100" width="40" height="40" pointer-events="none"/>
            <defs><clipPath id="half"><rect x="150" y="100" width="20" height="40"/></clipPath></defs>
            <rect id="clipped" x="150" y="100" width="40" height="40" clip-path="url(#half)"/>
        </svg>"""
    )

    /** the id of the topmost node at a point of the composition, in its coordinates of 200 by 100 */
    private fun at(x: Double, y: Double) = c.nodeAt(Vector2(x, y))?.id

    @Test
    fun hitsPaintedShapes() {
        // the view box halves the coordinates
        assertEquals("linked", at(60.0, 10.0))
        assertEquals("back", at(10.0, 10.0))
        // even-odd: the hole of the ring is not filled
        assertEquals("ring", at(102.0, 12.0))
        assertEquals("back", at(115.0, 25.0))
        // strokes are hit within half their weight
        assertEquals("line", at(151.0, 20.0))
        assertEquals("back", at(155.0, 20.0))
    }

    @Test
    fun followsPointerEventsAndClips() {
        // hidden and unpainted nodes are not hit, all is hit unpainted, none is never hit
        assertEquals("back", at(10.0, 60.0))
        assertEquals("anyway", at(35.0, 60.0))
        assertEquals("back", at(60.0, 60.0))
        // the clip path removes the right half
        assertEquals("clipped", at(80.0, 60.0))
        assertEquals("back", at(90.0, 60.0))
        // all hits, topmost first
        assertEquals(listOf("linked", "back"), c.hitTest(Vector2(60.0, 10.0)).map { it.node.id })
    }

    @Test
    fun knowsLinks() {
        val linked = c.nodeAt(Vector2(60.0, 10.0))!!
        assertEquals("https://openrndr.org", linked.effectiveLink)
        // the point in the coordinates of the node
        assertEquals(Vector2(20.0, 20.0), c.hitTest(Vector2(60.0, 10.0)).first().point)
        assertNull(c.nodeAt(Vector2(10.0, 10.0))!!.effectiveLink)
        assertTrue("<a href=\"https://openrndr.org\"" in writeSVG(c))
        assertEquals("https://openrndr.org", parseSVG(writeSVG(c)).findShapes().first { it.id == "linked" }.effectiveLink)
    }
}
