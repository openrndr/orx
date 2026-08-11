package org.openrndr.panel.elements

import org.openrndr.*
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.isolated
import org.openrndr.events.Event
import org.openrndr.extra.textwriter.Cursor
import org.openrndr.extra.textwriter.writer
import org.openrndr.panel.binding.Binding0
import org.openrndr.panel.binding.Binding1
import org.openrndr.panel.style.Color
import org.openrndr.panel.style.borderColor
import org.openrndr.panel.style.color
import org.openrndr.panel.style.effectiveBackground
import org.openrndr.shape.Rectangle
import org.openrndr.shape.bounds
import kotlin.math.max
import kotlin.math.min
import kotlin.reflect.KMutableProperty0
import kotlin.reflect.KMutableProperty1

class TextInput : Element(ElementType("text-input")) {

    private var glyphRectangles: MutableList<Pair<Rectangle, Rectangle>> = mutableListOf()
    private var ivalue: String = ""

    var value: String
        set(value) {
            if (ivalue != value) {
                ivalue = value
                selectionEnd = value.lastIndex
                selectionStart = selectionEnd
                requestRedraw()
            }
        }
        get() = ivalue


    private var selectionStart = value.lastIndex
    private var selectionEnd = value.lastIndex

    class ValueChangedEvent(val source: TextInput, val oldValue: String, val newValue: String)
    class Events : AutoCloseable {
        val valueChanged = Event<ValueChangedEvent>("text-input-value-changed")
        override fun close() {
            valueChanged.close()
        }

    }

    val events = Events()

    // Used for jumping to the previous or next word in the text input
    private fun nextCharTypeDiffers(direction: Int): Boolean {
        val a = selectionEnd + if (direction < 0) 1 else 0
        val b = a + direction
        return a !in 0..value.lastIndex ||
                b !in 0..value.lastIndex ||
                value[a].isLetterOrDigit() != value[b].isLetterOrDigit()
    }

    // Updates inputIndex. direction 1 is right, -1 is left
    private fun moveSelectionEnd(direction: Int, useWordBoundary: Boolean = false) {
        do {
            selectionEnd = (selectionEnd + direction).coerceIn(-1, value.lastIndex)
        } while (useWordBoundary && !nextCharTypeDiffers(direction))
    }

    private fun String.dropRange(fromIndex: Int, toIndex: Int) = this.filterIndexed { i, _ ->
        i !in fromIndex..toIndex
    }

    private fun deleteChars(direction: Int) {
        if (value.isNotEmpty()) {
            val oldValue = value

            if (selectionStart == selectionEnd) {
                selectionStart = (selectionEnd + direction).coerceIn(-1, value.lastIndex)
            }

            if (selectionStart > selectionEnd) {
                selectionStart = selectionEnd.also { selectionEnd = selectionStart }
            }

            val a = (min(selectionStart, selectionEnd) + 1).coerceIn(0, value.lastIndex)
            val b = (max(selectionStart, selectionEnd)).coerceIn(0, value.lastIndex)

            ivalue = value.dropRange(a, b)
            selectionEnd = selectionStart
            events.valueChanged.trigger(ValueChangedEvent(this, oldValue, value))
        }
    }

    init {
        keyboard.pressed.listen {
            if (KeyModifier.CTRL in it.modifiers || KeyModifier.SUPER in it.modifiers) {
                when (it.name) {
                    "v" -> {
                        // Paste
                        val oldValue = value
                        (root() as Body).controlManager?.program?.clipboard?.contents?.let {
                            ivalue += it
                        }
                        events.valueChanged.trigger(ValueChangedEvent(this, oldValue, value))
                        it.cancelPropagation()
                    }

                    "a" -> {
                        // Select all
                        selectionStart = -1
                        selectionEnd = value.lastIndex
                    }
                }
            }

            when (it.key) {
                KEY_HOME -> {
                    selectionEnd = -1
                    if (KeyModifier.SHIFT !in it.modifiers) selectionStart = selectionEnd
                }

                KEY_END -> {
                    selectionEnd = value.length - 1
                    if (KeyModifier.SHIFT !in it.modifiers) selectionStart = selectionEnd
                }

                KEY_ARROW_LEFT -> {
                    moveSelectionEnd(
                        -1, KeyModifier.CTRL in it.modifiers || KeyModifier.SUPER in it.modifiers
                    )
                    if (KeyModifier.SHIFT !in it.modifiers) selectionStart = selectionEnd
                }

                KEY_ARROW_RIGHT -> {
                    moveSelectionEnd(
                        1, KeyModifier.CTRL in it.modifiers || KeyModifier.SUPER in it.modifiers
                    )
                    if (KeyModifier.SHIFT !in it.modifiers) selectionStart = selectionEnd
                }

                KEY_DELETE -> deleteChars(1)
                KEY_BACKSPACE -> deleteChars(-1)
            }
            requestRedraw()
            it.cancelPropagation()
        }

        keyboard.character.listen {
            it.cancelPropagation()
            val oldValue = value
            if (selectionStart != selectionEnd) deleteChars(0)
            ivalue = value.take(selectionEnd + 1) + it.character.toString() + value.drop(selectionEnd + 1)
            selectionEnd++
            selectionStart = selectionEnd
            events.valueChanged.trigger(ValueChangedEvent(this, oldValue, value))
            requestRedraw()
        }

        mouse.pressed.listen {
            it.cancelPropagation()
        }
        mouse.clicked.listen {
            it.cancelPropagation()
        }
    }

    /*
    Possible improvements:
    - Add "dirty" flag: Recalculate scroll, caretX, selectionRect only when the selection changes.
    - Improve `scroll` calculation: only scroll if the caret is getting out of the visible area.
    - Recalculate `glyphRectangles` only when `value` changes.
    - Mouse click sets `selectionStart` and `selectionEnd`, double click selects the word under the mouse cursor,
      mouse drag selects characters.
    */
    override fun draw(drawer: Drawer) {
        drawer.fill = computedStyle.effectiveBackground
        drawer.stroke = ((computedStyle.borderColor as? Color.RGBa)?.color ?: ColorRGBa.TRANSPARENT)
        drawer.rectangle(layout.boundsAtOrigin)

        (root() as? Body)?.controlManager?.fontManager?.let {
            val font = it.font(computedStyle)
            val textHeight = font.ascenderLength
            val yOffset = ((layout.screenHeight / 2) + textHeight / 2.0 - 2.0).round(0)
            val xOffset = layout.contentBoundsAtOrigin.x
            val baseY = yOffset - font.descenderLength
            var caretX: Double? = null
            val isInputActive = ElementPseudoClass("active") in pseudoClasses

            //drawer.rectangle(layout.contentBoundsAtOrigin)
            drawer.drawStyle.clip = layout.contentBounds
            drawer.fontMap = font
            drawer.fill = ((computedStyle.color as? Color.RGBa)?.color ?: ColorRGBa.WHITE)

            writer(drawer) {
                cursor = Cursor(xOffset, yOffset)
                text(value, visible = false)
                glyphRectangles = glyphOutput.rectangles

                var scroll = 2.0
                if (isInputActive) {
                    caretX = if (glyphRectangles.isNotEmpty()) {
                        if (selectionEnd == glyphRectangles.lastIndex)
                            glyphRectangles.last().second.position(1.0, 0.0).x
                        else
                            glyphRectangles[selectionEnd + 1].second.position(0.0, 0.0).x
                    } else xOffset
                    // Calculate scroll value when the caret is outside the text box
                    val layoutMaxX = layout.contentBounds.position(1.0, 0.0).x
                    val rightPadding = textWidth("m") * 2
                    scroll = (layoutMaxX - rightPadding - caretX).coerceAtMost(2.0)
                    caretX += scroll
                }

                if (selectionStart != selectionEnd) {
                    val a = min(selectionStart, selectionEnd) + 1
                    val b = max(selectionStart, selectionEnd) + 1
                    val selectionBounds = glyphRectangles.subList(a, b).map { it.second }.bounds
                    val selectionRect = Rectangle(selectionBounds.x + scroll, baseY, selectionBounds.width, -textHeight)
                    drawer.isolated {
                        stroke = null
                        fill = ColorRGBa.BLACK
                        rectangle(selectionRect)
                    }
                }

                cursor = Cursor(xOffset + scroll, yOffset)
                text(value)
            }

            caretX?.let { x ->
                drawer.stroke = ColorRGBa.WHITE
                drawer.lineSegment(x, baseY, x, baseY - textHeight)
            }

            drawer.drawStyle.clip = null
        }
    }

    override fun close() {
        super.close()
        events.close()
    }
}

fun TextInput.bind(property: KMutableProperty0<String>, program: Program? = null) {
    val program = program ?: (root() as? Body)?.controlManager?.program
    Binding0(
        program ?: error("no program"),
        this,
        this.events.valueChanged,
        property,
        { it.newValue },
        { value = it })
}

fun TextInput.bind(container: Any, property: KMutableProperty1<Any, String>, program: Program? = null) {
    val program = program ?: (root() as? Body)?.controlManager?.program
    Binding1(
        program ?: error("no program"),
        this,
        this.events.valueChanged,
        container,
        property,
        { it.newValue },
        { value = it })
}