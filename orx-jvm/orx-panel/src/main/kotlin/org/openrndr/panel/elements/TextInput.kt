package org.openrndr.panel.elements

import org.openrndr.panel.intersection

import org.openrndr.*
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.StrokeAlignment
import org.openrndr.draw.isolated
import org.openrndr.events.Event
import org.openrndr.panel.binding.Binding0
import org.openrndr.panel.binding.Binding1
import org.openrndr.panel.style.Color
import org.openrndr.panel.style.borderColor
import org.openrndr.panel.style.color
import org.openrndr.panel.style.effectiveBackground
import org.openrndr.shape.Rectangle
import org.openrndr.shape.bounds
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.reflect.KMutableProperty0
import kotlin.reflect.KMutableProperty1

class TextInput : Element(ElementType("text-input")) {

    /** the x of the caret positions as the text was last drawn, before it is scrolled */
    private var caretPositions = DoubleArray(0)
    private var ivalue: String = ""
    private var scroll = 0.0

    override val handlesDoubleClick = true

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

    /** the text that is selected, empty when nothing is */
    val selectedText: String
        get() {
            val a = (min(selectionStart, selectionEnd) + 1).coerceIn(0, value.length)
            val b = (max(selectionStart, selectionEnd) + 1).coerceIn(0, value.length)
            return value.substring(a, b)
        }

    /** replaces the selection with [text], or inserts it at the caret when nothing is selected; the caret moves after it */
    fun insert(text: String) {
        val oldValue = value
        val a = (min(selectionStart, selectionEnd) + 1).coerceIn(0, value.length)
        val b = (max(selectionStart, selectionEnd) + 1).coerceIn(0, value.length)
        ivalue = value.substring(0, a) + text + value.substring(b)
        selectionEnd = a + text.length - 1
        selectionStart = selectionEnd
        if (value != oldValue) events.valueChanged.trigger(ValueChangedEvent(this, oldValue, value))
        requestRedraw()
    }

    /** the clipboard of the program */
    private val clipboard get() = (root() as? Body)?.controlManager?.program?.clipboard

    /** the caret position nearest to [x] on the screen: the index of the character before it, -1 before the first */
    private fun caretAt(x: Double): Int? {
        if (caretPositions.isEmpty()) return null
        val boundaries = caretPositions.toList()
        // the glyphs are drawn moved by the horizontal scroll of the text
        return (boundaries.withIndex().minBy { abs(x - layout.screenX - scroll - it.value) }.index - 1).coerceAtLeast(-1)
    }

    class ValueChangedEvent(val source: TextInput, val oldValue: String, val newValue: String)
    class Events : AutoCloseable {
        val valueChanged = Event<ValueChangedEvent>("text-input-value-changed")
        override fun close() {
            valueChanged.close()
        }

    }

    val events = Events()

    /**
     * Compares [selectionEnd] with the next character towards [direction] using `isLetterOrDigit()`
     *
     * @param direction +1 for right, -1 for left
     * @return `true` if the next character is of a different type
     */
    private fun nextCharTypeDiffers(pos: Int, direction: Int): Boolean {
        val a = pos + if (direction < 0) 1 else 0
        val b = a + direction
        return a !in 0..value.lastIndex ||
                b !in 0..value.lastIndex ||
                value[a].isLetterOrDigit() != value[b].isLetterOrDigit()
    }

    /**
     * Returns true if the [pos] + [direction] character is a letter or digit
     */
    private fun nextCharIsLetterOrDigit(pos: Int, direction: Int): Boolean {
        val a = pos + direction + if (direction < 0) 1 else 0
        return a in 0..value.lastIndex && value[a].isLetterOrDigit()
    }

    /**
     * Move [selectionEnd] towards [direction]
     *
     * @param direction +1 for right, -1 for left
     * @param useWordBoundary if true it keeps advancing towards `direction` until the next character is of a different type
     */
    private fun moveSelectionEnd(direction: Int, useWordBoundary: Boolean = false) {
        do {
            selectionEnd = (selectionEnd + direction).coerceIn(-1, value.lastIndex)
        } while (useWordBoundary && !nextCharTypeDiffers(selectionEnd, direction))
    }

    /**
     * Removes a range of characters on the receiver [String]
     *
     * @param fromIndex first character to remove
     * @param toIndex last character to remove
     */
    private fun String.dropRange(fromIndex: Int, toIndex: Int) = this.filterIndexed { i, _ ->
        i !in fromIndex..toIndex
    }

    /**
     * Deletes characters from [ivalue] based on [selectionStart], [selectionEnd] and [direction]
     *
     * @param direction Use +1 when reacting to the KEY_DELETE, -1 with KEY_BACKSPACE and 0 when typing
     * over an existing selection.
     */
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
                        // paste over the selection, or at the caret
                        clipboard?.contents?.let { text -> insert(text) }
                        it.cancelPropagation()
                    }

                    "c" -> {
                        // copy the selection
                        if (selectedText.isNotEmpty()) clipboard?.contents = selectedText
                        it.cancelPropagation()
                    }

                    "x" -> {
                        // cut the selection
                        if (selectedText.isNotEmpty()) {
                            clipboard?.contents = selectedText
                            insert("")
                        }
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
            // typing replaces the selection
            insert(it.character.toString())
        }

        // pressing places the caret, or extends the selection with shift; dragging selects
        mouse.pressed.listen { ev ->
            caretAt(ev.position.x)?.let { position ->
                selectionEnd = position
                if (KeyModifier.SHIFT !in ev.modifiers) selectionStart = position
                requestRedraw()
            }
            ev.cancelPropagation()
        }

        mouse.dragged.listen { ev ->
            caretAt(ev.position.x)?.let { position ->
                selectionEnd = position
                requestRedraw()
            }
            ev.cancelPropagation()
        }

        mouse.clicked.listen { ev ->
            ev.cancelPropagation()
        }

        mouse.doubleClicked.listen { ev ->
            do {
                selectionStart = (selectionStart - 1).coerceAtLeast(-1)
            } while (nextCharIsLetterOrDigit(selectionStart, -1))
            do {
                selectionEnd = (selectionEnd + 1).coerceAtMost(value.lastIndex)
            } while (nextCharIsLetterOrDigit(selectionEnd, 1))
            requestRedraw()
            ev.cancelPropagation()
        }
    }

    override fun draw(drawer: Drawer) {
        drawer.fill = computedStyle.effectiveBackground
        drawer.stroke = ((computedStyle.borderColor as? Color.RGBa)?.color ?: ColorRGBa.TRANSPARENT)
        drawer.strokeAlignment = StrokeAlignment.INSIDE
        drawer.box(layout.boundsAtOrigin, cornerRadius)

        val panel = panelText ?: return
        val face = panel.face(computedStyle)
        val baseline = ((layout.screenHeight / 2) + face.ascent / 2.0 - 2.0).round(0)
        val xOffset = layout.contentBoundsAtOrigin.x
        val isInputActive = ElementPseudoClass("active") in pseudoClasses

        // within the clipping of the elements around it
        drawer.drawStyle.clip = drawer.drawStyle.clip?.let { intersection(it, layout.contentBounds) ?: Rectangle(0.0, 0.0, 0.0, 0.0) }
            ?: layout.contentBounds

        // the carets of the shaped text, they place the caret, the selection and clicks
        caretPositions = panel.carets(computedStyle, value).let { carets -> DoubleArray(carets.size) { xOffset + carets[it] } }
        val caretX = caretPositions[(selectionEnd + 1).coerceIn(0, value.length)]

        if (isInputActive) {
            if (value.isEmpty()) {
                scroll = 2.0
            } else {
                // the text scrolls to keep the caret in view
                val padding = panel.width(computedStyle, "m")
                if (caretX + scroll > layout.boundsAtOrigin.width - padding) {
                    scroll = layout.boundsAtOrigin.width - padding - caretX
                }
                if (caretX + scroll < layout.boundsAtOrigin.x + padding) {
                    scroll = (layout.boundsAtOrigin.x + padding + caretX).coerceAtMost(2.0)
                }
            }
            if (selectionStart != selectionEnd) {
                val a = (min(selectionStart, selectionEnd) + 1).coerceIn(0, value.length)
                val b = (max(selectionStart, selectionEnd) + 1).coerceIn(0, value.length)
                drawer.isolated {
                    stroke = null
                    fill = computedStyle.effectiveBackground?.let {
                        if (it.alpha > 0.0) ColorRGBa(1.0 - it.r, 1.0 - it.g, 1.0 - it.b, it.alpha) else ColorRGBa.BLACK
                    } ?: ColorRGBa.BLACK
                    rectangle(caretPositions[a] + scroll, baseline - face.ascent, caretPositions[b] - caretPositions[a], face.ascent - face.descent)
                }
            }
        }

        panel.drawLine(drawer, this, "value", computedStyle, value, xOffset + scroll, baseline,
            (computedStyle.color as? Color.RGBa)?.color ?: ColorRGBa.WHITE)

        if (isInputActive) {
            drawer.stroke = ColorRGBa.WHITE
            drawer.lineSegment(caretX + scroll, baseline - face.descent, caretX + scroll, baseline - face.descent - face.ascent)
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