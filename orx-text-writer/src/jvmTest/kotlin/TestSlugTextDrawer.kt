import org.openrndr.draw.font.loadFace
import org.openrndr.extra.testing.AbstractApplicationTestFixture
import org.openrndr.extra.textwriter.SlugTextDrawer
import org.openrndr.extra.textwriter.TextStyle
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestSlugTextDrawer : AbstractApplicationTestFixture() {
    private fun face() = loadFace("../demo-data/fonts/IBMPlexMono-Regular.ttf").withSize(16.0)

    @Test
    fun basicDraw() {
        val slugTextDrawer = SlugTextDrawer()
        val face = face()
        slugTextDrawer.addText(face, "Hello world", Vector2.ZERO)
        slugTextDrawer.draw(program.drawer)
        face.close()
    }

    @Test
    fun textWidthFactorScalesHorizontally() {
        val slugTextDrawer = SlugTextDrawer()
        slugTextDrawer.style = TextStyle.Defaults.copy(face = face(), sizeInEm = 2.0, textWidthFactor = 0.5)
        slugTextDrawer.addText("hello", Rectangle(0.0, 0.0, 400.0, 400.0))
        val transform = slugTextDrawer.commands.first().transform
        assertEquals(1.0, transform.c0r0, 1E-9)
        assertEquals(2.0, transform.c1r1, 1E-9)
    }

    @Test
    fun verticalCenteringSingleLine() {
        val slugTextDrawer = SlugTextDrawer()
        val face = face()
        val box = Rectangle(0.0, 0.0, 400.0, 400.0)
        slugTextDrawer.addText("Hello", box, TextStyle(face, verticalAlign = 0.5))

        // the text block (ascent to descent, descent is negative) is centered in the box
        val textBlockHeight = face.ascent - face.descent
        val expectedBaseline = (box.height - textBlockHeight) / 2.0 + face.ascent
        for (cmd in slugTextDrawer.commands) {
            assertEquals(expectedBaseline, cmd.transform.c3r1, 0.01)
        }
        face.close()
    }

    @Test
    fun verticalCenteringMultiLine() {
        val slugTextDrawer = SlugTextDrawer()
        val face = face()
        val box = Rectangle(0.0, 0.0, 200.0, 400.0)
        val text = "Lorem ipsum dolor sit amet consectetur adipiscing elit sed do eiusmod tempor"
        slugTextDrawer.addText(text, box, TextStyle(face, verticalAlign = 0.5, lineHeightInEm = 1.5))

        val baselineYs = slugTextDrawer.commands.map { it.transform.c3r1 }.distinct().sorted()
        assertTrue(baselineYs.size > 1)
        val textBlockTop = baselineYs.first() - face.ascent
        val textBlockBottom = baselineYs.last() - face.descent
        assertEquals(box.center.y, (textBlockTop + textBlockBottom) / 2.0, 0.01)
        face.close()
    }

    @Test
    fun boxTextDoesNotExceedLineWidth() {
        val slugTextDrawer = SlugTextDrawer()
        val face = face()
        val box = Rectangle(10.0, 360.0, 350.0, 720.0)
        val text = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore " +
                "et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut " +
                "aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse " +
                "cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in " +
                "culpa qui officia deserunt mollit anim id est laborum"
        slugTextDrawer.addText(text, box, TextStyle(face))

        val maxX = box.x + box.width
        for (cmd in slugTextDrawer.commands) {
            assertTrue(cmd.transform.c3r0 <= maxX + 0.01, "Glyph at x=${cmd.transform.c3r0} exceeds box right edge at $maxX")
        }
        face.close()
    }
}
