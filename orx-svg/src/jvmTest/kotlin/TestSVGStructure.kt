import org.openrndr.extra.composition.*
import org.openrndr.extra.shadestyles.presets.GradientInterpolation
import org.openrndr.extra.svg.parseSVG
import org.openrndr.extra.svg.writeSVG
import org.openrndr.math.Vector2
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class TestSVGStructure {
    private fun parse(content: String) = parseSVG(
        """<svg xmlns="http://www.w3.org/2000/svg" width="200" height="100">$content</svg>"""
    )

    private fun Composition.shape(id: String) = findShapes().first { it.id == id }

    private fun assertBounds(expected: List<Double>, node: ShapeNode) {
        val b = node.effectiveShape.bounds
        listOf(b.x, b.y, b.width, b.height).zip(expected).forEach { (a, e) -> assertEquals(e, a, 1E-6) }
    }

    @Test
    fun resolvesUnits() {
        val c = parse(
            """
            <g font-size="10">
                <circle id="em" cx="5em" cy="2ex" r="1em"/>
            </g>
            <rect id="percent" x="10%" y="10%" width="50%" height="50%"/>
            <line id="units" x1="0" y1="0" x2="1in" y2="0" stroke="black"/>
            """
        )
        assertBounds(listOf(40.0, 0.0, 20.0, 20.0), c.shape("em"))
        assertBounds(listOf(20.0, 10.0, 100.0, 50.0), c.shape("percent"))
        assertEquals(96.0, c.shape("units").effectiveShape.bounds.width, 1E-6)
    }

    @Test
    fun placesNestedSVG() {
        val c = parse(
            """
            <svg x="10" y="20" width="50" height="50" viewBox="0 0 10 10">
                <rect id="inner" width="10" height="10"/>
            </svg>
            <svg x="100" width="20" height="20" overflow="visible">
                <rect id="visible" width="10" height="10"/>
            </svg>
            """
        )
        assertBounds(listOf(10.0, 20.0, 50.0, 50.0), c.shape("inner"))
        val clipped = c.findGroups().filter { it.style.clipPath is ClipPath.Shapes }
        assertEquals(1, clipped.size)
        assertBounds(listOf(100.0, 0.0, 10.0, 10.0), c.shape("visible"))
    }

    @Test
    fun appliesCSSTransforms() {
        val c = parse(
            """
            <rect id="attribute" width="10" height="10" transform="translate(10px, 5px) rotate(0.25turn)"/>
            <rect id="style" width="10" height="10" style="transform: translateX(20px) scale(2); transform-origin: 5px 5px"/>
            <rect id="percent" x="10" y="10" width="10" height="10" style="transform: rotate(90deg); transform-origin: 50% 50%"/>
            """
        )
        assertBounds(listOf(0.0, 5.0, 10.0, 10.0), c.shape("attribute"))
        assertBounds(listOf(15.0, -5.0, 20.0, 20.0), c.shape("style"))
        // the origin is relative to the viewport: rotating around (100, 50)
        assertBounds(listOf(130.0, -40.0, 10.0, 10.0), c.shape("percent"))
    }

    @Test
    fun readsColorInterpolation() {
        val c = parse(
            """
            <defs>
                <linearGradient id="linear" color-interpolation="linearRGB">
                    <stop offset="0" stop-color="red"/><stop offset="1" stop-color="blue"/>
                </linearGradient>
                <linearGradient id="srgb">
                    <stop offset="0" stop-color="red"/><stop offset="1" stop-color="blue"/>
                </linearGradient>
            </defs>
            <rect id="a" width="10" height="10" fill="url(#linear)"/>
            <rect id="b" width="10" height="10" fill="url(#srgb)"/>
            """
        )
        fun interpolation(id: String) =
            (c.shape(id).effectiveStyle.fill as Paint.Gradient).interpolation
        assertEquals(GradientInterpolation.LINEAR_RGB, interpolation("a"))
        assertEquals(GradientInterpolation.SRGB, interpolation("b"))
        val written = writeSVG(c)
        assertTrue("color-interpolation=\"linearRGB\"" in written)
    }
}
