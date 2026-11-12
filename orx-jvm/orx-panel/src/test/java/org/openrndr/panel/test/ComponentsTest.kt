package org.openrndr.panel.test

import org.openrndr.KEY_ARROW_DOWN
import org.openrndr.KEY_ARROW_LEFT
import org.openrndr.KEY_ARROW_RIGHT
import org.openrndr.KEY_ENTER
import org.openrndr.KEY_ESCAPE
import org.openrndr.KEY_TAB
import org.openrndr.math.Vector2
import org.openrndr.panel.ControlManager
import org.openrndr.panel.elements.*
import kotlinx.coroutines.launch
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ComponentsTest : PanelTestFixture() {
    private fun ControlManager.frames(n: Int = 2) = repeat(n) { afterDraw(program.drawer, program) }

    private class Node(val name: String, val children: List<Node> = emptyList())

    @Test
    fun `rich text wraps its spans together and finds the span under the pointer`() {
        lateinit var rich: RichText
        lateinit var link: Span
        val cm = panel {
            css("""
                rich-text { width: 200px; font-size: 16px }
                span.big { font-size: 28px }
                span.link { color: #5ab0ff; text-decoration: underline }
            """)
            layout {
                rich = richText {
                    span { "Some text with " }
                    span("big") { "large" }
                    span { " words and " }
                    link = span("link") { "a link" }
                    span { " that wraps over several lines in the box." }
                }
            }
        }
        cm.frames()
        // several lines, as tall as their line height
        assertTrue(rich.layout.screenHeight > 60.0, "${rich.layout.screenHeight}")
        var clicks = 0
        link.mouse.clicked.listen { clicks++ }
        // the glyphs of the link
        val text = cm.text
        val shaped = text.richParagraph(rich.spans.map { org.openrndr.panel.PanelText.Run(it.text, it.computedStyle) }, rich.layout.contentBounds.width)
        val i = shaped.runOfGlyph.indexOfFirst { it == 3 }
        val glyph = shaped.shaped.glyphs[i]
        val p = Vector2(rich.layout.contentBounds.x + glyph.position.x + 2.0, rich.layout.contentBounds.y + shaped.shaped.ascent + glyph.position.y - 4.0)
        assertSame(link, rich.spanAt(p))
        cm.move(p)
        cm.press(p)
        cm.release(p)
        assertEquals(1, clicks)
    }

    @Test
    fun `a tree expands and collapses by keyboard and pointer`() {
        val leaf = Node("leaf")
        val branch = Node("branch", listOf(leaf, Node("other")))
        val root = Node("root", listOf(branch))
        lateinit var tree: TreeView<Node>
        val cm = panel {
            css("tree-view { height: 200px; width: 200px }")
            layout { tree = treeView(listOf(root), { it.children }, { it.name }) }
        }
        cm.frames()
        val selected = mutableListOf<String>()
        tree.events.selected.listen { selected.add(it.name) }
        cm.keyboardInput.target = tree
        tree.selection = root
        cm.key(KEY_ARROW_RIGHT)
        assertTrue(root in tree.expanded)
        cm.key(KEY_ARROW_RIGHT)
        assertSame(branch, tree.selection)
        cm.key(KEY_ARROW_RIGHT)
        cm.key(KEY_ARROW_DOWN)
        assertSame(leaf, tree.selection)
        cm.key(KEY_ARROW_LEFT)
        assertSame(branch, tree.selection)
        cm.key(KEY_ARROW_LEFT)
        assertFalse(branch in tree.expanded)
        assertEquals(listOf("root", "branch", "leaf", "branch"), selected)
        cm.frames()
        // the rows in view
        val rows = tree.children.first().children.flatMap { it.children }.filterIsInstance<TreeRow<Node>>()
        assertEquals(listOf("root", "branch"), rows.map { it.item.name })
        assertEquals("1", rows[1].attribute("data-depth"))
    }

    @Test
    fun `a table sorts by its columns and resizes them`() {
        data class Item(val name: String, val size: Int)
        val items = listOf(Item("b", 2), Item("a", 3), Item("c", 1))
        val size = Column<Item>("size", 80.0, { it.size })
        lateinit var table: DataTable<Item>
        val cm = panel {
            css("data-table { width: 300px; height: 200px }")
            layout { table = dataTable(items, listOf(Column("name", 100.0, { it.name }), size)) }
        }
        cm.frames()
        table.cycleSort(size)
        assertEquals(listOf(1, 2, 3), table.shown.map { it.size })
        table.cycleSort(size)
        assertEquals(listOf(3, 2, 1), table.shown.map { it.size })
        table.cycleSort(size)
        assertEquals(items, table.shown)
        table.resize(size, 120.0)
        cm.frames()
        val cells = table.children.first().children.filterIsInstance<DataHeaderCell<Item>>()
        assertEquals(120.0, cells[1].layout.screenWidth, 1E-9)
        // a click on a row selects it
        val row = table.children[1].children.flatMap { it.children }.filterIsInstance<DataRow<Item>>().first()
        val p = row.screenArea.center
        cm.move(p); cm.press(p); cm.release(p)
        assertSame(items[0], table.selection)
    }

    @Test
    fun `a split pane, a dialog that keeps the focus and toasts that go away`() {
        lateinit var split: SplitPane
        lateinit var outside: Button
        val cm = panel {
            layout {
                outside = button { label = "outside" }
                split = splitPane { first { button { label = "a" } }; second { button { label = "b" } } }
            }
        }
        cm.frames()
        split.position = 150.0
        cm.frames()
        assertEquals(150.0, split.first.layout.screenWidth, 1E-9)

        lateinit var ok: Button
        lateinit var cancel: Button
        val dialog = cm.dialog { ok = button { label = "OK" }; cancel = button { label = "Cancel" } }
        cm.frames()
        assertSame(ok, cm.keyboardInput.target)
        cm.key(KEY_TAB)
        assertSame(cancel, cm.keyboardInput.target)
        cm.key(KEY_TAB)
        assertSame(ok, cm.keyboardInput.target)
        var closed = 0
        dialog.closed.listen { closed++ }
        cm.key(KEY_ESCAPE)
        assertFalse(dialog.open)
        assertEquals(1, closed)
        assertNull(dialog.backdrop.parent)

        val toast = cm.toast("saved", duration = 0.1)
        cm.frames()
        assertTrue(toast.parent != null)
        Thread.sleep(500)
        cm.frames(3)
        assertNull(toast.parent)
    }

    @Test
    fun `a confirmation answers once, by its buttons, the keyboard and the backdrop`() {
        val cm = panel { layout { button { label = "outside" } } }
        cm.frames()
        val answers = mutableListOf<Boolean>()
        fun ask(destructive: Boolean = false) = cm.confirm(
            "Discard the changes?", title = "Discard", confirmLabel = "Discard", destructive = destructive
        ) { answers.add(it) }
        fun Dialog.button(name: String): Button {
            var found: Button? = null
            fun visit(e: Element) { if (e is Button && ElementClass(name) in e.classes) found = e else e.children.forEach(::visit) }
            visit(box)
            return found!!
        }
        fun click(button: Button) {
            val p = button.screenArea.center
            cm.move(p); cm.press(p); cm.release(p)
        }

        // the title, the message and the buttons, with the labels that are given
        val first = ask()
        cm.frames()
        assertEquals("Discard", ((first.box.children.first() as H3).children.single() as TextNode).text)
        assertEquals("Discard", first.button("confirm-button").label)
        assertEquals("Cancel", first.button("cancel-button").label)
        // the confirming button has the focus
        assertSame(first.button("confirm-button"), cm.keyboardInput.target)
        click(first.button("confirm-button"))
        assertFalse(first.open)
        assertNull(first.backdrop.parent)
        assertEquals(listOf(true), answers)

        val second = ask()
        cm.frames()
        click(second.button("cancel-button"))
        assertEquals(listOf(true, false), answers)

        // escape cancels, enter presses the button with the focus
        ask()
        cm.frames()
        cm.key(KEY_ESCAPE)
        ask()
        cm.frames()
        cm.key(KEY_ENTER)
        val third = ask()
        cm.frames()
        cm.key(KEY_TAB)
        assertSame(third.button("cancel-button"), cm.keyboardInput.target)
        cm.key(KEY_ENTER)
        assertEquals(listOf(true, false, false, true, false), answers)

        // a destructive action starts on cancel, and a click on the backdrop cancels
        val destructive = ask(destructive = true)
        cm.frames()
        assertTrue(ElementClass("destructive") in destructive.button("confirm-button").classes)
        assertSame(destructive.button("cancel-button"), cm.keyboardInput.target)
        cm.key(KEY_ENTER)
        val last = ask()
        cm.frames()
        val corner = Vector2(5.0, 5.0)
        cm.move(corner); cm.press(corner); cm.release(corner)
        assertFalse(last.open)
        // closing it from the program cancels it, and an answered dialog answers once
        val closedByProgram = ask()
        closedByProgram.close()
        closedByProgram.close()
        last.close()
        assertEquals(listOf(true, false, false, true, false, false, false, false), answers)
    }

    @Test
    fun `a suspending confirmation returns the answer and closes when it is cancelled`() {
        val cm = panel { layout { button { label = "outside" } } }
        cm.frames()
        fun backdrops() = cm.body!!.children.count { ElementClass("dialog-backdrop") in it.classes }
        val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Unconfined)

        val answers = mutableListOf<Boolean>()
        scope.launch {
            answers.add(cm.confirm("Save before closing?"))
            answers.add(cm.confirm("Discard the changes?", destructive = true))
        }
        // the coroutine waits for the first answer, enter confirms it
        assertEquals(1, backdrops())
        assertTrue(answers.isEmpty())
        cm.key(KEY_ENTER)
        assertEquals(listOf(true), answers)
        // then for the second, which starts on cancel
        assertEquals(1, backdrops())
        cm.key(KEY_ENTER)
        assertEquals(listOf(true, false), answers)
        assertEquals(0, backdrops())

        // cancelling the coroutine closes the dialog, without an answer
        val job = scope.launch { answers.add(cm.confirm("Quit?")) }
        assertEquals(1, backdrops())
        job.cancel()
        assertEquals(0, backdrops())
        assertEquals(listOf(true, false), answers)
    }
}
