import org.openrndr.biditextdriver.BidiTextDriverUax9
import org.openrndr.draw.font.internal.BidiTextDriver
import org.openrndr.draw.font.internal.FontDriver
import org.openrndr.draw.font.internal.TextShapingDriver
import org.openrndr.draw.font.loadFace
import org.openrndr.extra.textwriter.TextStyle
import org.openrndr.extra.textwriter.VectorTextDrawer
import org.openrndr.fontdriver.freetype.FontDriverFreetype
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import org.openrndr.shape.bounds
import org.openrndr.textshapingdriver.harfbuzz.TextShapingDriverHarfBuzz
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// Producing outlines needs font and shaping drivers but no window or GL context
class TestVectorTextDrawer {
    @BeforeTest
    fun setupDrivers() {
        if (FontDriver.driver == null) FontDriver.driver = FontDriverFreetype()
        if (TextShapingDriver.driver == null) TextShapingDriver.driver = TextShapingDriverHarfBuzz()
        if (BidiTextDriver.driver == null) BidiTextDriver.driver = BidiTextDriverUax9()
    }

    private val face by lazy { loadFace("../demo-data/fonts/IBMPlexMono-Regular.ttf").withSize(24.0) }

    @Test
    fun producesAShapePerVisibleGlyph() {
        val drawer = VectorTextDrawer()
        drawer.addText("ab cd", Rectangle(0.0, 0.0, 400.0, 400.0), TextStyle(face))
        assertEquals(4, drawer.shapes.size)
    }

    @Test
    fun shapesAreInsideTheBox() {
        val box = Rectangle(50.0, 60.0, 200.0, 400.0)
        val drawer = VectorTextDrawer()
        drawer.addText("The wheels of the bus go round and round, all through the town.", box, TextStyle(face))
        val bounds = drawer.shapes.map { it.bounds }.bounds
        assertTrue(bounds.x >= box.x - 1.0 && bounds.x + bounds.width <= box.x + box.width + 1.0, "$bounds")
        assertTrue(bounds.y >= box.y && bounds.y + bounds.height <= box.y + box.height, "$bounds")
    }

    @Test
    fun sizeInEmScalesShapes() {
        val box = Rectangle(0.0, 0.0, 400.0, 400.0)
        val small = VectorTextDrawer().apply { addText("H", box, TextStyle(face)) }.shapes.single().bounds
        val large = VectorTextDrawer().apply { addText("H", box, TextStyle(face, sizeInEm = 2.0)) }.shapes.single().bounds
        assertEquals(small.height * 2.0, large.height, 1E-6)
    }

    @Test
    fun clearRemovesShapes() {
        val drawer = VectorTextDrawer()
        drawer.addText("ab", Rectangle(0.0, 0.0, 400.0, 400.0), TextStyle(face))
        drawer.clear()
        assertTrue(drawer.shapes.isEmpty())
    }

    @Test
    fun singleLineUsesSizeAndWidthFactor() {
        fun bounds(style: TextStyle) = VectorTextDrawer().apply {
            this.style = style
            addText(face, "HH", Vector2(10.0, 100.0))
        }.shapes.map { it.bounds }.bounds

        val normal = bounds(TextStyle.Defaults)
        val scaled = bounds(TextStyle.Defaults.copy(sizeInEm = 2.0, textWidthFactor = 0.5))
        assertEquals(normal.height * 2.0, scaled.height, 1E-6)
        assertEquals(normal.x - 10.0, scaled.x - 10.0, 1E-6)
        assertEquals(normal.width, scaled.width, 1E-6)
    }
}
