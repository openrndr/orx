package org.openrndr.panel.test

import org.openrndr.CharacterEvent
import org.openrndr.KEY_ARROW_DOWN
import org.openrndr.KEY_ARROW_RIGHT
import org.openrndr.KEY_ARROW_UP
import org.openrndr.KEY_ENTER
import org.openrndr.KEY_ESCAPE
import org.openrndr.KEY_TAB
import org.openrndr.color.ColorRGBa
import org.openrndr.color.Linearity
import org.openrndr.panel.elements.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ControlsKeysTest : PanelTestFixture() {
    @Test
    fun `tab reaches every control and leaves the xy pad`() {
        lateinit var pad: XYPad
        lateinit var picker: ColorpickerButton
        lateinit var dropdown: DropdownButton
        lateinit var sequence: SequenceEditor
        val cm = panel {
            layout {
                pad = xyPad { }
                picker = colorpickerButton { }
                dropdown = dropdownButton { item { label = "one" } }
                sequence = sequenceEditor { }
            }
        }
        cm.key(KEY_TAB)
        assertSame(pad, cm.keyboardInput.target)
        cm.key(KEY_TAB)
        assertSame(picker, cm.keyboardInput.target)
        cm.key(KEY_TAB)
        assertSame(dropdown, cm.keyboardInput.target)
        cm.key(KEY_TAB)
        assertSame(sequence, cm.keyboardInput.target)
    }

    @Test
    fun `dropdowns change by keys and by typing`() {
        lateinit var dropdown: DropdownButton
        val cm = panel {
            layout {
                dropdown = dropdownButton {
                    for (name in listOf("apple", "banana", "cherry", "chestnut")) item { label = name }
                }
            }
        }
        cm.keyboardInput.target = dropdown
        // closed, the arrows change the value
        cm.key(KEY_ARROW_DOWN)
        assertEquals("apple", dropdown.value?.label)
        cm.key(KEY_ARROW_DOWN)
        assertEquals("banana", dropdown.value?.label)
        // open, the list takes the keys, typing jumps
        cm.key(KEY_ENTER)
        relayout(cm)
        assertTrue(dropdown.isOpen)
        val list = cm.keyboardInput.target!!
        "che".forEach { list.keyboard.character.trigger(CharacterEvent(it, emptySet())) }
        assertEquals("cherry", dropdown.value?.label)
        "s".forEach { list.keyboard.character.trigger(CharacterEvent(it, emptySet())) }
        assertEquals("chestnut", dropdown.value?.label)
        cm.key(KEY_ARROW_UP)
        assertEquals("cherry", dropdown.value?.label)
        cm.key(KEY_ESCAPE)
        assertFalse(dropdown.isOpen)
        assertSame(dropdown, cm.keyboardInput.target)
    }

    @Test
    fun `color pickers open by keys, keep the alpha and remember colors`() {
        lateinit var button: ColorpickerButton
        val cm = panel { layout { button = colorpickerButton { color = ColorRGBa(1.0, 0.0, 0.0, 0.5, Linearity.SRGB) } } }
        cm.keyboardInput.target = button
        cm.key(KEY_ENTER)
        relayout(cm)
        assertTrue(button.isOpen)
        assertTrue(cm.keyboardInput.target is Colorpicker)
        cm.key(KEY_ARROW_RIGHT)
        // the hue changed, the alpha is kept
        assertTrue(button.color.g > 0.0)
        assertEquals(0.5, button.color.alpha, 1E-9)
        cm.key(KEY_ESCAPE)
        assertFalse(button.isOpen)
        assertSame(button, cm.keyboardInput.target)
        assertEquals(button.color, ColorpickerButton.recentColors.first())
        assertEquals("#ff0000", ColorRGBa(1.0, 0.0, 0.0, 1.0, Linearity.SRGB).toHexString())
    }

    @Test
    fun `sequence editors select and change values by keys`() {
        lateinit var sequence: SequenceEditor
        val cm = panel { layout { sequence = sequenceEditor { value = mutableListOf(0.0, 0.0, 0.0); range = -1.0..1.0 } } }
        cm.keyboardInput.target = sequence
        cm.key(KEY_ARROW_RIGHT)
        cm.key(KEY_ARROW_RIGHT)
        cm.key(KEY_ARROW_UP)
        assertEquals(listOf(0.0, 0.02, 0.0), sequence.value.map { Math.round(it * 1000) / 1000.0 })
    }
}
