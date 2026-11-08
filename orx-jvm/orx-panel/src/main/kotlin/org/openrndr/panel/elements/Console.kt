package org.openrndr.panel.elements

import org.openrndr.KEY_ARROW_DOWN
import org.openrndr.KEY_ARROW_UP
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.isolated
import org.openrndr.events.Event
import org.openrndr.math.Vector2
import org.openrndr.panel.PanelText
import org.openrndr.panel.intersection
import org.openrndr.panel.style.effectiveColor
import org.openrndr.shape.Rectangle

/**
 * a line of a [ConsoleLog]: [text] in [color], after [prompt] dimmed, with [marks] that color and underline spans of
 * [text], [indent] pixels from the left
 */
data class ConsoleLine(
    val text: String,
    val color: ColorRGBa? = null,
    val marks: List<TextMark> = emptyList(),
    val prompt: String? = null,
    val indent: Double = 0.0
)

/** a place in a [ConsoleLog]: before the character [char] of the line [line] */
data class LogPosition(val line: Int, val char: Int) : Comparable<LogPosition> {
    override fun compareTo(other: LogPosition) = compareValuesBy(this, other, { it.line }, { it.char })
}

/**
 * Lines of text, the last at the bottom, like the output of a console. The wheel scrolls back, new lines scroll
 * into view while the log is at its end. Dragging selects text, a double click a word, a triple click a line, shift
 * extends the selection; mod+c copies it and mod+a selects everything, while the log has the keyboard, which a press
 * gives it. The context menu copies the selection or the log, or clears it. The prompts of lines are not selected.
 */
class ConsoleLog : Element(ElementType("console-log")) {
    override val handlesDoubleClick = true
    private val lineList = ArrayList<ConsoleLine>()

    /** the lines, the oldest first */
    val lines: List<ConsoleLine> get() = lineList

    /** the most lines that are kept, the oldest go */
    var maxLines = 5000

    /** the number of lines the log is scrolled back from its end */
    var scrolledBack = 0
        private set

    fun add(line: ConsoleLine) {
        lineList.add(line)
        if (lineList.size > maxLines) {
            val removed = lineList.size - maxLines
            lineList.subList(0, removed).clear()
            // the selection stays with its lines
            fun shift(p: LogPosition) = if (p.line - removed < 0) LogPosition(0, 0) else p.copy(line = p.line - removed)
            anchor = anchor?.let(::shift)
            focus = focus?.let(::shift)
        }
        // the log stays where it was scrolled back to, and at the end when it was there
        if (scrolledBack > 0) scrolledBack = (scrolledBack + 1).coerceAtMost(lineList.size)
        requestRedraw()
    }

    /** replaces the line [index] with [line] */
    fun set(index: Int, line: ConsoleLine) {
        lineList[index] = line
        requestRedraw()
    }

    fun clear() {
        lineList.clear()
        scrolledBack = 0
        anchor = null
        focus = null
        requestRedraw()
    }

    //<editor-fold desc="selection">
    /** where the selection started, and where it ends, null when nothing is selected */
    private var anchor: LogPosition? = null
    private var focus: LogPosition? = null

    /** the selected text, from its start to its end, null when nothing is selected */
    val selection: Pair<LogPosition, LogPosition>?
        get() {
            val a = anchor ?: return null
            val b = focus ?: return null
            return if (a == b) null else minOf(a, b) to maxOf(a, b)
        }

    /** selects the text from [start] to [end] */
    fun select(start: LogPosition, end: LogPosition) {
        anchor = start
        focus = end
        requestRedraw()
    }

    fun selectAll() {
        if (lineList.isNotEmpty()) select(LogPosition(0, 0), LogPosition(lineList.lastIndex, lineList.last().text.length))
    }

    fun clearSelection() {
        anchor = null
        focus = null
        requestRedraw()
    }

    /** the text of the selection, its lines joined by line breaks, without the prompts; empty when nothing is selected */
    val selectedText: String
        get() {
            val (a, b) = selection ?: return ""
            return (a.line..b.line).joinToString("\n") { i ->
                val t = lineList[i].text
                t.substring(if (i == a.line) a.char.coerceIn(0, t.length) else 0, if (i == b.line) b.char.coerceIn(0, t.length) else t.length)
            }
        }

    /** called when the log was clicked without selecting, like to give the keyboard back to an input */
    internal var clickedWithoutSelecting: (() -> Unit)? = null

    private val clipboard get() = (root() as? Body)?.controlManager?.program?.clipboard

    /** copies the selection to the clipboard */
    fun copySelection() {
        selectedText.takeIf { it.isNotEmpty() }?.let { clipboard?.contents = it }
    }

    /** the position in the log nearest to [p], a point of the window */
    private fun positionAt(p: Vector2): LogPosition? {
        if (lineList.isEmpty()) return null
        val text = panelText ?: return null
        val content = layout.contentBoundsAtOrigin
        val local = p - Vector2(layout.screenX, layout.screenY)
        val h = lineHeight
        val last = lineList.size - 1 - scrolledBack
        // over the first line or under the last, the beginning of the one and the end of the other
        val row = kotlin.math.floor((content.y + content.height - local.y) / h).toInt()
        val i = last - row
        if (i < 0) return LogPosition(0, 0)
        if (i > lineList.lastIndex) return LogPosition(lineList.lastIndex, lineList.last().text.length)
        val line = lineList[i]
        val x0 = content.x + line.indent + (line.prompt?.let { text.width(computedStyle, it) } ?: 0.0)
        val carets = text.carets(computedStyle, line.text)
        val c = carets.indices.minBy { kotlin.math.abs(x0 + carets[it] - local.x) }
        return LogPosition(i, c)
    }

    /** the point of the window at [p], in the middle of its line, where the log shows it now */
    internal fun pointOf(p: LogPosition): Vector2 {
        val content = layout.contentBoundsAtOrigin
        val row = lineList.size - 1 - scrolledBack - p.line
        val y = content.y + content.height - (row + 0.5) * lineHeight
        return Vector2(layout.screenX + xOf(lineList[p.line], p.char), layout.screenY + y)
    }

    /** the word around [p]: letters, digits and the characters of names, or a single other character */
    private fun wordAt(p: LogPosition): Pair<LogPosition, LogPosition> {
        val t = lineList[p.line].text
        fun inWord(c: Char) = c.isLetterOrDigit() || c == '_' || c == '#'
        var a = p.char.coerceIn(0, t.length)
        var b = a
        if (a < t.length && inWord(t[a]) || a > 0 && inWord(t[a - 1])) {
            while (a > 0 && inWord(t[a - 1])) a--
            while (b < t.length && inWord(t[b])) b++
        } else if (a < t.length) b = a + 1
        return LogPosition(p.line, a) to LogPosition(p.line, b)
    }

    /** the presses in a row at about the same place, for double and triple clicks */
    private var presses = 0
    private var lastPress = 0L
    private var lastPressPosition = Vector2.ZERO
    private var dragging = false
    private var dragged = false
    private var pointer = Vector2.ZERO
    //</editor-fold>

    private val lineHeight: Double get() = (panelText?.face(computedStyle)?.let { it.ascent - it.descent } ?: 14.0) + 5.0

    /** the x of the character [i] of [line] in the log, from its left */
    internal fun xOf(line: ConsoleLine, i: Int): Double {
        val text = panelText ?: return 0.0
        val carets = text.carets(computedStyle, line.text)
        return layout.contentBoundsAtOrigin.x + line.indent + (line.prompt?.let { text.width(computedStyle, it) } ?: 0.0) +
                carets[i.coerceIn(0, line.text.length)]
    }

    private fun scrollBy(lines: Int) {
        val visible = (layout.contentBoundsAtOrigin.height / lineHeight).toInt()
        scrolledBack = (scrolledBack + lines).coerceIn(0, maxOf(0, lineList.size - visible))
        requestRedraw()
    }

    init {
        mouse.scrolled.listen {
            scrollBy(it.rotation.y.toInt().let { r -> if (r == 0) (if (it.rotation.y > 0) 1 else -1) else r } * 3)
            // a selection that is dragged follows the scrolling
            if (dragging) positionAt(pointer)?.let { p -> focus = p }
            it.cancelPropagation()
        }
        mouse.pressed.listen {
            if (it.button != org.openrndr.MouseButton.LEFT) return@listen
            // the log takes the keyboard, for mod+c
            it.cancelPropagation()
            val now = System.currentTimeMillis()
            presses = if (now - lastPress < 400 && it.position.distanceTo(lastPressPosition) < 4.0) presses + 1 else 1
            lastPress = now
            lastPressPosition = it.position
            pointer = it.position
            dragging = true
            dragged = false
            val p = positionAt(it.position) ?: return@listen
            when {
                presses == 2 -> wordAt(p).let { (a, b) -> select(a, b) }
                presses >= 3 -> select(LogPosition(p.line, 0), LogPosition(p.line, lineList[p.line].text.length))
                org.openrndr.KeyModifier.SHIFT in it.modifiers && anchor != null -> { focus = p; dragged = true }
                else -> { anchor = p; focus = p }
            }
            requestRedraw()
        }
        mouse.dragged.listen {
            if (!dragging) return@listen
            it.cancelPropagation()
            pointer = it.position
            dragged = true
            // over or under the log it scrolls
            val local = it.position.y - layout.screenY
            val content = layout.contentBoundsAtOrigin
            if (local < content.y) scrollBy(1) else if (local > content.y + content.height) scrollBy(-1)
            positionAt(it.position)?.let { p -> focus = p }
            requestRedraw()
        }
        mouse.released.listen {
            if (!dragging) return@listen
            dragging = false
            // a click that selects nothing clears the selection and gives the keyboard back
            if (!dragged && presses == 1) {
                clearSelection()
                clickedWithoutSelecting?.invoke()
            }
        }
        mouse.doubleClicked.listen { it.cancelPropagation() }
        keyboard.pressed.listen {
            val mod = org.openrndr.KeyModifier.CTRL in it.modifiers || org.openrndr.KeyModifier.SUPER in it.modifiers
            when {
                mod && it.name == "c" -> { copySelection(); it.cancelPropagation() }
                mod && it.name == "a" -> { selectAll(); it.cancelPropagation() }
                it.key == org.openrndr.KEY_ESCAPE -> { clearSelection(); clickedWithoutSelecting?.invoke(); it.cancelPropagation() }
            }
        }
        contextMenu = {
            if (selection != null) item("Copy") { copySelection() }
            item("Copy log") { clipboard?.contents = lineList.joinToString("\n") { (it.prompt ?: "") + it.text } }
            item("Clear") { clear() }
        }
    }

    override fun draw(drawer: Drawer) {
        val text = panelText ?: return
        val content = layout.contentBoundsAtOrigin
        val color = computedStyle.effectiveColor ?: ColorRGBa.WHITE
        val h = lineHeight
        val visible = (content.height / h).toInt() + 1
        val last = lineList.size - 1 - scrolledBack
        val texts = mutableListOf<PanelText.Placed>()
        drawer.isolated {
            drawer.drawStyle.clip = drawer.drawStyle.clip?.let { intersection(it, layout.contentBounds) ?: Rectangle(0.0, 0.0, 0.0, 0.0) }
                ?: layout.contentBounds
            for (r in 0 until visible) {
                val i = last - r
                if (i < 0) break
                val line = lineList[i]
                val y = content.y + content.height - (r + 1) * h
                // the selection under the text, a little past the end of lines that it goes on after
                selection?.let { (a, b) ->
                    if (i in a.line..b.line) {
                        val x0 = xOf(line, if (i == a.line) a.char else 0)
                        val x1 = xOf(line, if (i == b.line) b.char else line.text.length) + if (i < b.line) 6.0 else 0.0
                        drawer.stroke = null
                        drawer.fill = ColorRGBa.fromHex("#5ab0ff").opacify(0.35)
                        drawer.rectangle(x0, y, x1 - x0, h)
                    }
                }
                var x = content.x + line.indent
                val lineColor = line.color ?: color
                line.prompt?.let { p ->
                    texts += text.placeInBox(computedStyle, p, Rectangle(x, y, content.width, h), 0.0, 0.5, lineColor.opacify(0.5))
                    x += text.width(computedStyle, p)
                }
                // the line in runs of the colors of its marks
                val colored = line.marks.filter { it.color != null }.sortedBy { it.start }
                if (colored.isEmpty()) {
                    texts += text.placeInBox(computedStyle, line.text, Rectangle(x, y, content.width, h), 0.0, 0.5, lineColor)
                } else {
                    val carets = text.carets(computedStyle, line.text)
                    var at = 0
                    fun run(a: Int, b: Int, c: ColorRGBa) {
                        if (b > a) texts += text.placeInBox(computedStyle, line.text.substring(a, b), Rectangle(x + carets[a], y, content.width, h), 0.0, 0.5, c)
                    }
                    for (m in colored) {
                        val a = m.start.coerceIn(at, line.text.length)
                        val b = m.end.coerceIn(a, line.text.length)
                        run(at, a, lineColor)
                        run(a, b, m.color!!)
                        at = b
                    }
                    run(at, line.text.length, lineColor)
                }
                // wavy lines under the marks that have them
                for (m in line.marks) {
                    val c = m.underline ?: continue
                    val x0 = xOf(line, m.start)
                    val x1 = if (m.end <= m.start) x0 + text.width(computedStyle, "m") else xOf(line, m.end)
                    val baseline = y + (h + text.face(computedStyle).let { it.ascent + it.descent }) / 2.0 + 2.5
                    drawer.fill = null
                    drawer.stroke = c
                    drawer.strokeWeight = 1.0
                    val steps = maxOf(2, ((x1 - x0) / 2.0).toInt())
                    drawer.lineStrip((0..steps).map { k -> Vector2(x0 + (x1 - x0) * k / steps, baseline + if (k % 2 == 0) -1.0 else 1.0) })
                }
            }
            text.drawBatch(drawer, this@ConsoleLog, "lines", texts)
        }
    }
}

/**
 * A console: a [log] of lines over an [input] for commands. Enter fires [Events.submitted] with the command, which,
 * unless it is blank, is echoed in the log after [prompt] and kept in the [history] that up and down go through.
 * [completer] completes in the input, [diagnose] marks the command while it is typed, like the span of an error, and
 * [error] shows an error in a command in the log, under its span.
 */
class Console : Element(ElementType("console")) {
    val log = ConsoleLog()
    val input = TextInput()

    /** the text before echoed commands */
    var prompt = "> "

    /** the colors of the kinds of lines, null for the color of the log */
    var echoColor: ColorRGBa? = null
    var infoColor: ColorRGBa? = ColorRGBa.fromHex("#9aa3b2")
    var okColor: ColorRGBa? = ColorRGBa.fromHex("#7fd18b")
    var errorColor: ColorRGBa? = ColorRGBa.fromHex("#ff7a6e")

    /** true when commands are echoed in the log when they are submitted */
    var echo = true

    class Events : AutoCloseable {
        /** a command was submitted, after it was echoed */
        val submitted = Event<String>("console-submitted")
        override fun close() = submitted.close()
    }

    val events = Events()

    /** the commands that were submitted, the oldest first */
    val history = mutableListOf<String>()

    /** the most commands the history keeps */
    var maxHistory = 500

    /** where up and down are in the history, history.size for the command that is typed */
    private var historyIndex = 0

    /** the command that was typed before up went into the history */
    private var draft = ""

    /** completes the command in the input, see [TextInput.completer] */
    var completer: Completer?
        get() = input.completer
        set(v) { input.completer = v }

    /** the marks of a command while it is typed, like the span of a parse error; null marks nothing */
    var diagnose: ((String) -> List<TextMark>)? = null
        set(v) { field = v; input.marks = v?.invoke(input.value).orEmpty() }

    /** the index in the log of the line that echoed the last command, -1 when there is none */
    private var echoLine = -1

    init {
        append(log)
        append(input)
        input.events.valueChanged.listen { input.marks = diagnose?.invoke(it.newValue).orEmpty() }
        input.events.keyPressed.listen {
            val mod = org.openrndr.KeyModifier.CTRL in it.modifiers || org.openrndr.KeyModifier.SUPER in it.modifiers
            val clipboard = (root() as? Body)?.controlManager?.program?.clipboard
            if (mod && it.name == "v" && pasteLines) {
                val text = clipboard?.contents ?: return@listen
                if ('\n' in text || '\r' in text) {
                    paste(text)
                    it.cancelPropagation()
                    return@listen
                }
            }
            // mod+c without a selection copies the whole command
            if (mod && it.name == "c" && input.selection.isEmpty() && input.value.isNotEmpty()) {
                clipboard?.contents = input.value
                it.cancelPropagation()
                return@listen
            }
            when (it.key) {
                KEY_ARROW_UP -> { browse(-1); it.cancelPropagation() }
                KEY_ARROW_DOWN -> { browse(1); it.cancelPropagation() }
            }
        }
        input.events.submitted.listen { submit(input.value) }
        // a click in the log that selects nothing gives the keyboard back to the input
        log.clickedWithoutSelecting = { (root() as? Body)?.controlManager?.keyboardInput?.requestFocus(input) }
    }

    private fun browse(direction: Int) {
        if (history.isEmpty()) return
        if (historyIndex == history.size) draft = input.value
        historyIndex = (historyIndex + direction).coerceIn(0, history.size)
        input.closeCompletions()
        input.value = if (historyIndex == history.size) draft else history[historyIndex]
        input.marks = diagnose?.invoke(input.value).orEmpty()
    }

    /**
     * true when text with line breaks that is pasted in the input is given as commands: the first line joins what is
     * typed, every finished line is submitted in order, blank lines are skipped, and a last line without a line
     * break stays in the input; false pastes the lines in the input with spaces for the line breaks
     */
    var pasteLines = true

    /** pastes [text] in the input, see [pasteLines] */
    fun paste(text: String) {
        val lines = text.split(Regex("\r\n|\r|\n"))
        input.closeCompletions()
        input.insert(lines.first())
        for ((i, line) in lines.withIndex()) {
            if (i == lines.lastIndex) {
                // the last line, without a line break after it, stays to be edited
                if (i > 0) input.insert(line)
                break
            }
            if (i > 0) input.value = line
            if (input.value.isNotBlank()) submit(input.value) else input.value = ""
        }
        input.marks = diagnose?.invoke(input.value).orEmpty()
    }

    /** submits [command] as if it was typed: it is echoed, kept in the history, and [Events.submitted] fires */
    fun submit(command: String) {
        if (command.isNotBlank() && history.lastOrNull() != command) {
            history.add(command)
            if (history.size > maxHistory) history.removeAt(0)
        }
        historyIndex = history.size
        draft = ""
        input.value = ""
        input.marks = emptyList()
        // an empty command is not echoed
        if (echo && command.isNotBlank()) {
            // the echo has the colors that the command had while it was typed
            val colors = diagnose?.invoke(command).orEmpty().filter { it.color != null }.map { it.copy(underline = null) }
            log.add(ConsoleLine(command, echoColor, colors, prompt = prompt))
            echoLine = log.lines.lastIndex
        } else {
            echoLine = -1
        }
        events.submitted.trigger(command)
    }

    /** adds [text] to the log in [color] */
    fun print(text: String, color: ColorRGBa? = null) {
        for (line in text.lines()) log.add(ConsoleLine(line, color))
    }

    fun info(text: String) = print(text, infoColor)
    fun ok(text: String) = print(text, okColor)
    fun error(text: String) = print(text, errorColor)

    /**
     * shows an error in the characters from [start] to [end] of the last command: they are underlined in the echo of
     * the command, and [message] is shown under them, after a caret. Without an echo the command is shown first
     */
    fun error(message: String, start: Int, end: Int, command: String? = null) {
        val underline = TextMark(start, end, underline = errorColor ?: ColorRGBa.RED)
        val line = if (echoLine in log.lines.indices && (command == null || log.lines[echoLine].text == command)) {
            log.lines[echoLine].let { l -> l.copy(marks = l.marks + underline).also { log.set(echoLine, it) } }
        } else {
            ConsoleLine(command ?: "", echoColor, listOf(underline), prompt = prompt).also { log.add(it) }
        }
        // the message starts under the span
        val x = log.xOf(line, start) - log.layout.contentBoundsAtOrigin.x
        log.add(ConsoleLine("^ $message", errorColor, indent = x - (panelText?.width(computedStyle, "^")?.div(2.0) ?: 0.0)))
    }

    /** empties the log */
    fun clear() = log.clear()

    override fun close() {
        super.close()
        events.close()
    }
}

/** a [Console], [init] sets it up */
fun Element.console(vararg classes: String, init: Console.() -> Unit = {}): Console = initElement(classes, Console(), init)
