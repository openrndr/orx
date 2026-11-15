package org.openrndr.panel.test

import org.openrndr.CharacterEvent
import org.openrndr.KEY_ENTER
import org.openrndr.panel.ControlManager
import org.openrndr.panel.elements.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** the text fields tell the window where their caret is, for the candidates of input methods */
class TextInputAreaTest : PanelTestFixture() {
    private fun ControlManager.frames(n: Int = 2) = repeat(n) { afterDraw(program.drawer, program) }
    private fun ControlManager.type(text: String) = text.forEach { keyboardInput.character(CharacterEvent(it, emptySet())) }

    @Test
    fun `a focused text input gives the window the line of its caret, none once it loses the focus`() {
        lateinit var input: TextInput
        lateinit var other: Button
        val cm = panel {
            css("text-input { width: 300px; height: 32px } button { width: 80px; height: 32px }")
            layout {
                input = textInput { }
                other = button { label = "other" }
            }
        }
        cm.frames()
        assertNull(program.window.textInputArea, "nothing has the focus")
        cm.keyboardInput.requestFocus(input)
        cm.frames()
        val empty = assertNotNull(program.window.textInputArea)
        // the line is within the input, on the screen
        val bounds = org.openrndr.shape.Rectangle(input.layout.screenX, input.layout.screenY, input.layout.screenWidth, input.layout.screenHeight)
        assertTrue(empty.area.x >= bounds.x - 1E-6 && empty.area.x + empty.area.width <= bounds.x + bounds.width + 1E-6, "${empty.area} in $bounds")
        assertTrue(empty.area.y >= bounds.y - 1E-6 && empty.area.y + empty.area.height <= bounds.y + bounds.height + 1E-6, "${empty.area} in $bounds")
        assertTrue(empty.area.height > 4.0)

        // the caret moves to the right while typing, the line stays
        cm.type("hello")
        cm.frames()
        val typed = assertNotNull(program.window.textInputArea)
        assertTrue(typed.cursor > empty.cursor + 10.0, "${typed.cursor} after ${empty.cursor}")
        assertEquals(empty.area, typed.area)

        cm.keyboardInput.requestFocus(other)
        cm.frames()
        assertNull(program.window.textInputArea, "a button takes no text")
    }

    @Test
    fun `a text area gives the line of its caret, lower on later lines`() {
        lateinit var area: TextArea
        val cm = panel {
            css("text-area { width: 300px; height: 200px }")
            layout { area = textArea { } }
        }
        cm.frames()
        cm.keyboardInput.requestFocus(area)
        cm.frames()
        val first = assertNotNull(program.window.textInputArea, "focused")
        cm.type("one")
        cm.key(KEY_ENTER)
        cm.type("two")
        cm.frames()
        val second = assertNotNull(program.window.textInputArea, "after typing \"${area.value}\"")
        assertTrue(second.area.y > first.area.y + 4.0, "${second.area} below ${first.area}")
        assertTrue(second.cursor > 0.0)
        // focused from code, the area draws its caret and gives its line; without the focus it gives none
        cm.keyboardInput.target = null
        cm.frames()
        assertNull(program.window.textInputArea)
    }
}
