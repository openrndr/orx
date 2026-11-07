package org.openrndr.panel.test

import org.openrndr.math.Vector2
import org.openrndr.panel.elements.*
import kotlin.test.Test
import kotlin.test.assertEquals

class TransformedInputTest : PanelTestFixture() {
    @Test
    fun `controls in transformed elements get the positions under the pointer`() {
        lateinit var slider: Slider
        val cm = panel {
            css("""
                .scaled { width: 200px; height: 60px; transform: scale(2); transform-origin: left top }
                .scaled slider { width: 200px; margin: 0; padding: 0 }
            """)
            layout {
                div("scaled") { slider = slider { range = Range(0.0, 1.0); precision = 3 } }
            }
        }
        repeat(2) { cm.afterDraw(program.drawer, program) }
        // the slider is drawn twice as large: a quarter of its drawn width is a quarter of its range
        val area = slider.screenArea
        val drawnQuarter = Vector2(area.x * 2.0 + area.width * 2.0 * 0.25, area.y * 2.0 + area.height)
        cm.move(drawnQuarter)
        cm.press(drawnQuarter)
        cm.release(drawnQuarter)
        assertEquals(0.25, slider.value, 0.06)
    }
}
