import org.openrndr.extra.parameters.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TestOptionLabels {
    enum class Mode {
        NORMAL,
        @OptionLabel("soft light (W3C)")
        SOFT_LIGHT,
        COLOR_DODGE
    }

    class Settings {
        @OptionParameter("mode", order = 0)
        var mode = Mode.NORMAL

        @DoubleParameter("amount", 0.0, 1.0, order = 1)
        var amount = 0.5
    }

    @Test
    fun `option parameters list the labels of their constants`() {
        val (mode, amount) = Settings().listParameters()
        // the label of the annotation, else the name in lowercase with spaces, in the order of the enum
        assertEquals(
            listOf("NORMAL" to "normal", "SOFT_LIGHT" to "soft light (W3C)", "COLOR_DODGE" to "color dodge"),
            mode.optionLabels!!.toList()
        )
        assertNull(amount.optionLabels)
        assertEquals("soft light", optionLabelOf("SOFT_LIGHT"))
    }
}
