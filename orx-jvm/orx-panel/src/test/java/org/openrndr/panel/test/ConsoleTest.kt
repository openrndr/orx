package org.openrndr.panel.test

import org.openrndr.panel.style.cursor
import org.openrndr.CharacterEvent
import org.openrndr.KEY_ARROW_DOWN
import org.openrndr.KEY_ARROW_RIGHT
import org.openrndr.KEY_ARROW_UP
import org.openrndr.KEY_ENTER
import org.openrndr.KEY_ESCAPE
import org.openrndr.KEY_TAB
import org.openrndr.color.ColorRGBa
import org.openrndr.panel.ControlManager
import org.openrndr.panel.elements.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ConsoleTest : PanelTestFixture() {
    private fun ControlManager.frames(n: Int = 2) = repeat(n) { afterDraw(program.drawer, program) }
    private fun ControlManager.type(text: String) = text.forEach { keyboardInput.character(CharacterEvent(it, emptySet())) }

    /** completes the words of [words] at the word at the caret */
    private fun words(vararg words: String) = Completer { text, caret ->
        val start = text.lastIndexOf(' ', caret - 1) + 1
        Completions(start, caret, words.map { Completion(it) })
    }

    @Test
    fun `an input completes the word at the caret`() {
        lateinit var input: TextInput
        val cm = panel {
            layout { input = textInput { completer = words("add", "alpha", "remove") } }
        }
        cm.frames()
        cm.keyboardInput.requestFocus(input)
        cm.type("a")
        assertTrue(input.completing)
        assertEquals("dd", input.ghost)
        // down chooses the next candidate, enter takes it once the list was used
        cm.key(KEY_ARROW_DOWN)
        assertEquals("lpha", input.ghost)
        val submitted = mutableListOf<String>()
        input.events.submitted.listen { submitted += it.newValue }
        cm.key(KEY_ENTER)
        assertEquals("alpha", input.value)
        assertFalse(input.completing)
        assertTrue(submitted.isEmpty())
        // enter without a list submits
        cm.key(KEY_ENTER)
        assertEquals(listOf("alpha"), submitted)

        // tab takes the only candidate
        input.value = ""
        cm.type("r")
        cm.key(KEY_TAB)
        assertEquals("remove", input.value)
        // right at the end takes the ghost
        input.value = ""
        cm.type("ad")
        cm.key(KEY_ARROW_RIGHT)
        assertEquals("add", input.value)
        // escape closes the list
        input.value = ""
        cm.type("a")
        cm.key(KEY_ESCAPE)
        assertFalse(input.completing)
        assertNull(input.ghost)
    }

    @Test
    fun `tab types the common beginning of the candidates`() {
        lateinit var input: TextInput
        val cm = panel {
            layout { input = textInput { completer = words("alpha", "alps", "beta") } }
        }
        cm.frames()
        cm.keyboardInput.requestFocus(input)
        cm.type("set a")
        cm.key(KEY_ESCAPE)
        cm.key(KEY_TAB)
        assertEquals("set alp", input.value)
        assertTrue(input.completing)
        assertEquals(7, input.caret)
    }

    @Test
    fun `keys are seen before the input handles them`() {
        lateinit var input: TextInput
        val cm = panel { layout { input = textInput { } } }
        cm.frames()
        cm.keyboardInput.requestFocus(input)
        cm.type("abc")
        input.events.keyPressed.listen { if (it.key == KEY_ARROW_UP) { input.value = "history"; it.cancelPropagation() } }
        cm.key(KEY_ARROW_UP)
        assertEquals("history", input.value)
        input.caret = 2
        assertEquals(2, input.caret)
        input.select(1, 4)
        assertEquals(1 until 4, input.selection)
        input.replace(0, 1, "H")
        assertEquals("History", input.value)
    }

    @Test
    fun `a console echoes commands, keeps a history and shows errors under their span`() {
        lateinit var console: Console
        val commands = mutableListOf<String>()
        val cm = panel {
            layout {
                console = console {
                    diagnose = { text -> if (text.startsWith("add")) listOf(TextMark(0, 3, ColorRGBa.RED)) else emptyList() }
                    events.submitted.listen { commands += it }
                }
            }
        }
        cm.frames()
        cm.keyboardInput.requestFocus(console.input)
        cm.type("add a")
        assertEquals(listOf(TextMark(0, 3, ColorRGBa.RED)), console.input.marks)
        cm.key(KEY_ENTER)
        cm.type("remove a")
        cm.key(KEY_ENTER)
        assertEquals(listOf("add a", "remove a"), commands)
        assertEquals("", console.input.value)
        // the echo has the colors of the command
        assertEquals(listOf(TextMark(0, 3, ColorRGBa.RED)), console.log.lines[0].marks)

        // up and down go through the history, back to what was typed
        cm.type("dra")
        cm.key(KEY_ARROW_UP)
        assertEquals("remove a", console.input.value)
        cm.key(KEY_ARROW_UP)
        assertEquals("add a", console.input.value)
        cm.key(KEY_ARROW_DOWN)
        cm.key(KEY_ARROW_DOWN)
        assertEquals("dra", console.input.value)

        // an error underlines its span in the echo and is shown under it
        cm.frames()
        console.error("expected a node", 7, 8)
        val echo = console.log.lines[1]
        assertEquals("remove a", echo.text)
        assertTrue(echo.marks.any { it.start == 7 && it.end == 8 && it.underline != null })
        assertEquals("^ expected a node", console.log.lines.last().text)
        assertTrue(console.log.lines.last().indent > 0.0)
    }

    @Test
    fun `text of the log is selected by dragging, clicking twice and three times, and copied`() {
        lateinit var console: Console
        val cm = panel { layout { console = console { } } }
        cm.frames()
        console.print("first line of text")
        console.submit("add blur#3 at 10 20")
        console.error("expected a number", 15, 17)
        cm.frames()
        val log = console.log
        // a drag from inside the first line to inside the echo
        cm.move(log.pointOf(LogPosition(0, 6))); cm.press(log.pointOf(LogPosition(0, 6)))
        cm.drag(log.pointOf(LogPosition(1, 3)))
        cm.release(log.pointOf(LogPosition(1, 3)))
        assertEquals("line of text\nadd", log.selectedText)
        assertTrue(cm.keyboardInput.target === log)
        cm.key(0, org.openrndr.KeyModifier.SUPER, name = "c")
        assertEquals("line of text\nadd", program.clipboard.contents)
        // shift extends it
        cm.press(log.pointOf(LogPosition(2, 2)), org.openrndr.MouseButton.LEFT, org.openrndr.KeyModifier.SHIFT)
        cm.release(log.pointOf(LogPosition(2, 2)))
        assertEquals("line of text\nadd blur#3 at 10 20\n^ ", log.selectedText)
        // a click without a drag clears it and gives the keyboard back to the input
        Thread.sleep(450)
        cm.press(log.pointOf(LogPosition(0, 2))); cm.release(log.pointOf(LogPosition(0, 2)))
        assertNull(log.selection)
        cm.frames()
        assertTrue(cm.keyboardInput.target === console.input)
        // two presses select a word, three the line
        Thread.sleep(450)
        val word = log.pointOf(LogPosition(1, 6))
        cm.press(word); cm.release(word); cm.press(word); cm.release(word)
        assertEquals("blur#3", log.selectedText)
        cm.press(word); cm.release(word)
        assertEquals("add blur#3 at 10 20", log.selectedText)
    }

    @Test
    fun `pasted lines are given as commands`() {
        lateinit var console: Console
        val commands = mutableListOf<String>()
        val cm = panel { layout { console = console { events.submitted.listen { commands += it } } } }
        cm.frames()
        cm.keyboardInput.requestFocus(console.input)
        cm.type("set ")
        program.clipboard.contents = "a 1\r\n\nadd b\nlay"
        cm.key(0, org.openrndr.KeyModifier.SUPER, name = "v")
        assertEquals(listOf("set a 1", "add b"), commands)
        assertEquals("lay", console.input.value)
        assertEquals(listOf("set a 1", "add b"), console.history)
        // without a selection mod+c copies the whole command
        cm.key(0, org.openrndr.KeyModifier.SUPER, name = "c")
        assertEquals("lay", program.clipboard.contents)
        // a single line input pastes line breaks as spaces
        console.pasteLines = false
        program.clipboard.contents = "out\nside"
        cm.key(0, org.openrndr.KeyModifier.SUPER, name = "v")
        assertEquals("layout side", console.input.value)
    }

    @Test
    fun `a console in a dock fits its panel after switching tabs`() {
        lateinit var dock: Dock
        lateinit var console: Console
        val cm = panel {
            css("""
                .page { display: flex; flex-direction: column; align-items: stretch; width: 100%; height: 100% }
                .page > dock { flex: 1; flex-basis: 0 }
                .bar { height: 30px; flex-shrink: 0 }
                dock-content.flush { padding: 0; overflow: hidden }
                .curves { display: flex; flex-direction: column; width: 100%; height: 100% }
                .curves > .tools { height: auto; flex-shrink: 0 }
                .curves > .editor { flex: 1; flex-basis: 0; width: 100% }
            """)
            layout {
                div("page") {
                    div("bar") { }
                    dock = dock {
                        val patch = panel("Patch") { classes.add(ElementClass("flush")); p { "patch" } }
                        val curves = panel("Curves", DockZone.BOTTOM, patch) {
                            classes.add(ElementClass("flush"))
                            div("curves") { div("tools") { p { "tools" } }; div("editor") { } }
                        }
                        panel("Console", DockZone.RIGHT, curves) {
                            classes.add(ElementClass("flush"))
                            console = console { }
                        }
                    }
                    div("bar") { }
                }
            }
        }
        fun bottom(e: Element) = e.layout.screenY + e.layout.screenHeight
        val curves = dock.panels.single { it.title == "Curves" }
        val panel = dock.panels.single { it.title == "Console" }
        cm.frames(3)
        // the console goes into the group of the curves, after the first layout
        dock.dock(panel, dock.groupOf(curves), DockZone.CENTER)
        cm.frames(3)
        assertTrue(bottom(dock) <= bottom(dock.parent!!) - 30.0 + 0.5, "the dock reaches over the bar under it: ${bottom(dock)}")
        cm.frames(3)
        val first = bottom(console.input)
        assertTrue(first <= bottom(panel.content) + 0.5, "${bottom(console.input)} > ${bottom(panel.content)}")
        dock.select(curves)
        cm.frames(3)
        dock.select(panel)
        cm.frames(3)
        assertEquals(first, bottom(console.input), 0.5)
        assertTrue(bottom(console.input) <= bottom(panel.content) + 0.5, "${bottom(console.input)} > ${bottom(panel.content)}")
    }

    @Test
    fun `a log keeps its start or its end in view, and its scrolling when its lines are replaced`() {
        lateinit var console: Console
        val cm = panel {
            css("console { width: 300px; height: 200px }")
            layout { console = console { } }
        }
        val log = console.log
        val rows = List(30) { ConsoleLine("row $it") }
        rows.forEach { log.add(it) }
        cm.frames()
        val n = log.visibleLines
        assertTrue(n in 3..20, "$n lines fit")
        val content = log.layout.contentBoundsAtOrigin
        val top = log.layout.screenY + content.y
        val bottom = top + content.height
        val h = log.pointOf(LogPosition(1, 0)).y - log.pointOf(LogPosition(0, 0)).y

        // the end in view: the last line at the bottom
        assertEquals(bottom - h / 2.0, log.pointOf(LogPosition(29, 0)).y, 1E-6)
        // scrolled back, which replacing the lines keeps, and clear does not
        log.scrolledBack = 5
        assertEquals(bottom - h / 2.0, log.pointOf(LogPosition(24, 0)).y, 1E-6)
        log.replaceAll(List(30) { ConsoleLine("new row $it") })
        assertEquals(5, log.scrolledBack)
        assertEquals("new row 0", log.lines.first().text)
        log.scrolledBack = 1000
        assertEquals(30 - n, log.scrolledBack)
        log.clear()
        assertEquals(0, log.scrolledBack)

        // the start in view: the first line at the top, also while lines are added under it
        log.anchor = LogAnchor.START
        rows.take(10).forEach { log.add(it) }
        assertEquals(top + h / 2.0, log.pointOf(LogPosition(0, 0)).y, 1E-6)
        rows.drop(10).forEach { log.add(it) }
        assertEquals(top + h / 2.0, log.pointOf(LogPosition(0, 0)).y, 1E-6)
        assertEquals(0, log.scrolledDown)
        // scrolled down, which replacing the lines keeps, measured from the start
        log.scrolledDown = 4
        assertEquals(top + h / 2.0, log.pointOf(LogPosition(4, 0)).y, 1E-6)
        log.replaceAll(rows.map { it.copy(text = "again " + it.text) })
        assertEquals(4, log.scrolledDown)
        assertEquals(top + h / 2.0, log.pointOf(LogPosition(4, 0)).y, 1E-6)
        // the wheel scrolls back up towards the start
        val p = log.pointOf(LogPosition(6, 0))
        cm.move(p)
        log.mouse.scrolled.trigger(org.openrndr.MouseEvent(p, org.openrndr.math.Vector2(0.0, 1.0), org.openrndr.math.Vector2.ZERO, org.openrndr.MouseEventType.SCROLLED, org.openrndr.MouseButton.NONE, emptySet()))
        assertEquals(1, log.scrolledDown)
        // to either end: the last line is the last whole line, under the lines that fit from the top
        log.scrollToEnd()
        val lastMiddle = log.pointOf(LogPosition(29, 0)).y
        assertTrue(lastMiddle + h / 2.0 <= bottom + 1E-6 && lastMiddle + h / 2.0 > bottom - h, "the last line at $lastMiddle")
        log.scrollToStart()
        assertEquals(top + h / 2.0, log.pointOf(LogPosition(0, 0)).y, 1E-6)
        // switching ends keeps the lines that show; from the end, the first of them is the first whole line
        log.scrolledDown = 7
        log.anchor = LogAnchor.END
        val firstTop = log.pointOf(LogPosition(7, 0)).y - h / 2.0
        assertTrue(firstTop >= top - 1E-6 && firstTop < top + h, "the first whole line at $firstTop")
    }

    @Test
    fun `a click on a line of a log tells which line it is, from either end`() {
        lateinit var console: Console
        val cm = panel {
            css("console { width: 300px; height: 200px }")
            layout { console = console { } }
        }
        val log = console.log
        log.clickableLines = true
        val clicks = mutableListOf<LogLineClicked>()
        log.lineClicked.listen { clicks.add(it) }
        log.replaceAll(List(30) { ConsoleLine("node $it", data = "node#$it") })
        cm.frames()
        val content = log.layout.contentBoundsAtOrigin
        val top = log.layout.screenY + content.y
        val bottom = top + content.height
        val h = log.pointOf(LogPosition(1, 0)).y - log.pointOf(LogPosition(0, 0)).y
        val x = log.layout.screenX + content.x + 20.0
        fun click(y: Double) {
            // more than a double click apart
            Thread.sleep(450)
            val p = org.openrndr.math.Vector2(x, y)
            cm.move(p); cm.press(p); cm.release(p)
        }

        // from the end, scrolled back 3 lines: the second line from the bottom
        log.scrolledBack = 3
        click(bottom - 1.5 * h)
        assertEquals(30 - 1 - 3 - 1, clicks.last().index)
        assertEquals("node#25", clicks.last().line.data)
        assertEquals(LogPosition(25, clicks.last().position.char), clicks.last().position)

        // from the start, scrolled down 4 lines: the second line from the top
        log.anchor = LogAnchor.START
        log.scrolledDown = 4
        click(top + 1.5 * h)
        assertEquals(5, clicks.last().index)
        assertEquals("node#5", clicks.last().line.data)
        assertEquals(2, clicks.size)

        // a drag selects and does not click
        Thread.sleep(450)
        val a = org.openrndr.math.Vector2(x, top + 2.5 * h)
        cm.move(a); cm.press(a); cm.drag(a + org.openrndr.math.Vector2(40.0, h)); cm.release(a + org.openrndr.math.Vector2(40.0, h))
        assertEquals(2, clicks.size)
        // the pointer is a hand over a log with clickable lines
        cm.frames()
        assertEquals(org.openrndr.panel.style.Cursor.Pointer, log.computedStyle.cursor)
    }

    @Test
    fun `a log wraps long lines into rows, which stay one line`() {
        lateinit var console: Console
        val cm = panel {
            css(".narrow { width: 240px; height: 300px } .wide { width: 480px; height: 300px }")
            layout { console = console("narrow") { } }
        }
        val log = console.log
        log.wrap = true
        val long = (1..30).joinToString(" ") { "word$it" }
        log.add(ConsoleLine("short", data = "first"))
        log.add(ConsoleLine(long, indent = 4.0, wrapIndent = 24.0, data = "long"))
        cm.frames()
        val content = log.layout.contentBoundsAtOrigin
        val left = log.layout.screenX + content.x
        val right = left + content.width
        val bottom = log.layout.screenY + content.y + content.height
        val h = log.pointOf(LogPosition(1, 0)).y - log.pointOf(LogPosition(0, 0)).y

        // the rows of the long line: their starts, at the starts of words
        fun rowStarts(line: Int, text: String): List<Int> = text.indices.filter { c ->
            c == 0 || log.pointOf(LogPosition(line, c)).y > log.pointOf(LogPosition(line, c - 1)).y + h / 2.0
        }
        val starts = rowStarts(1, long)
        assertTrue(starts.size >= 3, "rows at $starts")
        for (c in starts.drop(1)) assertEquals(' ', long[c - 1], "a row starts within a word at $c")
        // the first row at the indent, the others at the hanging indent, none past the right
        assertEquals(left + 4.0, log.pointOf(LogPosition(1, 0)).x, 1E-6)
        for (c in starts.drop(1)) assertEquals(left + 24.0, log.pointOf(LogPosition(1, c)).x, 1E-6)
        for (c in long.indices) assertTrue(log.pointOf(LogPosition(1, c)).x <= right + 1E-6, "character $c is past the right")
        // the end in view: the last row of the last line at the bottom
        assertEquals(bottom - h / 2.0, log.pointOf(LogPosition(1, long.length)).y, 1E-6)

        // a click on the second row is on the line
        val clicks = mutableListOf<LogLineClicked>()
        log.lineClicked.listen { clicks.add(it) }
        val second = log.pointOf(LogPosition(1, starts[1] + 2))
        cm.move(second); cm.press(second); cm.release(second)
        assertEquals(1, clicks.single().index)
        assertEquals("long", clicks.single().line.data)
        assertEquals(starts[1] + 2, clicks.single().position.char, "${clicks.single().position}")
        // the selection of the line is its text, without the breaks
        log.select(LogPosition(1, 0), LogPosition(1, long.length))
        assertEquals(long, log.selectedText)

        // wider, it is broken again, into fewer rows
        console.classes.remove(org.openrndr.panel.elements.ElementClass("narrow"))
        console.classes.add(org.openrndr.panel.elements.ElementClass("wide"))
        cm.frames(3)
        val wider = rowStarts(1, long)
        assertTrue(wider.size < starts.size, "${wider.size} rows, ${starts.size} before")

        // line breaks break rows, and a word that is wider than a row is broken within
        log.replaceAll(listOf(ConsoleLine("one\ntwo"), ConsoleLine("x".repeat(200))))
        cm.frames()
        assertTrue(log.pointOf(LogPosition(0, 4)).y > log.pointOf(LogPosition(0, 2)).y + h / 2.0)
        assertTrue(rowStarts(1, "x".repeat(200)).size > 1)
        // without wrapping, a line is a row
        log.wrap = false
        cm.frames()
        assertEquals(log.pointOf(LogPosition(1, 0)).y, log.pointOf(LogPosition(1, 199)).y, 1E-6)
    }
}
