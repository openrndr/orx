package org.openrndr.panel.elements

import org.openrndr.*
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.StrokeAlignment
import org.openrndr.draw.isolated
import org.openrndr.events.Event
import org.openrndr.math.Vector2
import org.openrndr.panel.Edit
import org.openrndr.panel.binding.PropertyBinding
import org.openrndr.panel.binding.bindValue
import org.openrndr.panel.editHistory
import org.openrndr.panel.style.*
import org.openrndr.shape.Rectangle
import kotlin.math.abs
import kotlin.math.floor
import kotlin.reflect.KMutableProperty0

/** a range of a line in [color], see [TextArea.highlighter] */
data class Highlight(val start: Int, val end: Int, val color: ColorRGBa)

/**
 * a range of the text from [start] to [end], exclusive, in characters of [TextArea.value], underlined in [color] with
 * [message] as the tooltip, like a problem; see [TextArea.marks]
 */
data class Mark(val start: Int, val end: Int, val message: String, val color: ColorRGBa)

/** highlighters of code for [TextArea.highlighter] */
object Highlighters {
    private val number = Regex("""\b\d+(\.\d+)?([eE][-+]?\d+)?[fFuU]?\b""")
    private val string = Regex(""""([^"\\]|\\.)*"""")
    private val word = Regex("""\b[A-Za-z_][A-Za-z0-9_]*\b""")

    /** colors for the kinds of words */
    var keywordColor = ColorRGBa.fromHex("#c49bff")
    var typeColor = ColorRGBa.fromHex("#7fd1b9")
    var numberColor = ColorRGBa.fromHex("#ffe08a")
    var stringColor = ColorRGBa.fromHex("#f8b779")
    var commentColor = ColorRGBa.fromHex("#7c8594")
    var functionColor = ColorRGBa.fromHex("#82aaff")

    /** a highlighter of [keywords] and [types], numbers, strings and comments after [comment] */
    fun of(keywords: Set<String>, types: Set<String>, comment: String = "//"): (String) -> List<Highlight> = { line ->
        val result = mutableListOf<Highlight>()
        val commentAt = line.indexOf(comment).takeIf { it >= 0 } ?: line.length
        val code = line.substring(0, commentAt)
        val strings = string.findAll(code).map { it.range }.toList()
        fun inString(i: Int) = strings.any { i in it }
        for (r in strings) result.add(Highlight(r.first, r.last + 1, stringColor))
        for (m in number.findAll(code)) if (!inString(m.range.first)) result.add(Highlight(m.range.first, m.range.last + 1, numberColor))
        for (m in word.findAll(code)) {
            if (inString(m.range.first)) continue
            when (m.value) {
                in keywords -> result.add(Highlight(m.range.first, m.range.last + 1, keywordColor))
                in types -> result.add(Highlight(m.range.first, m.range.last + 1, typeColor))
            }
        }
        if (commentAt < line.length) result.add(Highlight(commentAt, line.length, commentColor))
        result
    }

    /** GLSL, like the code of shade styles */
    val glsl = of(
        setOf("if", "else", "for", "while", "do", "return", "break", "continue", "discard", "in", "out", "inout", "uniform",
            "const", "struct", "layout", "flat", "smooth", "precision", "highp", "mediump", "lowp", "true", "false", "switch", "case", "default"),
        setOf("void", "bool", "int", "uint", "float", "double", "vec2", "vec3", "vec4", "ivec2", "ivec3", "ivec4", "uvec2", "uvec3",
            "uvec4", "bvec2", "bvec3", "bvec4", "mat2", "mat3", "mat4", "sampler2D", "sampler3D", "samplerCube", "sampler2DArray")
    )

    /** the expressions of orx-expression-evaluator and orx-expression-evaluator-typed, see [ExpressionArea] */
    val expressions: (String) -> List<Highlight> = ::expressionHighlights

    /** Kotlin */
    val kotlin = of(
        setOf("fun", "val", "var", "if", "else", "when", "for", "while", "do", "return", "break", "continue", "class", "object",
            "interface", "private", "internal", "public", "override", "open", "data", "sealed", "enum", "companion", "in", "is",
            "as", "null", "true", "false", "this", "super", "import", "package", "try", "catch", "finally", "throw", "by", "lazy"),
        setOf("Int", "Long", "Double", "Float", "Boolean", "String", "Char", "Unit", "Any", "List", "Map", "Set", "Vector2",
            "Vector3", "Vector4", "ColorRGBa", "Drawer", "Program")
    )
}

/**
 * Multi-line text that is edited: a caret and a selection that the pointer and the keys move, with shift extending
 * the selection and alt or ctrl moving by words; enter keeps the indentation of the line, mod+enter fires [submitted],
 * tab indents and shift-tab outdents the selected lines; mod+a, mod+c, mod+x and mod+v select all, copy, cut and
 * paste. The text scrolls with the wheel and keeps the caret in view; the edits can be undone, see
 * [org.openrndr.panel.History]. [lineNumbers] shows the numbers of the lines, [highlighter] colors the lines, see
 * [Highlighters], [marks] underline parts of the text, like problems, with their messages as tooltips.
 */
open class TextArea : Element(ElementType("text-area")) {
    override val handlesKeyboardFocus = true
    override val handlesDoubleClick = true

    /** the text changed, by the user or not */
    val valueChanged = Event<ValueChange<String>>("text-area-value-changed")

    /** mod+enter was pressed, with the text, like to send it; enter alone starts a new line */
    val submitted = Event<String>("text-area-submitted")

    private var lines = mutableListOf("")

    /** the text */
    var value: String
        get() = lines.joinToString("\n")
        set(v) {
            if (v == value) return
            val old = value
            lines = v.split("\n").toMutableList()
            caret = clamp(caret)
            anchor = caret
            requestRedraw()
            valueChanged.trigger(ValueChange(old, v, false))
        }

    /** true to show the numbers of the lines at the left */
    var lineNumbers = true
        set(v) { field = v; requestRedraw() }

    /** the colors of the parts of a line, null for one color */
    var highlighter: ((String) -> List<Highlight>)? = null
        set(v) { field = v; requestRedraw() }

    /**
     * the ranges of the text that are underlined, like problems, and colored in the numbers of their lines; the message
     * of a mark shows as the tooltip when the pointer rests on it. Marks stay where they are when the text changes
     */
    var marks: List<Mark> = emptyList()
        set(v) { field = v; requestRedraw() }

    /** the number of spaces of a tab */
    var tabSize = 4

    /** the position of the caret: a line and a column */
    data class Position(val line: Int, val column: Int) : Comparable<Position> {
        override fun compareTo(other: Position) = compareValuesBy(this, other, { it.line }, { it.column })
    }

    var caret = Position(0, 0)
        private set
    private var anchor = Position(0, 0)

    /** the column the caret goes back to when it moves up and down through shorter lines, in pixels */
    private var preferredX: Double? = null

    /** the text that is selected */
    val selectedText: String
        get() {
            val (a, b) = ordered()
            return if (a.line == b.line) lines[a.line].substring(a.column, b.column)
            else (listOf(lines[a.line].substring(a.column)) + lines.subList(a.line + 1, b.line) + lines[b.line].substring(0, b.column)).joinToString("\n")
        }

    private var scroll = Vector2.ZERO

    /** the position of the character [offset] of [value] */
    fun positionOf(offset: Int): Position {
        var rest = offset.coerceAtLeast(0)
        for ((i, line) in lines.withIndex()) {
            if (rest <= line.length) return Position(i, rest)
            rest -= line.length + 1
        }
        return Position(lines.lastIndex, lines.last().length)
    }

    /** the offset in [value] of [position] */
    fun offsetOf(position: Position): Int {
        val p = clamp(position)
        var offset = 0
        for (i in 0 until p.line) offset += lines[i].length + 1
        return offset + p.column
    }

    /** the marks at [offset], from their start to their end, both inclusive */
    fun marksAt(offset: Int): List<Mark> = marks.filter { offset in it.start..it.end }

    /** the marks under the screen position [p], which is on the text of a line, not below it or past its end */
    private fun markedAt(p: Vector2): List<Mark> {
        val local = p - Vector2(layout.screenX, layout.screenY) + scroll
        val line = floor((local.y - 4.0) / lineHeight).toInt()
        if (line !in lines.indices || local.x - gutter > carets(line).last() + 6.0) return emptyList()
        return marksAt(offsetOf(positionAt(p)))
    }

    /** the messages of the marks under the pointer, or the tooltip that is set */
    override var tooltip: String?
        get() {
            val pointer = (root() as? Body)?.controlManager?.mousePosition
            val marked = pointer?.takeIf { marks.isNotEmpty() && containsScreenPoint(it) }?.let { markedAt(it) }.orEmpty()
            return if (marked.isNotEmpty()) marked.joinToString("\n") { it.message } else super.tooltip
        }
        set(v) { super.tooltip = v }

    private fun ordered() = if (anchor <= caret) anchor to caret else caret to anchor
    private fun clamp(p: Position): Position {
        val line = p.line.coerceIn(0, lines.lastIndex)
        return Position(line, p.column.coerceIn(0, lines[line].length))
    }

    //<editor-fold desc="geometry">
    private val lineHeight: Double get() = (panelText?.face(computedStyle)?.let { (it.ascent - it.descent) * 1.35 } ?: 18.0)
    private val gutter: Double get() = if (lineNumbers) (panelText?.width(computedStyle, "${lines.size}") ?: 20.0) + 18.0 else 6.0
    private fun carets(line: Int): DoubleArray = panelText?.carets(computedStyle, lines[line].replace('\t', ' ')) ?: DoubleArray(lines[line].length + 1)

    private fun positionAt(p: Vector2): Position {
        val local = p - Vector2(layout.screenX, layout.screenY) + scroll
        val line = floor((local.y - 4.0) / lineHeight).toInt().coerceIn(0, lines.lastIndex)
        val x = local.x - gutter
        val c = carets(line)
        return Position(line, c.indices.minBy { abs(c[it] - x) })
    }

    /** scrolls the text so that the caret is in view */
    private fun reveal() {
        val y = caret.line * lineHeight + 4.0
        val x = carets(caret.line)[caret.column] + gutter
        val w = layout.screenWidth
        val h = layout.screenHeight
        var s = scroll
        if (y < s.y) s = s.copy(y = y)
        if (y + lineHeight > s.y + h) s = s.copy(y = y + lineHeight - h + 4.0)
        if (x < s.x + gutter) s = s.copy(x = maxOf(0.0, x - gutter - 20.0))
        if (x > s.x + w - 8.0) s = s.copy(x = x - w + 40.0)
        scroll = s
    }
    //</editor-fold>

    //<editor-fold desc="editing">
    /** replaces the selection with [text], the caret after it; recorded in the history of the control manager */
    fun insert(text: String) {
        val beforeText = value
        val beforeCaret = caret
        val beforeAnchor = anchor
        val (a, b) = ordered()
        val head = lines[a.line].substring(0, a.column)
        val tail = lines[b.line].substring(b.column)
        val inserted = (head + text + tail).split("\n")
        for (i in b.line downTo a.line) lines.removeAt(i)
        lines.addAll(a.line, inserted)
        val lastLine = a.line + inserted.size - 1
        caret = Position(lastLine, inserted.last().length - tail.length)
        anchor = caret
        changed(beforeText, beforeCaret, beforeAnchor)
    }

    private fun changed(beforeText: String, beforeCaret: Position, beforeAnchor: Position) {
        val afterText = value
        val afterCaret = caret
        preferredX = null
        reveal()
        requestRedraw()
        if (afterText == beforeText) return
        valueChanged.trigger(ValueChange(beforeText, afterText, true))
        // typing that goes on is one edit, see History
        editHistory?.record("Edit text", this, Triple(beforeText, beforeCaret, beforeAnchor), Triple(afterText, afterCaret, afterCaret)) { (t, c, a) ->
            val old = value
            lines = t.split("\n").toMutableList()
            caret = clamp(c); anchor = clamp(a)
            reveal()
            requestRedraw()
            valueChanged.trigger(ValueChange(old, t, false))
        }
    }

    private fun move(to: Position, select: Boolean) {
        caret = clamp(to)
        if (!select) anchor = caret
        reveal()
        requestRedraw()
    }

    private fun wordBoundary(from: Position, direction: Int): Position {
        val line = lines[from.line]
        var i = from.column
        if (direction < 0) {
            if (i == 0) return if (from.line > 0) Position(from.line - 1, lines[from.line - 1].length) else from
            while (i > 0 && !line[i - 1].isLetterOrDigit()) i--
            while (i > 0 && line[i - 1].isLetterOrDigit()) i--
        } else {
            if (i == line.length) return if (from.line < lines.lastIndex) Position(from.line + 1, 0) else from
            while (i < line.length && !line[i].isLetterOrDigit()) i++
            while (i < line.length && line[i].isLetterOrDigit()) i++
        }
        return Position(from.line, i)
    }

    private fun left(p: Position) = if (p.column > 0) Position(p.line, p.column - 1) else if (p.line > 0) Position(p.line - 1, lines[p.line - 1].length) else p
    private fun right(p: Position) = if (p.column < lines[p.line].length) Position(p.line, p.column + 1) else if (p.line < lines.lastIndex) Position(p.line + 1, 0) else p

    private fun vertical(delta: Int, select: Boolean) {
        val x = preferredX ?: carets(caret.line)[caret.column]
        val line = (caret.line + delta).coerceIn(0, lines.lastIndex)
        val c = carets(line)
        move(Position(line, c.indices.minBy { abs(c[it] - x) }), select)
        preferredX = x
    }

    /** indents the selected lines by [tabSize] spaces, or outdents them */
    private fun indent(out: Boolean) {
        val beforeText = value
        val beforeCaret = caret
        val beforeAnchor = anchor
        val (a, b) = ordered()
        val pad = " ".repeat(tabSize)
        var caretShift = 0
        var anchorShift = 0
        for (i in a.line..b.line) {
            val removed = if (out) lines[i].takeWhile { it == ' ' }.length.coerceAtMost(tabSize) else -tabSize
            lines[i] = if (out) lines[i].substring(removed) else pad + lines[i]
            if (i == caret.line) caretShift = -removed
            if (i == anchor.line) anchorShift = -removed
        }
        caret = clamp(caret.copy(column = maxOf(0, caret.column + caretShift)))
        anchor = clamp(anchor.copy(column = maxOf(0, anchor.column + anchorShift)))
        changed(beforeText, beforeCaret, beforeAnchor)
    }
    //</editor-fold>

    init {
        // the caret shows while the area has the focus, also when the focus comes by the keyboard or from code
        keyboard.focusGained.listen { requestRedraw() }
        keyboard.focusLost.listen { requestRedraw() }
        mouse.pressed.listen {
            val p = positionAt(it.position)
            move(p, KeyModifier.SHIFT in it.modifiers)
            preferredX = null
            it.cancelPropagation()
        }
        mouse.dragged.listen {
            move(positionAt(it.position), true)
            it.cancelPropagation()
        }
        mouse.doubleClicked.listen {
            // the word under the pointer
            val p = positionAt(it.position)
            anchor = wordBoundary(Position(p.line, minOf(p.column + 1, lines[p.line].length)), -1)
            caret = wordBoundary(anchor, 1)
            requestRedraw()
            it.cancelPropagation()
        }
        mouse.scrolled.listen {
            val maxY = maxOf(0.0, lines.size * lineHeight + 8.0 - layout.screenHeight)
            scroll = Vector2(maxOf(0.0, scroll.x - it.rotation.x * 20.0), (scroll.y - it.rotation.y * lineHeight * 3.0).coerceIn(0.0, maxY))
            requestRedraw()
            it.cancelPropagation()
        }
        keyboard.character.listen {
            insert(it.character.toString())
            it.cancelPropagation()
        }
        keyboard.pressed.listen { handleKey(it) }
        keyboard.repeated.listen { handleKey(it) }
    }

    private fun handleKey(event: KeyEvent) {
        val shift = KeyModifier.SHIFT in event.modifiers
        val word = KeyModifier.ALT in event.modifiers || KeyModifier.CTRL in event.modifiers
        val command = KeyModifier.SUPER in event.modifiers || KeyModifier.CTRL in event.modifiers
        val clipboard = (root() as? Body)?.controlManager?.program?.clipboard
        if (command && event.key == KEY_ENTER) {
            submitted.trigger(value)
            event.cancelPropagation()
            return
        }
        if (command && event.name.length == 1) {
            when (event.name) {
                "a" -> { anchor = Position(0, 0); caret = Position(lines.lastIndex, lines.last().length); requestRedraw() }
                "c" -> if (selectedText.isNotEmpty()) clipboard?.contents = selectedText
                "x" -> if (selectedText.isNotEmpty()) { clipboard?.contents = selectedText; insert("") }
                "v" -> clipboard?.contents?.let { insert(it.replace("\r\n", "\n")) }
                else -> return
            }
            event.cancelPropagation()
            return
        }
        when (event.key) {
            KEY_ARROW_LEFT -> move(if (!shift && anchor != caret) ordered().first else if (word) wordBoundary(caret, -1) else left(caret), shift)
            KEY_ARROW_RIGHT -> move(if (!shift && anchor != caret) ordered().second else if (word) wordBoundary(caret, 1) else right(caret), shift)
            KEY_ARROW_UP -> { vertical(-1, shift); event.cancelPropagation(); return }
            KEY_ARROW_DOWN -> { vertical(1, shift); event.cancelPropagation(); return }
            KEY_PAGE_UP -> { vertical(-(layout.screenHeight / lineHeight).toInt(), shift); event.cancelPropagation(); return }
            KEY_PAGE_DOWN -> { vertical((layout.screenHeight / lineHeight).toInt(), shift); event.cancelPropagation(); return }
            KEY_HOME -> {
                // the first character that is not white space, then the start of the line
                val indent = lines[caret.line].takeWhile { it == ' ' || it == '\t' }.length
                move(Position(caret.line, if (caret.column == indent) 0 else indent), shift)
            }
            KEY_END -> move(Position(caret.line, lines[caret.line].length), shift)
            KEY_ENTER -> insert("\n" + lines[caret.line].takeWhile { it == ' ' || it == '\t' })
            KEY_TAB -> if (shift) indent(true) else if (anchor.line != caret.line) indent(false) else insert(" ".repeat(tabSize - caret.column % tabSize))
            KEY_BACKSPACE -> {
                if (anchor == caret) anchor = if (word) wordBoundary(caret, -1) else left(caret)
                insert("")
            }
            KEY_DELETE -> {
                if (anchor == caret) anchor = if (word) wordBoundary(caret, 1) else right(caret)
                insert("")
            }
            else -> return
        }
        preferredX = null
        event.cancelPropagation()
    }

    override fun draw(drawer: Drawer) {
        val color = computedStyle.effectiveColor ?: ColorRGBa.WHITE
        val text = panelText ?: return
        val face = text.face(computedStyle)
        val lh = lineHeight
        val w = layout.screenWidth
        val h = layout.screenHeight
        val g = gutter
        val first = floor(scroll.y / lh).toInt().coerceAtLeast(0)
        val last = minOf(lines.lastIndex, first + (h / lh).toInt() + 1)
        val focused = ElementPseudoClass("focus") in pseudoClasses || (root() as? Body)?.controlManager?.keyboardInput?.target === this
        val (a, b) = ordered()
        // the background and the border of the box, under the text
        val background = computedStyle.effectiveBackground ?: ColorRGBa.TRANSPARENT
        val border = computedStyle.effectiveBorderColor ?: ColorRGBa.TRANSPARENT
        if (background.alpha > 0.0 || (border.alpha > 0.0 && layout.borderWidth > 0.0)) drawer.isolated {
            drawer.fill = background
            drawer.stroke = border
            drawer.strokeWeight = layout.borderWidth
            drawer.strokeAlignment = StrokeAlignment.INSIDE
            drawer.box(layout.boundsAtOrigin, cornerRadius)
        }
        drawer.stroke = null
        // the text that scrolls is cut off at the numbers of the lines
        val outerClip = drawer.drawStyle.clip
        val textClip = Rectangle(layout.screenX + g - 4.0, layout.screenY, w - g + 4.0, h)
        drawer.drawStyle.clip = outerClip?.let { o -> intersection(o, textClip) } ?: textClip
        for (i in first..last) {
            val y = i * lh + 4.0 - scroll.y
            val c = carets(i)
            // the selection
            if (a != b && i in a.line..b.line) {
                val x0 = if (i == a.line) c[a.column] else 0.0
                val x1 = if (i == b.line) c[b.column] else c.last() + 6.0
                drawer.fill = ColorRGBa.fromHex("#5ab0ff").opacify(if (focused) 0.35 else 0.18)
                drawer.rectangle(g + x0 - scroll.x, y, x1 - x0, lh)
            }
            // the line, in the colors of the highlighter
            val line = lines[i].replace('\t', ' ')
            if (line.isNotEmpty()) {
                val shaped = text.line(computedStyle, line)
                val colors = highlighter?.let { hl ->
                    val byChar = MutableList(line.length) { color }
                    for (span in hl(line)) for (k in span.start until span.end.coerceAtMost(line.length)) byChar[k] = span.color
                    // a glyph has the color of the character where its pen starts, white space has no glyphs
                    shaped.glyphs.map { glyph ->
                        val x = glyph.position.x - glyph.offset.x
                        var k = c.binarySearch(x + 0.01).let { if (it >= 0) it else -it - 2 }
                        k = k.coerceIn(0, line.length - 1)
                        byChar[k]
                    }
                }
                text.draw(drawer, this, "line $i", shaped, Vector2(g - scroll.x, y + (lh + face.ascent + face.descent) / 2.0), color, glyphColors = colors)
            }
        }
        // the marks, underlined by zigzags; a mark of no width is as wide as a narrow character
        val markedLines = HashMap<Int, ColorRGBa>()
        for (mark in marks) {
            val from = positionOf(mark.start)
            val to = positionOf(maxOf(mark.start, mark.end))
            for (i in from.line..to.line) {
                markedLines.putIfAbsent(i, mark.color)
                if (i !in first..last) continue
                val c = carets(i)
                val x0 = if (i == from.line) c[from.column] else 0.0
                val x1 = maxOf(if (i == to.line) c[to.column] else c.last(), x0 + 6.0)
                val y = i * lh + 4.0 - scroll.y + lh - 2.5
                val zigzag = (0..((x1 - x0) / 2.0).toInt()).map { k -> Vector2(g + x0 + k * 2.0 - scroll.x, y + if (k % 2 == 0) 1.0 else -1.0) }
                drawer.stroke = mark.color
                drawer.strokeWeight = 1.0
                drawer.lineStrip(zigzag)
            }
        }
        drawer.stroke = null
        // the caret, and its line over the width of the text for input methods
        caretLine = null
        if (focused && caret.line in first..last) {
            val x = g + carets(caret.line)[caret.column] - scroll.x
            drawer.fill = color
            drawer.rectangle(x - 0.75, caret.line * lh + 4.0 - scroll.y + 2.0, 1.5, lh - 4.0)
            caretLine = Rectangle(g, caret.line * lh + 4.0 - scroll.y, w - g, lh) to (x - g)
        }
        drawer.drawStyle.clip = outerClip
        // the numbers of the lines
        if (lineNumbers) {
            drawer.fill = color.opacify(0.04)
            drawer.rectangle(0.0, 0.0, g - 6.0, h)
            for (i in first..last) {
                val y = i * lh + 4.0 - scroll.y
                text.drawInBox(drawer, this, "number $i", computedStyle, "${i + 1}", Rectangle(0.0, y, g - 12.0, lh), 1.0, 0.5,
                    markedLines[i] ?: color.opacify(if (i == caret.line) 0.7 else 0.35))
            }
        }
    }

    /** the line of the caret and the caret in it, in the coordinates of the element, as it was drawn last */
    private var caretLine: Pair<Rectangle, Double>? = null

    override val textInputArea: TextInputArea?
        get() = caretLine?.let { (line, cursor) -> TextInputArea(line.movedBy(Vector2(layout.screenX, layout.screenY)), cursor) }

    override fun close() {
        super.close()
        valueChanged.close()
    }
}

/** binds the text to [property], both ways, see [bindValue] */
fun TextArea.bind(property: KMutableProperty0<String>): PropertyBinding<String> =
    bindValue(valueChanged, property, { value }, { value = it })

fun Element.textArea(vararg classes: String, init: TextArea.() -> Unit): TextArea = initElement(classes, TextArea(), init)

private fun intersection(a: Rectangle, b: Rectangle): Rectangle {
    val x0 = maxOf(a.x, b.x)
    val y0 = maxOf(a.y, b.y)
    val x1 = minOf(a.x + a.width, b.x + b.width)
    val y1 = minOf(a.y + a.height, b.y + b.height)
    return Rectangle(x0, y0, maxOf(0.0, x1 - x0), maxOf(0.0, y1 - y0))
}
