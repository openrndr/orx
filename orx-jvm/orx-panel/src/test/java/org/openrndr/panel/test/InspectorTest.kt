package org.openrndr.panel.test

import org.openrndr.math.Vector2
import org.openrndr.panel.elements.*
import org.openrndr.panel.style.width
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class InspectorTest : PanelTestFixture() {
    @Test
    fun `the inspector shows the path, the rules in the order of the cascade and the computed style`() {
        lateinit var button: Button
        val cm = panel {
            css("""
                .bar > button { width: 60px; background-color: #ff0000 }
                button.go { width: 80px }
                .bar button { width: 10px !important }
            """)
            layout {
                div("bar") { button = button { label = "go"; classes.add(ElementClass("go")) } }
            }
        }
        relayout(cm)
        cm.move(button.screenArea.center)
        val inspector = cm.inspector
        assertSame(button, inspector.inspected)
        val lines = inspector.lines(button, cm.document?.layouter).map { it.text to it.strike }
        val texts = lines.map { it.first }
        assertTrue(texts[0] == "body:hover > div.bar:hover > button.go:hover", texts[0])
        // the rules, the last in the cascade first: of the same specificity, the last in the style sheet; the important
        // declaration wins over the others
        val rules = texts.indexOf("rules")
        assertEquals(".bar button  (0,1,1)", texts[rules + 1])
        assertTrue(("width: 80px" to true) in lines)
        assertTrue(("width: 60px" to true) in lines)
        assertTrue(("width: 10px !important" to false) in lines)
        assertTrue(("background-color: #ff0000" to false) in lines)
        // the computed width
        val computed = texts.indexOf("computed")
        assertTrue("width: 10px" in texts.drop(computed))
        // pinned, the inspected element stays when the pointer moves away
        inspector.pinned = button
        cm.move(Vector2(-10.0, -10.0))
        assertSame(button, inspector.inspected)
        assertEquals("10px", inspector.cssText(button.computedStyle.width))
    }
}
