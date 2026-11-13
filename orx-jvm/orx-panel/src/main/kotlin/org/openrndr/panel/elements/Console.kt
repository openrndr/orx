package org.openrndr.panel.elements

import org.openrndr.KeyModifier

import org.openrndr.KEY_ARROW_DOWN
import org.openrndr.KEY_ARROW_UP
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.isolated
import org.openrndr.events.Event
import org.openrndr.math.Vector2
import org.openrndr.panel.PanelText
import org.openrndr.panel.intersection
import org.openrndr.panel.style.Cursor
import org.openrndr.panel.style.cursor
import org.openrndr.panel.style.effectiveColor
import org.openrndr.shape.Rectangle

/**
 * a line of a [ConsoleLog]: [text] in [color], after [prompt] dimmed, with [marks] that color and underline spans of
 * [text], [indent] pixels from the left; [data] is what the line stands for, like for [ConsoleLog.lineClicked]. In a
 * log that [ConsoleLog.wrap]s its lines, the rows after the first start [wrapIndent] pixels from the left
 */
data class ConsoleLine(
    val text: String,
    val color: ColorRGBa? = null,
    val marks: List<TextMark> = emptyList(),
    val prompt: String? = null,
    val indent: Double = 0.0,
    val data: Any? = null,
    val wrapIndent: Double = indent
)

/**
 * a click on the line [index] of a [ConsoleLog] that selected nothing, see [ConsoleLog.lineClicked]: at [position] in
 * the line, with the keys of [modifiers] held
 */
class LogLineClicked(val index: Int, val line: ConsoleLine, val position: LogPosition, val modifiers: Set<KeyModifier>)

/** a place in a [ConsoleLog]: before the character [char] of the line [line] */
data class LogPosition(val line: Int, val char: Int) : Comparable<LogPosition> {
    override fun compareTo(other: LogPosition) = compareValuesBy(this, other, { it.line }, { it.char })
}

/** the end of a [ConsoleLog] that it keeps in view, see [ConsoleLog.anchor] */
enum class LogAnchor { START, END }

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

    /**
     * true when lines wider than the log are broken into rows: at spaces, within a word that is wider than a row, and
     * at line breaks; again when the width of the log changes. A line that is broken is still one line, for the
     * selection, copying and [lineClicked]; the rows after the first start at [ConsoleLine.wrapIndent]
     */
    var wrap = false
        set(value) {
            if (value != field) {
                val back = scrolledBack
                field = value
                rowsValid = false
                scrolledBack = back
                requestRedraw()
            }
        }

    /**
     * the end of the log that it keeps in view: [LogAnchor.END], the last line at the bottom, where new lines scroll
     * into view, like the output of a console; or [LogAnchor.START], the first line at the top, like a list that is
     * read from the top. Scrolling is kept from that end, also when the lines are replaced, see [replaceAll]
     */
    var anchor = LogAnchor.END
        set(value) {
            if (value != field) {
                val back = scrolledBack
                field = value
                scrolledBack = back
            }
        }

    /** a line was clicked without selecting anything: a press and a release at the same place */
    val lineClicked = Event<LogLineClicked>("console-log-line-clicked")

    /**
     * true when the lines are clicked to do something, see [lineClicked]: the line under the pointer is highlighted and
     * the pointer is a hand
     */
    var clickableLines = false
        set(value) {
            if (value != field) {
                field = value
                style { cursor = if (value) Cursor.Pointer else Cursor.Default }
                if (!value) hoveredLine = null
                requestRedraw()
            }
        }

    /** the line under the pointer while the lines are [clickableLines] */
    private var hoveredLine: Int? = null
        set(value) {
            if (value != field) {
                field = value
                requestRedraw()
            }
        }

    //<editor-fold desc="rows">
    /** a row of the line [line]: its characters from [start] to [end], starting [x] pixels from the left of the content */
    private class Row(val line: Int, val start: Int, val end: Int, val x: Double)

    private val rowList = ArrayList<Row>()

    /** the index of the first row of every line, and the number of rows after the last line */
    private var firstRow = IntArray(1)
    private var rowsValid = false
    private var rowsWidth = -1.0

    /** the characters where a line is broken, as start and end pairs, for the width it was broken at */
    private class Breaks(val width: Double, val spans: IntArray)

    /** what a line is broken by */
    private data class BreakKey(val text: String, val prompt: String?, val indent: Double, val wrapIndent: Double)

    /** the breaks of lines by their text, so that lines that are added again are not broken again */
    private val breaks = object : LinkedHashMap<BreakKey, Breaks>(256, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<BreakKey, Breaks>?) = size > 2 * maxLines
    }

    private fun promptWidth(line: ConsoleLine): Double = line.prompt?.let { p -> panelText?.width(computedStyle, p) } ?: 0.0

    /** the rows of [line] at [width], as start and end pairs */
    private fun spansOf(line: ConsoleLine, width: Double): IntArray {
        val t = line.text
        if (!wrap || width <= 0.0) return intArrayOf(0, t.length)
        val key = BreakKey(t, line.prompt, line.indent, line.wrapIndent)
        breaks[key]?.takeIf { it.width == width }?.let { return it.spans }
        val text = panelText ?: return intArrayOf(0, t.length)
        val carets = text.carets(computedStyle, t)
        val spans = ArrayList<Int>()
        val firstX = line.indent + promptWidth(line)
        var s = 0
        var first = true
        while (true) {
            val available = width - if (first) firstX else line.wrapIndent
            val hard = t.indexOf('\n', s).let { if (it < 0) t.length else it }
            val x0 = carets[s]
            if (carets[hard] - x0 <= available) {
                spans.add(s); spans.add(hard)
                if (hard >= t.length) break
                s = hard + 1
            } else {
                // the last character that fits, at least one
                var e = s + 1
                while (e < hard && carets[e + 1] - x0 <= available) e++
                // at the last space before it, or within the word that does not fit
                var k = e
                while (k > s && !(t[k].isWhitespace() && t[k] != '\n')) k--
                if (k > s) {
                    spans.add(s); spans.add(k)
                    s = k
                    while (s < hard && t[s].isWhitespace()) s++
                    // spaces at the end of a row that is followed by nothing
                    if (s == t.length) break
                } else {
                    spans.add(s); spans.add(e)
                    s = e
                }
            }
            first = false
        }
        return spans.toIntArray().also { breaks[key] = Breaks(width, it) }
    }

    /** the rows of all lines, broken again when the lines or the width changed */
    private fun ensureRows() {
        val width = layout.contentBoundsAtOrigin.width
        if (rowsValid && width == rowsWidth) return
        rowsValid = true
        rowsWidth = width
        rowList.clear()
        firstRow = IntArray(lineList.size + 1)
        for (i in lineList.indices) appendRows(i)
        firstRow[lineList.size] = rowList.size
    }

    private fun appendRows(i: Int) {
        val line = lineList[i]
        firstRow[i] = rowList.size
        val spans = spansOf(line, rowsWidth)
        for (k in 0 until spans.size / 2) {
            rowList.add(Row(i, spans[2 * k], spans[2 * k + 1], if (k == 0) line.indent + promptWidth(line) else line.wrapIndent))
        }
    }

    /** the number of rows of all lines */
    private val rowCount: Int get() = ensureRows().let { rowList.size }

    /** the rows of the line [i] */
    private fun rowsOf(i: Int): List<Row> {
        ensureRows()
        return rowList.subList(firstRow[i], firstRow[i + 1])
    }

    /** the row of the character [c] of the line [i]: the last that starts at or before it */
    private fun rowOf(i: Int, c: Int): Int {
        ensureRows()
        var r = firstRow[i]
        while (r + 1 < firstRow[i + 1] && rowList[r + 1].start <= c) r++
        return r
    }
    //</editor-fold>

    /**
     * the number of rows the log is scrolled back from its end, at least 0 and at most what leaves no room; rows are
     * lines, unless the lines [wrap]
     */
    var scrolledBack: Int
        get() = if (anchor == LogAnchor.END) back else maxOf(0, rowCount - visibleLines - topRow)
        set(value) {
            val v = value.coerceIn(0, maxScroll)
            if (anchor == LogAnchor.END) back = v else topRow = maxScroll - v
            requestRedraw()
        }

    /** the number of rows the log is scrolled down from its start */
    var scrolledDown: Int
        get() = maxScroll - scrolledBack
        set(value) { scrolledBack = maxScroll - value }

    /** the rows from the end, with [LogAnchor.END] */
    private var back = 0

    /** the first row that is shown, with [LogAnchor.START] */
    private var topRow = 0

    /** the number of whole rows that fit in the log; rows are lines, unless the lines [wrap] */
    val visibleLines: Int get() = (layout.contentBoundsAtOrigin.height / lineHeight).toInt().coerceAtLeast(1)

    /** how far the log scrolls: the rows that do not fit */
    private val maxScroll: Int get() = maxOf(0, rowCount - visibleLines)

    /** shows the first line at the top */
    fun scrollToStart() { scrolledDown = 0 }

    /** shows the last line at the bottom */
    fun scrollToEnd() { scrolledBack = 0 }

    fun add(line: ConsoleLine) {
        val wasValid = rowsValid
        lineList.add(line)
        var trimmed = 0
        if (lineList.size > maxLines) {
            val removed = lineList.size - maxLines
            trimmed = if (wasValid) firstRow[removed] else removed
            lineList.subList(0, removed).clear()
            // the selection stays with its lines
            fun shift(p: LogPosition) = if (p.line - removed < 0) LogPosition(0, 0) else p.copy(line = p.line - removed)
            selectionStart = selectionStart?.let(::shift)
            focus = focus?.let(::shift)
            rowsValid = false
        } else if (wasValid) {
            // the rows of the new line after the others
            firstRow = firstRow.copyOf(lineList.size + 1)
            appendRows(lineList.lastIndex)
            firstRow[lineList.size] = rowList.size
        }
        topRow = maxOf(0, topRow - trimmed)
        // the log stays where it was scrolled back to, and at the end when it was there; from the start, the lines
        // are added under what shows
        if (anchor == LogAnchor.END && back > 0) back = (back + rowsOf(lineList.lastIndex).size).coerceAtMost(rowCount)
        requestRedraw()
    }

    /**
     * replaces the lines with [lines], like a list that is built again: the log stays scrolled as far from the end it
     * keeps in view, see [anchor], as far as there are lines; the selection is cleared
     */
    fun replaceAll(lines: List<ConsoleLine>) {
        lineList.clear()
        lineList.addAll(lines.takeLast(maxLines))
        rowsValid = false
        back = back.coerceIn(0, maxScroll)
        topRow = topRow.coerceIn(0, maxScroll)
        selectionStart = null
        focus = null
        requestRedraw()
    }

    /** replaces the line [index] with [line] */
    fun set(index: Int, line: ConsoleLine) {
        lineList[index] = line
        rowsValid = false
        requestRedraw()
    }

    /** removes the lines, and shows the end of the log it keeps in view, see [anchor] */
    fun clear() {
        lineList.clear()
        rowsValid = false
        back = 0
        topRow = 0
        selectionStart = null
        focus = null
        requestRedraw()
    }

    //<editor-fold desc="selection">
    /** where the selection started, and where it ends, null when nothing is selected */
    private var selectionStart: LogPosition? = null
    private var focus: LogPosition? = null

    /** the selected text, from its start to its end, null when nothing is selected */
    val selection: Pair<LogPosition, LogPosition>?
        get() {
            val a = selectionStart ?: return null
            val b = focus ?: return null
            return if (a == b) null else minOf(a, b) to maxOf(a, b)
        }

    /** selects the text from [start] to [end] */
    fun select(start: LogPosition, end: LogPosition) {
        selectionStart = start
        focus = end
        requestRedraw()
    }

    fun selectAll() {
        if (lineList.isNotEmpty()) select(LogPosition(0, 0), LogPosition(lineList.lastIndex, lineList.last().text.length))
    }

    fun clearSelection() {
        selectionStart = null
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
        // over the first line or under the last, the beginning of the one and the end of the other
        val r = rowAt(local.y)
        if (r < 0) return LogPosition(0, 0)
        if (r >= rowCount) return LogPosition(lineList.lastIndex, lineList.last().text.length)
        val row = rowList[r]
        val carets = text.carets(computedStyle, lineList[row.line].text)
        val c = (row.start..row.end).minBy { kotlin.math.abs(content.x + row.x + carets[it] - carets[row.start] - local.x) }
        return LogPosition(row.line, c)
    }

    /** the point of the window at [p], in the middle of its line, where the log shows it now */
    internal fun pointOf(p: LogPosition): Vector2 {
        val y = rowTop(rowOf(p.line, p.char)) + lineHeight / 2.0
        return Vector2(layout.screenX + xOf(p.line, p.char), layout.screenY + y)
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

    /** the x of the character [c] of the line [i] in the log, from its left, in the row it is in */
    internal fun xOf(i: Int, c: Int): Double {
        val text = panelText ?: return 0.0
        val line = lineList[i]
        val carets = text.carets(computedStyle, line.text)
        val row = rowList[rowOf(i, c)]
        return layout.contentBoundsAtOrigin.x + row.x + carets[c.coerceIn(row.start, row.end)] - carets[row.start]
    }

    private fun scrollBy(lines: Int) {
        scrolledBack += lines
    }

    /** the top of the row [r] in the log, from the bottom with [LogAnchor.END] and from the top with [LogAnchor.START] */
    private fun rowTop(r: Int): Double {
        val content = layout.contentBoundsAtOrigin
        return if (anchor == LogAnchor.END) content.y + content.height - (rowCount - scrolledBack - r) * lineHeight
        else content.y + (r - topRow) * lineHeight
    }

    /** the index of the row at [y] in the log, outside the rows when it is before the first or after the last */
    private fun rowAt(y: Double): Int {
        val content = layout.contentBoundsAtOrigin
        return if (anchor == LogAnchor.END) rowCount - 1 - scrolledBack - kotlin.math.floor((content.y + content.height - y) / lineHeight).toInt()
        else topRow + kotlin.math.floor((y - content.y) / lineHeight).toInt()
    }

    /** the index of the line at [y] in the log, outside the lines when it is before the first or after the last */
    private fun lineAt(y: Double): Int {
        val r = rowAt(y)
        return when {
            r < 0 -> -1
            r >= rowCount -> lineList.size
            else -> rowList[r].line
        }
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
                org.openrndr.KeyModifier.SHIFT in it.modifiers && selectionStart != null -> { focus = p; dragged = true }
                else -> { selectionStart = p; focus = p }
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
                // on a line, not under the last or beside the log
                val i = lineAt(it.position.y - layout.screenY)
                val p = positionAt(it.position)
                if (i in lineList.indices && p != null && it.position in screenArea) {
                    lineClicked.trigger(LogLineClicked(i, lineList[i], p, it.modifiers))
                }
            }
        }
        mouse.moved.listen {
            if (clickableLines) hoveredLine = lineAt(it.position.y - layout.screenY).takeIf { i -> i in lineList.indices }
        }
        mouse.exited.listen { hoveredLine = null }
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
        ensureRows()
        // the rows in view, and the one partly in view at the far end
        val first = maxOf(0, rowAt(content.y))
        val last = minOf(rowList.lastIndex, rowAt(content.y + content.height - 1E-6))
        val texts = mutableListOf<PanelText.Placed>()
        drawer.isolated {
            drawer.drawStyle.clip = drawer.drawStyle.clip?.let { intersection(it, layout.contentBounds) ?: Rectangle(0.0, 0.0, 0.0, 0.0) }
                ?: layout.contentBounds
            for (r in first..last) {
                val row = rowList[r]
                val i = row.line
                val line = lineList[i]
                val y = rowTop(r)
                val carets = text.carets(computedStyle, line.text)
                // the x of the character c in this row
                fun x(c: Int) = content.x + row.x + carets[c.coerceIn(row.start, row.end)] - carets[row.start]
                // the line under the pointer, when lines are clicked
                if (i == hoveredLine && clickableLines) {
                    drawer.stroke = null
                    drawer.fill = color.opacify(0.08)
                    drawer.rectangle(content.x, y, content.width, h)
                }
                // the selection under the text, a little past the end of rows that it goes on after
                selection?.let { (a, b) ->
                    val from = if (i == a.line) a.char else 0
                    val to = if (i == b.line) b.char else line.text.length
                    if (i in a.line..b.line && from <= row.end && to >= row.start) {
                        val x0 = x(maxOf(from, row.start))
                        val past = to > row.end || (i < b.line && row.end == line.text.length)
                        val x1 = x(minOf(to, row.end)) + if (past) 6.0 else 0.0
                        drawer.stroke = null
                        drawer.fill = ColorRGBa.fromHex("#5ab0ff").opacify(0.35)
                        drawer.rectangle(x0, y, x1 - x0, h)
                    }
                }
                val lineColor = line.color ?: color
                // the prompt on the first row
                if (row.start == 0) line.prompt?.let { p ->
                    val px = content.x + line.indent
                    texts += text.placeInBox(computedStyle, p, Rectangle(px, y, content.width, h), 0.0, 0.5, lineColor.opacify(0.5))
                }
                // the row in runs of the colors of the marks
                fun run(a: Int, b: Int, c: ColorRGBa) {
                    val from = maxOf(a, row.start)
                    val to = minOf(b, row.end)
                    if (to > from) texts += text.placeInBox(computedStyle, line.text.substring(from, to), Rectangle(x(from), y, content.width, h), 0.0, 0.5, c)
                }
                val colored = line.marks.filter { it.color != null }.sortedBy { it.start }
                var at = 0
                for (m in colored) {
                    val a = m.start.coerceIn(at, line.text.length)
                    val b = m.end.coerceIn(a, line.text.length)
                    run(at, a, lineColor)
                    run(a, b, m.color!!)
                    at = b
                }
                run(at, line.text.length, lineColor)
                // wavy lines under the marks that have them, in this row
                for (m in line.marks) {
                    val c = m.underline ?: continue
                    val empty = m.end <= m.start
                    if (m.start > row.end || (!empty && m.end <= row.start) || (empty && m.start < row.start)) continue
                    val x0 = x(maxOf(m.start, row.start))
                    val x1 = if (empty) x0 + text.width(computedStyle, "m") else x(minOf(m.end, row.end))
                    if (x1 <= x0) continue
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
        val index = if (echoLine in log.lines.indices && (command == null || log.lines[echoLine].text == command)) {
            log.lines[echoLine].let { l -> log.set(echoLine, l.copy(marks = l.marks + underline)) }
            echoLine
        } else {
            log.add(ConsoleLine(command ?: "", echoColor, listOf(underline), prompt = prompt))
            log.lines.lastIndex
        }
        // the message starts under the span
        val x = log.xOf(index, start) - log.layout.contentBoundsAtOrigin.x
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
