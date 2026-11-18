package org.openrndr.panel.test

import org.openrndr.MouseButton
import org.openrndr.MouseEvent
import org.openrndr.MouseEventType
import org.openrndr.math.Vector2
import org.openrndr.panel.ControlManager
import org.openrndr.panel.elements.*
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** what is dragged on the panel does not reach the program, like a camera that is dragged */
class DragPropagationTest : PanelTestFixture() {
    private fun ControlManager.dragEvent(position: Vector2) =
        MouseEvent(position, Vector2.ZERO, Vector2.ZERO, MouseEventType.DRAGGED, MouseButton.LEFT, emptySet()).also { mouseInput.drag(it) }

    @Test
    fun `drags from a press on a slider are the panel's, also before they pass the drag threshold`() {
        lateinit var s: Slider
        val cm = panel {
            css("slider { width: 300px }")
            layout { s = slider { label = "x" } }
        }
        repeat(2) { cm.afterDraw(cm.program.drawer, cm.program) }
        val p = Vector2(s.layout.screenX + s.layout.screenWidth / 2.0, s.layout.screenY + s.layout.screenHeight / 2.0)
        cm.move(p)
        cm.press(p)
        assertTrue(cm.dragEvent(p + Vector2(1.0, 0.0)).propagationCancelled, "within the threshold")
        assertTrue(cm.dragEvent(p + Vector2(40.0, 0.0)).propagationCancelled, "past the threshold")
        cm.release(p + Vector2(40.0, 0.0))

        // drags from a press outside the panel are the program's
        val outside = Vector2(600.0, 400.0)
        cm.move(outside)
        cm.press(outside)
        assertFalse(cm.dragEvent(outside + Vector2(1.0, 0.0)).propagationCancelled)
        assertFalse(cm.dragEvent(outside + Vector2(40.0, 0.0)).propagationCancelled)
        cm.release(outside + Vector2(40.0, 0.0))
    }
}
