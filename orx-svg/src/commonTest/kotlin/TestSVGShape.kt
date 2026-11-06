import org.openrndr.extra.svg.svgShape
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import org.openrndr.shape.Shape
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestSVGShape {
    private fun assertBounds(expected: Rectangle, shape: Shape) {
        val b = shape.bounds
        listOf(b.x to expected.x, b.y to expected.y, b.width to expected.width, b.height to expected.height)
            .forEach { (a, e) -> assertEquals(e, a, 1E-6) }
    }

    @Test
    fun readsLinesAndRelativeCommands() {
        val square = svgShape("M 10 10 h 80 v 80 h -80 z")
        assertEquals(1, square.contours.size)
        assertTrue(square.contours.single().closed)
        assertBounds(Rectangle(10.0, 10.0, 80.0, 80.0), square)
        // the same square in absolute commands, without separators
        assertBounds(Rectangle(10.0, 10.0, 80.0, 80.0), svgShape("M10,10L90,10L90,90L10,90Z"))
    }

    @Test
    fun readsCurvesAndSubpaths() {
        val shape = svgShape("M0 0C10 0 10 10 20 10S30 20 40 20M0 50Q10 40 20 50T40 50z")
        assertEquals(2, shape.contours.size)
        assertEquals(false, shape.contours[0].closed)
        assertEquals(true, shape.contours[1].closed)
        assertEquals(Vector2(40.0, 20.0), shape.contours[0].segments.last().end)
        // the reflected control point of the smooth quadratic
        assertEquals(Vector2(30.0, 60.0), shape.contours[1].segments[1].control.single())
    }

    @Test
    fun readsArcsWithCompactFlags() {
        // flags written without separators, as minified paths do
        val compact = svgShape("M0 0a5 5 0 1010 0")
        val spaced = svgShape("M0 0 a 5 5 0 1 0 10 0")
        assertBounds(spaced.bounds, compact)
        assertBounds(Rectangle(0.0, 0.0, 10.0, 5.0), spaced)
    }

    @Test
    fun handlesEmptyAndInvalidData() {
        assertTrue(svgShape("").empty)
        assertTrue(svgShape("none").empty)
        // read up to the error
        val partial = svgShape("M 0 0 L 10 0 L 10")
        assertEquals(Vector2(10.0, 0.0), partial.contours.single().segments.last().end)
    }
}
