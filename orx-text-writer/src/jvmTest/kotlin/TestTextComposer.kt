import org.openrndr.color.ColorRGBa
import org.openrndr.draw.font.internal.ShapeFeature
import org.openrndr.extra.textwriter.StrokeMode
import org.openrndr.extra.textwriter.TextStyle
import org.openrndr.extra.textwriter.composeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TestTextComposer {
    @Test
    fun spansCaptureCurrentStyle() {
        val spans = composeText {
            fill = ColorRGBa.RED
            text("a")
            sizeInEm = 2.0
            text("b")
            line("c")
        }
        assertEquals(listOf("a", "b", "c\n"), spans.map { it.text })
        assertEquals(TextStyle(fill = ColorRGBa.RED), spans[0].style)
        assertEquals(TextStyle(fill = ColorRGBa.RED, sizeInEm = 2.0), spans[1].style)
    }

    @Test
    fun scopesRestoreStyle() {
        val spans = composeText(TextStyle(strokeMode = StrokeMode.INNER)) {
            styled {
                fill = ColorRGBa.BLUE
                text("a")
            }
            span("b", TextStyle(baselineShiftInEm = -0.25)) { sizeInEm = 0.5 }
            text("c")
        }
        assertEquals(TextStyle(strokeMode = StrokeMode.INNER, fill = ColorRGBa.BLUE), spans[0].style)
        assertEquals(TextStyle(strokeMode = StrokeMode.INNER, baselineShiftInEm = -0.25, sizeInEm = 0.5), spans[1].style)
        assertEquals(TextStyle(strokeMode = StrokeMode.INNER), spans[2].style)
        assertNull(spans[2].style?.fill)
    }

    @Test
    fun featuresAccumulate() {
        val spans = composeText {
            feature(ShapeFeature.ENABLE_SMALL_CAPS)
            styled {
                feature("tnum")
                text("a")
            }
            text("b")
        }
        assertEquals(listOf(ShapeFeature.ENABLE_SMALL_CAPS, ShapeFeature("tnum", 1)), spans[0].style?.features)
        assertEquals(listOf(ShapeFeature.ENABLE_SMALL_CAPS), spans[1].style?.features)
    }
}
