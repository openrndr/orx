import org.openrndr.biditextdriver.BidiTextDriverUax9
import org.openrndr.draw.font.Face
import org.openrndr.draw.font.internal.BidiTextDriver
import org.openrndr.draw.font.internal.Direction
import org.openrndr.draw.font.internal.FontDriver
import org.openrndr.draw.font.internal.Script
import org.openrndr.draw.font.internal.ShapeFeature
import org.openrndr.draw.font.internal.ShapeResult
import org.openrndr.draw.font.internal.TextShapingDriver
import org.openrndr.draw.font.loadFace
import org.openrndr.extra.textwriter.LineBox
import org.openrndr.extra.textwriter.ShapedWord
import org.openrndr.extra.textwriter.TextSpan
import org.openrndr.extra.textwriter.TextStyle
import org.openrndr.extra.textwriter.shapeParagraph
import org.openrndr.fontdriver.freetype.FontDriverFreetype
import org.openrndr.math.Vector2
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * A shaper that records how it is called and produces one glyph per UTF-16 unit
 */
private class RecordingShaper : TextShapingDriver {
    val calls = mutableListOf<Triple<String, Direction?, Script?>>()

    override fun shape(
        face: Face, text: String, features: List<ShapeFeature>, direction: Direction?, script: Script?, language: String?
    ): List<ShapeResult> {
        calls.add(Triple(text, direction, script))
        return text.map { ShapeResult(it.code, Vector2.ZERO, Vector2(10.0, 0.0)) }
    }

    override fun querySubstitutionFeatures(face: Face, script: Script) = emptyList<String>()
    override fun queryPositionFeatures(face: Face, script: Script) = emptyList<String>()
}

class TestShapedParagraph {
    @BeforeTest
    fun setupDrivers() {
        if (FontDriver.driver == null) FontDriver.driver = FontDriverFreetype()
        if (BidiTextDriver.driver == null) BidiTextDriver.driver = BidiTextDriverUax9()
    }

    private val style by lazy { TextStyle.Defaults.copy(face = loadFace("../demo-data/fonts/IBMPlexMono-Regular.ttf").withSize(24.0)) }

    private fun visualWords(text: String, direction: Direction?): List<String> {
        val paragraph = shapeParagraph(listOf(TextSpan(text, null)), style, direction, shaper = RecordingShaper())
        return paragraph.visualOrder(0, paragraph.items.size).map { index ->
            when (val item = paragraph.items[index]) {
                is LineBox<ShapedWord<TextStyle>> -> item.content.text
                else -> " "
            }
        }
    }

    @Test
    fun rightToLeftRunInLeftToRightParagraph() {
        assertEquals(listOf("a", " ", "عالم", " ", "مرحبا", " ", "b"), visualWords("a مرحبا عالم b", Direction.LEFT_TO_RIGHT))
    }

    @Test
    fun leftToRightRunInRightToLeftParagraph() {
        assertEquals(
            listOf("عالم", " ", "first", " ", "second", " ", "مرحبا"),
            visualWords("مرحبا first second عالم", Direction.RIGHT_TO_LEFT)
        )
    }

    @Test
    fun paragraphDirectionIsDetectedWhenNull() {
        assertEquals(listOf("عالم", " ", "مرحبا"), visualWords("مرحبا عالم", null))
    }

    @Test
    fun runsAreShapedWithTheirDirection() {
        val shaper = RecordingShaper()
        shapeParagraph(listOf(TextSpan("مرحبا ٢٠٢٦ (عالم)", null)), style, Direction.RIGHT_TO_LEFT, shaper = shaper)
        val directions = shaper.calls.associate { it.first to it.second }
        assertEquals(Direction.RIGHT_TO_LEFT, directions["مرحبا"])
        // Arabic-Indic digits are an Arabic number run, shaped left-to-right
        assertEquals(Direction.LEFT_TO_RIGHT, directions["٢٠٢٦"])
        // brackets stay in the right-to-left run so the shaper mirrors them
        assertEquals(Direction.RIGHT_TO_LEFT, directions["(عالم)"])
    }

    @Test
    fun mixedWordsAreSplitIntoRuns() {
        val shaper = RecordingShaper()
        shapeParagraph(listOf(TextSpan("و٢٠٢٦", null)), style, Direction.RIGHT_TO_LEFT, shaper = shaper)
        assertEquals(listOf("و", "٢٠٢٦"), shaper.calls.map { it.first })
    }

    @Test
    fun spansShareOneAnalysis() {
        // the space between the spans sits between two right-to-left words and resolves right-to-left
        val spans = listOf(TextSpan("a مرحبا", null), TextSpan(" عالم b", null))
        val paragraph = shapeParagraph(spans, style, Direction.LEFT_TO_RIGHT, shaper = RecordingShaper())
        val words = paragraph.visualOrder(0, paragraph.items.size).map { index ->
            (paragraph.items[index] as? LineBox<ShapedWord<TextStyle>>)?.content?.text ?: " "
        }
        assertEquals(listOf("a", " ", "عالم", " ", "مرحبا", " ", "b"), words)
    }
}
