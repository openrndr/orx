package org.openrndr.panel.test

import org.openrndr.CharacterEvent
import org.openrndr.panel.elements.TextInput
import kotlin.test.Test
import kotlin.test.assertEquals

/** the keys and the mouse need a running application, they are not tested here */
class TextInputTest {
    private fun TextInput.type(text: String) = text.forEach { keyboard.character.trigger(CharacterEvent(it, emptySet())) }

    @Test
    fun `typing and inserting go to the caret`() {
        val input = TextInput()
        val changes = mutableListOf<String>()
        input.events.valueChanged.listen { changes.add(it.newValue) }
        input.type("ab")
        assertEquals("ab", input.value)
        input.insert("cd")
        assertEquals("abcd", input.value)
        assertEquals("", input.selectedText)
        input.type("e")
        assertEquals("abcde", input.value)
        assertEquals(listOf("a", "ab", "abcd", "abcde"), changes)
        // inserting nothing changes nothing
        input.insert("")
        assertEquals(4, changes.size)
    }
}
