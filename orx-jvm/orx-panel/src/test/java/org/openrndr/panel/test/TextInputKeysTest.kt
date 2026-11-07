package org.openrndr.panel.test

import org.openrndr.CharacterEvent
import org.openrndr.KEY_ARROW_LEFT
import org.openrndr.KEY_BACKSPACE
import org.openrndr.KEY_HOME
import org.openrndr.KeyModifier
import org.openrndr.panel.elements.TextInput
import org.openrndr.panel.elements.Textfield
import org.openrndr.panel.elements.tabs
import org.openrndr.panel.elements.textfield
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** the clipboard of the program only works on the main thread, which tests do not run on, it is not tested */
class TextInputKeysTest : PanelTestFixture() {
    @Test
    fun `keys select, replace and delete text`() {
        lateinit var field: Textfield
        val cm = panel { layout { field = textfield { label = "name" } } }
        // the value of a textfield reaches its input when the program draws, the input is set directly
        val input: TextInput = field.textInput
        input.value = "one two"
        cm.keyboardInput.target = input
        cm.key(KEY_ARROW_LEFT, KeyModifier.SHIFT, KeyModifier.CTRL)
        assertEquals("two", input.selectedText)
        input.keyboard.character.trigger(CharacterEvent('X', emptySet()))
        assertEquals("one X", input.value)
        cm.key(KEY_HOME, KeyModifier.SHIFT)
        assertEquals("one X", input.selectedText)
        cm.key(KEY_BACKSPACE)
        assertEquals("", input.value)
    }

    @Test
    fun `tabs are laid out with their labels`() {
        val cm = panel {
            layout {
                tabs {
                    tab("first") {}
                    tab("second tab") {}
                }
            }
        }
        val bar = cm.body!!.children.first().children.first()
        val (first, second) = bar.children
        // the tabs are as wide as their labels and their padding, next to each other
        assertTrue(second.layout.screenWidth > first.layout.screenWidth)
        assertEquals(first.layout.screenX + first.layout.screenWidth + 2.0, second.layout.screenX, 1E-6)
        assertTrue(first.layout.screenHeight > 12.0)
    }
}
