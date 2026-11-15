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
import org.openrndr.panel.style.effectiveBorderColor
import org.openrndr.panel.style.effectiveColor
import org.openrndr.panel.style.left
import org.openrndr.panel.style.position
import org.openrndr.panel.style.px
import org.openrndr.panel.style.top
import org.openrndr.panel.style.zIndex
import org.openrndr.shape.Rectangle
import org.openrndr.shape.bounds
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.reflect.KMutableProperty0
import kotlin.reflect.KMutableProperty1

/**
 * a span of the value of a [TextInput] from [start] to [end], the end excluded: its characters in [color], and a
 * wavy line under them in [underline], like for an error; null leaves either as it is
 */
data class TextMark(val start: Int, val end: Int, val color: ColorRGBa? = null, val underline: ColorRGBa? = null)

/**
 * a candidate of a completion: [text] replaces the word that is completed; the list shows [label], [detail] dimmed
 * at its right, and [color] for the label
 */
data class Completion(val text: String, val label: String = text, val detail: String? = null, val color: ColorRGBa? = null)

/**
 * the candidates that can replace the characters of the value from [start] to [end], the end excluded, usually the
 * word at the caret. With [filter] the input keeps those that start with what is typed of the word, then those that
 * contain it, ignoring case
 */
class Completions(val start: Int, val end: Int, val candidates: List<Completion>, val filter: Boolean = true)

/** what can be typed at [caret] in [text], see [TextInput.completer]; null when nothing can be completed there */
fun interface Completer {
    fun complete(text: String, caret: Int): Completions?
}

/**
 * A single line of text. Enter fires [Events.submitted], [Events.keyPressed] sees the keys before the input does.
 * With a [completer] the input completes the word at the caret: typing shows a list of the candidates under it and
 * the rest of the best one as [ghost] text, tab or right at the end takes it, up and down choose in the list, enter
 * takes the chosen one once the list was used, and escape closes it. [marks] color and underline spans of the text.
 */
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

    /** the position of the caret, from 0 before the first character to the length of the value after the last */
    var caret: Int
        get() = (selectionEnd + 1).coerceIn(0, value.length)
        set(v) {
            selectionEnd = v.coerceIn(0, value.length) - 1
            selectionStart = selectionEnd
            requestRedraw()
        }

    /** the characters that are selected, an empty range at the caret when none are */
    val selection: IntRange
        get() = (min(selectionStart, selectionEnd) + 1).coerceIn(0, value.length) until (max(selectionStart, selectionEnd) + 1).coerceIn(0, value.length)

    /** selects the characters from [start] to [end], the end excluded, with the caret at [end] */
    fun select(start: Int, end: Int) {
        selectionStart = start.coerceIn(0, value.length) - 1
        selectionEnd = end.coerceIn(0, value.length) - 1
        requestRedraw()
    }

    /** replaces the characters from [start] to [end], the end excluded, with [text]; the caret goes after it */
    fun replace(start: Int, end: Int, text: String) {
        select(start, end)
        insert(text)
    }

    /** colored and underlined spans of the value, like the tokens of a command and an error in it */
    var marks: List<TextMark> = emptyList()
        set(v) { if (v != field) { field = v; requestRedraw() } }

    /** dimmed text after the caret, shown when the caret is at the end of the value, like the rest of a completion */
    var ghost: String? = null
        set(v) { if (v != field) { field = v; requestRedraw() } }

    /** the caret on the screen, from the top to the bottom of the text, at the scroll of the text when it was last drawn */
    val caretArea: Rectangle
        get() = Rectangle(screenX(caret), layout.screenY, 0.0, layout.screenHeight)

    /** the x of the position [i] of the value on the screen, at the scroll of the text when it was last drawn */
    private fun screenX(i: Int): Double {
        val carets = panelText?.carets(computedStyle, value)
        return layout.screenX + scroll + layout.contentBoundsAtOrigin.x + (carets?.getOrNull(i.coerceIn(0, value.length)) ?: 0.0)
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

        /** enter was pressed, with the value */
        val submitted = Event<ValueChangedEvent>("text-input-submitted")

        /**
         * a key was pressed while the input has the keyboard, before the input handles it; cancelling its propagation
         * keeps the input from handling it, like to use up and down for a history. The list of completions sees its
         * keys first
         */
        val keyPressed = Event<KeyEvent>("text-input-key-pressed")

        override fun close() {
            valueChanged.close()
            submitted.close()
            keyPressed.close()
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

    //<editor-fold desc="completion">
    /** completes the word at the caret, see [Completer]; null completes nothing */
    var completer: Completer? = null
        set(v) { field = v; if (v == null) closeCompletions() }

    /** the most candidates the list shows at once, it scrolls through the others */
    var maxCompletionRows = 8

    /** the completions at the caret and the candidates that fit what is typed, while they show */
    private var completions: Completions? = null
    internal var candidates: List<Completion> = emptyList()
        private set

    /** the candidate that tab and enter take */
    internal var chosen = 0
        private set

    /** true once up or down chose in the list, after which enter takes the chosen candidate */
    private var navigated = false

    private var popup: CompletionList? = null

    /** the candidates of [c] that fit what is typed of its word: those that start with it, then those that contain it */
    private fun filtered(c: Completions): List<Completion> {
        if (!c.filter) return c.candidates
        val typed = value.substring(c.start.coerceIn(0, value.length), caret.coerceAtLeast(c.start).coerceIn(0, value.length))
        if (typed.isEmpty()) return c.candidates
        val starts = c.candidates.filter { it.text.startsWith(typed, ignoreCase = true) }
        val contains = c.candidates.filter { it !in starts && it.text.contains(typed, ignoreCase = true) }
        return starts + contains
    }

    /** what is typed of the word that [completions] complete */
    private val typed: String
        get() = completions?.let { c -> value.substring(c.start.coerceIn(0, value.length), caret.coerceIn(c.start, value.length)) } ?: ""

    /**
     * asks the [completer] for the completions at the caret: after typing they show when something is typed of the
     * word, after tab also when nothing is
     */
    private fun updateCompletions(explicit: Boolean) {
        val c = completer?.complete(value, caret)
        val list = c?.let(::filtered).orEmpty()
        val typedNow = c?.let { value.substring(it.start.coerceIn(0, value.length), caret.coerceIn(it.start, value.length)) } ?: ""
        // nothing to show: no candidates, nothing typed after typing, or only what is typed already
        if (c == null || list.isEmpty() || (!explicit && typedNow.isEmpty()) || (list.size == 1 && list[0].text == typedNow)) {
            closeCompletions()
            return
        }
        val before = candidates.getOrNull(chosen)
        completions = c
        candidates = list
        chosen = before?.let { list.indexOf(it) }?.takeIf { it >= 0 } ?: 0
        if (!explicit) navigated = false
        showCompletions()
    }

    /** the rest of the chosen candidate after what is typed, as ghost text, and the list under the word */
    private fun showCompletions() {
        val c = completions ?: return
        val best = candidates.getOrNull(chosen)
        val t = typed
        ghost = if (best != null && caret == value.length && best.text.length > t.length && best.text.startsWith(t, ignoreCase = false)) best.text.substring(t.length) else null
        val body = root() as? Body ?: return
        val list = popup ?: CompletionList(this).also { popup = it; body.append(it) }
        // under the input, or over it when the list does not fit under it, within the window
        val face = panelText?.face(computedStyle)
        val height = minOf(candidates.size, maxCompletionRows) * ((face?.let { it.ascent - it.descent } ?: 14.0) + 8.0) + 2.0
        val below = layout.screenY + layout.screenHeight + 2.0
        val y = if (below + height > body.layout.screenHeight && layout.screenY - height - 2.0 >= 0.0) layout.screenY - height - 2.0 else below
        val x = (screenX(c.start) - 4.0).coerceAtMost(body.layout.screenWidth - list.layout.screenWidth - 4.0).coerceAtLeast(0.0)
        list.anchor = org.openrndr.math.Vector2(x, y)
        list.requestLayout()
        list.requestRedraw()
    }

    /** closes the list of completions and the ghost text */
    fun closeCompletions() {
        completions = null
        candidates = emptyList()
        chosen = 0
        navigated = false
        ghost = null
        popup?.let { p -> p.parent?.remove(p); p.close() }
        popup = null
    }

    /** true while the list of completions shows */
    val completing: Boolean get() = popup != null

    /** replaces the word of the completions with [candidate] */
    internal fun accept(candidate: Completion) {
        val c = completions ?: return
        val end = maxOf(c.end, caret).coerceIn(0, value.length)
        closeCompletions()
        replace(c.start.coerceIn(0, value.length), end, candidate.text)
    }

    /** the keys of completion: true when the key was used */
    private fun completionKey(e: KeyEvent): Boolean {
        if (completer == null) return false
        val open = popup != null
        return when {
            e.key == KEY_TAB && KeyModifier.SHIFT !in e.modifiers -> {
                if (open) {
                    candidates.getOrNull(chosen)?.let(::accept)
                } else {
                    updateCompletions(explicit = true)
                    when (candidates.size) {
                        0 -> {}
                        1 -> accept(candidates[0])
                        else -> {
                            // the common beginning of the candidates that start with what is typed is typed, the
                            // list shows the rest
                            val t = typed
                            val starting = candidates.filter { it.text.startsWith(t, ignoreCase = true) }
                            val prefix = starting.map { it.text }.reduceOrNull { a, b -> a.commonPrefixWith(b, ignoreCase = true) } ?: ""
                            val c = completions!!
                            if (prefix.length > t.length) {
                                replace(c.start, caret, prefix)
                                updateCompletions(explicit = true)
                            }
                        }
                    }
                }
                // tab does not move the keyboard focus out of an input that completes
                true
            }
            open && e.key == KEY_ARROW_DOWN -> { chosen = (chosen + 1).mod(candidates.size); navigated = true; showCompletions(); true }
            open && e.key == KEY_ARROW_UP -> { chosen = (chosen - 1).mod(candidates.size); navigated = true; showCompletions(); true }
            open && e.key == KEY_ESCAPE -> { closeCompletions(); true }
            open && e.key == KEY_ENTER && navigated -> { candidates.getOrNull(chosen)?.let(::accept); true }
            ghost != null && e.key == KEY_ARROW_RIGHT && caret == value.length && e.modifiers.isEmpty() -> {
                candidates.getOrNull(chosen)?.let(::accept); true
            }
            else -> false
        }
    }
    //</editor-fold>

    init {
        keyboard.pressed.listen {
            if (it.propagationCancelled) return@listen
            if (completionKey(it)) {
                it.cancelPropagation()
                requestRedraw()
                return@listen
            }
            events.keyPressed.trigger(it)
            if (it.propagationCancelled) return@listen
            if (it.key == KEY_ENTER) {
                closeCompletions()
                events.submitted.trigger(ValueChangedEvent(this, value, value))
                it.cancelPropagation()
                return@listen
            }
            if (KeyModifier.CTRL in it.modifiers || KeyModifier.SUPER in it.modifiers) {
                when (it.name) {
                    "v" -> {
                        // paste over the selection, or at the caret
                        // a single line: line breaks become spaces
                        clipboard?.contents?.let { text -> insert(text.replace(Regex("\r\n|\r|\n"), " ")) }
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
            // completions follow what is typed, and close when the caret moves
            when (it.key) {
                KEY_DELETE, KEY_BACKSPACE -> updateCompletions(explicit = false)
                KEY_HOME, KEY_END, KEY_ARROW_LEFT, KEY_ARROW_RIGHT -> closeCompletions()
            }
            requestRedraw()
            it.cancelPropagation()
        }

        keyboard.character.listen {
            it.cancelPropagation()
            // typing replaces the selection
            insert(it.character.toString())
            updateCompletions(explicit = false)
        }

        // pressing places the caret, or extends the selection with shift; dragging selects
        mouse.pressed.listen { ev ->
            closeCompletions()
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

        val textColor = (computedStyle.color as? Color.RGBa)?.color ?: ColorRGBa.WHITE
        if (value.isNotEmpty()) {
            val shaped = panel.line(computedStyle, value)
            // the colors of the marks, a glyph has the color of the character where its pen starts
            val colored = marks.filter { it.color != null }
            val glyphColors = if (colored.isEmpty()) null else {
                val byChar = MutableList(value.length) { textColor }
                for (m in colored) for (k in m.start.coerceAtLeast(0) until m.end.coerceAtMost(value.length)) byChar[k] = m.color!!
                shaped.glyphs.map { glyph ->
                    val x = xOffset + glyph.position.x - glyph.offset.x
                    val k = caretPositions.toList().binarySearch(x + 0.01).let { if (it >= 0) it else -it - 2 }
                    byChar[k.coerceIn(0, value.length - 1)]
                }
            }
            panel.draw(drawer, this, "value", shaped, org.openrndr.math.Vector2(xOffset + scroll, baseline), textColor, glyphColors = glyphColors)
        }
        // wavy lines under the marks that have them
        for (m in marks) {
            val c = m.underline ?: continue
            val a = m.start.coerceIn(0, value.length)
            // an empty span at the end, like a missing word, is a short line after the text
            val b = m.end.coerceIn(0, value.length).let { if (it == a) -1 else it }
            val x0 = caretPositions[a] + scroll
            val x1 = if (b < 0) x0 + panel.width(computedStyle, "m") else caretPositions[b] + scroll
            val y = baseline + 2.5
            drawer.isolated {
                fill = null
                stroke = c
                strokeWeight = 1.0
                val steps = maxOf(2, ((x1 - x0) / 2.0).toInt())
                lineStrip((0..steps).map { i ->
                    val x = x0 + (x1 - x0) * i / steps
                    org.openrndr.math.Vector2(x, y + if (i % 2 == 0) -1.0 else 1.0)
                })
            }
        }
        // the rest of a completion, dimmed, after the caret at the end
        ghost?.takeIf { isInputActive && caret == value.length && it.isNotEmpty() }?.let { g ->
            panel.drawLine(drawer, this, "ghost", computedStyle, g, caretPositions[value.length] + scroll, baseline, textColor.opacify(0.4))
        }

        if (isInputActive) {
            drawer.stroke = ColorRGBa.WHITE
            drawer.lineSegment(caretX + scroll, baseline - face.descent, caretX + scroll, baseline - face.descent - face.ascent)
        }
        // the line of the caret over the width of the content, with the caret in it
        val content = layout.contentBoundsAtOrigin
        caretLine = if (isInputActive) {
            Rectangle(content.x, baseline - face.ascent, content.width, face.ascent - face.descent) to (caretX + scroll - content.x)
        } else null
    }

    /** the line of the caret and the caret in it, in the coordinates of the element, as it was drawn last */
    private var caretLine: Pair<Rectangle, Double>? = null

    override val textInputArea: TextInputArea?
        get() = caretLine?.let { (line, cursor) -> TextInputArea(line.movedBy(org.openrndr.math.Vector2(layout.screenX, layout.screenY)), cursor) }

    /** true when the keyboard is to come back to the input, after a candidate of the list was clicked */
    internal var refocus = false

    override fun prepare() {
        val cm = (root() as? Body)?.controlManager
        if (refocus) {
            refocus = false
            cm?.keyboardInput?.requestFocus(this)
        } else if (popup != null && cm?.keyboardInput?.target !== this) {
            // the list closes when the input loses the keyboard
            closeCompletions()
        }
    }

    override fun close() {
        closeCompletions()
        super.close()
        events.close()
    }
}

/**
 * the list of the candidates of the completion of [input], under the word that is completed; a click takes a
 * candidate. Its colors are those of `completion-list` in the style sheet
 */
class CompletionList internal constructor(val input: TextInput) : Element(ElementType("completion-list")) {
    /** the top left of the list on the screen */
    internal var anchor = org.openrndr.math.Vector2.ZERO
        set(v) {
            field = v
            style {
                position = org.openrndr.panel.style.Position.FIXED
                left = v.x.px
                top = v.y.px
                zIndex = org.openrndr.panel.style.ZIndex.Value(1000)
            }
        }

    private val rowHeight: Double get() = (panelText?.face(input.computedStyle)?.let { it.ascent - it.descent } ?: 14.0) + 8.0
    private val rows: Int get() = minOf(input.candidates.size, input.maxCompletionRows)

    /** the first row that shows, so that the chosen candidate is in view */
    private var first = 0

    override val widthHint: Double
        get() {
            val text = panelText ?: return 200.0
            return input.candidates.maxOfOrNull { c ->
                text.width(computedStyle, c.label) + (c.detail?.let { 24.0 + text.width(computedStyle, it) } ?: 0.0)
            }?.let { kotlin.math.ceil(it) + 16.0 } ?: 0.0
        }

    override val heightHint: Double get() = rows * rowHeight

    init {
        mouse.pressed.listen { e ->
            e.cancelPropagation()
            val i = first + ((e.position.y - layout.screenY - layout.borderWidth) / rowHeight).toInt()
            input.candidates.getOrNull(i)?.let {
                input.refocus = true
                input.accept(it)
            }
        }
        mouse.scrolled.listen { e ->
            e.cancelPropagation()
            first = (first - e.rotation.y.toInt().coerceIn(-1, 1)).coerceIn(0, maxOf(0, input.candidates.size - rows))
            requestRedraw()
        }
    }

    override fun draw(drawer: Drawer) {
        val text = panelText ?: return
        val color = computedStyle.effectiveColor ?: ColorRGBa.WHITE
        drawer.fill = computedStyle.effectiveBackground ?: ColorRGBa.fromHex("#26292f")
        drawer.stroke = computedStyle.effectiveBorderColor
        drawer.strokeWeight = layout.borderWidth
        drawer.strokeAlignment = StrokeAlignment.INSIDE
        drawer.box(layout.boundsAtOrigin, cornerRadius)
        val chosen = input.chosen
        if (chosen < first) first = chosen
        if (chosen >= first + rows) first = chosen - rows + 1
        val h = rowHeight
        val w = layout.screenWidth
        val b = layout.borderWidth
        val texts = mutableListOf<org.openrndr.panel.PanelText.Placed>()
        for (r in 0 until rows) {
            val c = input.candidates.getOrNull(first + r) ?: break
            val box = Rectangle(b + 8.0, b + r * h, w - 2 * b - 16.0, h)
            if (first + r == chosen) {
                drawer.stroke = null
                drawer.fill = ColorRGBa.fromHex("#5ab0ff").opacify(0.3)
                drawer.rectangle(b, b + r * h, w - 2 * b, h)
            }
            texts += text.placeInBox(computedStyle, c.label, box, 0.0, 0.5, c.color ?: color)
            c.detail?.let { texts += text.placeInBox(computedStyle, it, box, 1.0, 0.5, color.opacify(0.5)) }
        }
        text.drawBatch(drawer, this, "rows", texts)
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