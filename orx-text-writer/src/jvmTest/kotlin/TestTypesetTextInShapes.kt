import org.openrndr.biditextdriver.BidiTextDriverUax9
import org.openrndr.draw.font.Face
import org.openrndr.draw.font.internal.BidiTextDriver
import org.openrndr.draw.font.internal.FontDriver
import org.openrndr.draw.font.internal.TextShapingDriver
import org.openrndr.draw.font.loadFace
import org.openrndr.extra.textwriter.TextSpan
import org.openrndr.extra.textwriter.TextStyle
import org.openrndr.extra.textwriter.VectorTextDrawer
import org.openrndr.extra.textwriter.addText
import org.openrndr.extra.textwriter.typesetText
import org.openrndr.fontdriver.freetype.FontDriverFreetype
import org.openrndr.math.Vector2
import org.openrndr.shape.Circle
import org.openrndr.shape.Rectangle
import org.openrndr.shape.difference
import org.openrndr.textshapingdriver.harfbuzz.TextShapingDriverHarfBuzz
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestTypesetTextInShapes {
    @BeforeTest
    fun setupDrivers() {
        if (FontDriver.driver == null) FontDriver.driver = FontDriverFreetype()
        if (TextShapingDriver.driver == null) TextShapingDriver.driver = TextShapingDriverHarfBuzz()
        if (BidiTextDriver.driver == null) BidiTextDriver.driver = BidiTextDriverUax9()
    }

    private val face: Face by lazy { loadFace("../demo-data/fonts/IBMPlexMono-Regular.ttf").withSize(16.0) }
    private val style by lazy { TextStyle.Defaults.copy(face = face) }

    private val text = "The wheels of the bus go round and round, round and round, round and round. " +
            "The wheels of the bus go round and round, all through the town. "

    @Test
    fun rectangleShapeMatchesBox() {
        val box = Rectangle(20.0, 30.0, 200.0, 300.0)
        val inBox = typesetText(listOf(TextSpan(text, null)), listOf(box), style)
        val inShape = typesetText(listOf(TextSpan(text, null)), listOf(box.shape), style)
        assertEquals(inBox.size, inShape.size)
        for ((a, b) in inBox.flatMap { it.glyphs }.zip(inShape.flatMap { it.glyphs })) {
            assertEquals(a.position.x, b.position.x, 1E-6)
            assertEquals(a.position.y, b.position.y, 1E-6)
        }
    }

    @Test
    fun textStaysInsideACircle() {
        val circle = Circle(300.0, 300.0, 150.0)
        val lines = typesetText(listOf(TextSpan(text.repeat(4), null)), listOf(circle.shape), style)
        assertTrue(lines.size > 5)
        val advance = face.glyphForCharacter('a').advanceWidth()
        for (glyph in lines.flatMap { it.glyphs }) {
            val top = glyph.position + Vector2(0.0, -face.ascent)
            val bottomRight = glyph.position + Vector2(advance, -face.descent)
            assertTrue(top in circle || top.distanceTo(circle.center) < circle.radius + 1.0, "$glyph")
            assertTrue(bottomRight.distanceTo(circle.center) < circle.radius + 1.0, "$glyph")
        }
    }

    @Test
    fun holesSplitLines() {
        val center = Vector2(300.0, 300.0)
        val ring = difference(Circle(center, 200.0).shape, Circle(center, 80.0).shape)
        val lines = typesetText(listOf(TextSpan(text.repeat(10), null)), listOf(ring), style)
        val glyphs = lines.flatMap { it.glyphs }
        assertTrue(glyphs.none { it.position.distanceTo(center) < 80.0 - 1.0 })
        // lines next to the hole share a baseline
        val baselines = lines.filter { it.glyphs.isNotEmpty() }.groupBy { it.glyphs.first().position.y }
        assertTrue(baselines.values.any { it.size == 2 })
        // the left line on a shared baseline comes first
        val shared = baselines.values.first { it.size == 2 }
        assertTrue(shared[0].glyphs.first().position.x < center.x && shared[1].glyphs.first().position.x > center.x)
    }

    @Test
    fun textThatDoesNotFitIsDiscarded() {
        // a line is 20.8 high (ascent 16.4, descent 4.4), the rectangle holds two lines
        val lines = typesetText(listOf(TextSpan(text, null)), listOf(Rectangle(0.0, 0.0, 200.0, 45.0).shape), style)
        assertEquals(2, lines.size)
    }

    @Test
    fun drawerAddsTextInShapes() {
        val drawer = VectorTextDrawer()
        drawer.style = style
        drawer.addText(listOf(TextSpan("hello world", null)), listOf(Circle(100.0, 100.0, 80.0).shape))
        assertEquals(10, drawer.shapes.size)
    }

    @Test
    fun verticalAlignmentInARectangleMatchesBox() {
        val box = Rectangle(20.0, 30.0, 200.0, 300.0)
        for (verticalAlign in listOf(0.5, 1.0)) {
            val aligned = style.copy(verticalAlign = verticalAlign)
            val inBox = typesetText(listOf(TextSpan(text, null)), listOf(box), aligned)
            val inShape = typesetText(listOf(TextSpan(text, null)), listOf(box.shape), aligned)
            assertEquals(inBox.size, inShape.size)
            for ((a, b) in inBox.flatMap { it.glyphs }.zip(inShape.flatMap { it.glyphs })) {
                assertEquals(a.position.x, b.position.x, 1E-6)
                assertEquals(a.position.y, b.position.y, 0.01)
            }
        }
    }

    @Test
    fun verticalAlignmentInACircleKeepsAllText() {
        val circle = Circle(300.0, 300.0, 150.0)
        val spans = listOf(TextSpan(text, null))
        val top = typesetText(spans, listOf(circle.shape), style)
        val glyphCount = top.sumOf { it.glyphs.size }
        val firstY = { lines: List<org.openrndr.extra.textwriter.TypesetLine> -> lines.first().glyphs.first().position.y }
        val lastY = { lines: List<org.openrndr.extra.textwriter.TypesetLine> -> lines.last().glyphs.first().position.y }

        val center = typesetText(spans, listOf(circle.shape), style.copy(verticalAlign = 0.5))
        val bottom = typesetText(spans, listOf(circle.shape), style.copy(verticalAlign = 1.0))
        for (lines in listOf(center, bottom)) {
            assertEquals(glyphCount, lines.sumOf { it.glyphs.size })
        }
        assertTrue(firstY(top) < firstY(center) && firstY(center) < firstY(bottom))
        // centered text is roughly centered in the circle
        val middle = (firstY(center) - face.ascent + lastY(center) - face.descent) / 2.0
        assertEquals(circle.center.y, middle, 15.0)
    }

    @Test
    fun filledShapesPassTextOnWhenAligned() {
        val shapes = listOf(Rectangle(0.0, 0.0, 200.0, 100.0).shape, Rectangle(300.0, 0.0, 200.0, 400.0).shape)
        val top = typesetText(listOf(TextSpan(text, null)), shapes, style)
        val bottom = typesetText(listOf(TextSpan(text, null)), shapes, style.copy(verticalAlign = 1.0))
        assertEquals(top.count { it.boxIndex == 0 }, bottom.count { it.boxIndex == 0 })
        assertEquals(top.sumOf { it.glyphs.size }, bottom.sumOf { it.glyphs.size })
        // the first shape is full and moves less than a line, the second moves to the bottom
        val firstShapeShift = bottom.first().glyphs.first().position.y - top.first().glyphs.first().position.y
        assertTrue(firstShapeShift in 0.0..face.height)
        assertTrue(bottom.last().glyphs.first().position.y > 400.0 - face.height * 2.0)
    }
}
