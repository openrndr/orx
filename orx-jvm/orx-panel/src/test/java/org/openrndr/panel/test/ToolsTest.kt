package org.openrndr.panel.test

import org.openrndr.KEY_ARROW_DOWN
import org.openrndr.KEY_ARROW_LEFT
import org.openrndr.KEY_ARROW_RIGHT
import org.openrndr.KEY_ARROW_UP
import org.openrndr.KEY_ENTER
import org.openrndr.KEY_ESCAPE
import org.openrndr.KEY_BACKSPACE
import org.openrndr.KEY_DELETE
import org.openrndr.KEY_TAB
import org.openrndr.KeyEvent
import org.openrndr.KeyEventType
import org.openrndr.KeyModifier
import org.openrndr.color.ColorRGBa
import org.openrndr.math.Vector2
import org.openrndr.panel.Commands
import org.openrndr.panel.ControlManager
import org.openrndr.panel.Shortcut
import org.openrndr.panel.elements.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ToolsTest : PanelTestFixture() {
    private fun ControlManager.frames(n: Int = 2) = repeat(n) { afterDraw(program.drawer, program) }

    @Test
    fun `shortcuts run their commands and the search finds commands by their letters`() {
        val commands = Commands()
        var saved = 0
        commands.add("Save", "ctrl+s", "File") { saved++ }
        commands.add("Save as", "ctrl+shift+s", "File") {}
        commands.add("Select all", null, "Edit") {}
        assertEquals(Shortcut("s", setOf(KeyModifier.CTRL, KeyModifier.SHIFT)), Shortcut.parse("ctrl+shift+s"))
        assertTrue(commands.handle(KeyEvent(KeyEventType.KEY_DOWN, 0, "s", setOf(KeyModifier.CTRL))))
        assertEquals(1, saved)
        // shift is a different shortcut
        commands.handle(KeyEvent(KeyEventType.KEY_DOWN, 0, "s", setOf(KeyModifier.CTRL, KeyModifier.SHIFT)))
        assertEquals(1, saved)
        assertEquals("File: Save as", commands.search("save as").first().title)
        assertEquals("Edit: Select all", commands.search("sel all").first().title)
        assertTrue(commands.search("xyz").isEmpty())
    }

    @Test
    fun `the items of a menu bar become commands`() {
        var opened = 0
        val cm = panel {
            layout {
                menuBar {
                    menu("File") {
                        item("Open", "ctrl+o") { opened++ }
                        separator()
                        submenu("Recent") { item("a.txt") {} }
                    }
                }
            }
        }
        cm.frames()
        val titles = cm.commands.all.map { it.title }
        assertTrue("File: Open" in titles && "File: Recent: a.txt" in titles, "$titles")
        cm.commands.handle(KeyEvent(KeyEventType.KEY_DOWN, 0, "o", setOf(KeyModifier.CTRL)))
        assertEquals(1, opened)
    }

    @Test
    fun `rows of a list are reordered by dragging them`() {
        lateinit var list: VirtualList<String>
        val cm = panel {
            css("virtual-list { width: 200px; height: 200px }")
            layout { list = virtualList(listOf("a", "b", "c", "d"), 24.0) { item, _ -> p { item } } }
        }
        list.reorderable = true
        cm.frames()
        val moves = mutableListOf<Pair<Int, Int>>()
        list.reordered.listen { moves.add(it) }
        val x = list.layout.screenX + 50.0
        val y = list.layout.screenY
        cm.move(Vector2(x, y + 12.0))
        cm.press(Vector2(x, y + 12.0))
        cm.drag(Vector2(x, y + 40.0))
        cm.drag(Vector2(x, y + 74.0))
        assertTrue(cm.draggingAndDropping)
        cm.release(Vector2(x, y + 74.0))
        assertEquals(listOf(0 to 2), moves)
    }

    @Test
    fun `typed values are evaluated against the value and the range`() {
        assertEquals(6.0, evaluateInput("2*3", 0.0))
        assertEquals(5.0, evaluateInput("50%", 0.0, 0.0..10.0))
        assertEquals(3.0, evaluateInput("_ + 1", 2.0))
        assertEquals(Math.PI * 2, evaluateInput("tau", 0.0)!!, 1E-9)
        assertNull(evaluateInput("2 +", 0.0))
    }

    @Test
    fun `logarithmic sliders spread the decades evenly and snap to ticks`() {
        val slider = Slider()
        slider.range = Range(0.01, 100.0)
        slider.logarithmic = true
        assertEquals(1.0, slider.valueAt(0.5), 1E-9)
        assertEquals(0.75, slider.fraction(10.0), 1E-9)
        slider.logarithmic = false
        slider.range = Range(0.0, 10.0)
        slider.ticks = 4
        slider.snapToTicks = true
        assertEquals(7.5, slider.valueAt(0.7), 1E-9)
    }

    @Test
    fun `knobs turn with the keys and a double click sets their default`() {
        lateinit var knob: Knob
        val cm = panel { layout { knob = knob { range = 0.0..10.0; default = 5.0; value = 2.0 } } }
        cm.frames()
        val changes = mutableListOf<ValueChange<Double>>()
        knob.valueChanged.listen { changes.add(it) }
        cm.keyboardInput.target = knob
        cm.key(KEY_ARROW_UP)
        assertEquals(2.1, knob.value, 1E-9)
        knob.value = 20.0
        assertEquals(10.0, knob.value)
        assertTrue(changes[0].interactive && !changes[1].interactive)

        val range = RangeSlider()
        range.range = 0.0..1.0
        range.value = 0.8..0.2
        assertEquals(0.8..0.8, range.value)
    }

    @Test
    fun `curves are CSS timing functions and gradients interpolate between their stops`() {
        assertEquals("cubic-bezier(0.25, 0.1, 0.25, 1)", CubicBezier.EASE.toCss())
        assertEquals(0.5, CubicBezier.LINEAR.ease(0.5), 1E-6)
        val editor = GradientEditor()
        editor.value = listOf(ColorStop(0.0, ColorRGBa.BLACK), ColorStop(1.0, ColorRGBa.WHITE))
        editor.space = GradientSpace.SRGB
        assertEquals(0.5, editor.colorAt(0.5).r, 1E-6)
        editor.space = GradientSpace.LINEAR_RGB
        assertTrue(editor.colorAt(0.5).toSRGB().r > 0.7, "${editor.colorAt(0.5)}")
    }

    @Test
    fun `timelines interpolate their keyframes and set the properties of their tracks`() {
        var x = 0.0
        val property = object { var v: Double get() = x; set(value) { x = value } }
        lateinit var timeline: Timeline
        val cm = panel {
            css("timeline { width: 400px }")
            layout { timeline = timeline { duration = 4.0 } }
        }
        val track = timeline.track("x", property::v, 0.0..10.0)
        track.keyframes = listOf(Keyframe(3.0, 10.0), Keyframe(1.0, 0.0))
        assertEquals(listOf(1.0, 3.0), track.keyframes.map { it.time })
        assertEquals(0.0, track.valueAt(0.0))
        assertEquals(5.0, track.valueAt(2.0)!!, 1E-9)
        timeline.time = 2.5
        assertEquals(7.5, x, 1E-9)
        cm.frames()
        assertEquals(22.0 + 30.0, timeline.layout.screenHeight, 1E-9)
        // a press on the ruler moves the playhead
        val l = timeline.layout
        val laneX = l.screenX + 110.0
        val laneWidth = l.screenWidth - 110.0 - 8.0
        val p = Vector2(laneX + laneWidth * 0.25, l.screenY + 10.0)
        cm.move(p); cm.press(p); cm.release(p)
        assertEquals(1.0, timeline.time, 1E-6)
        // a keyframe is selected by a press on it, and deleted
        val k = Vector2(laneX + laneWidth * 0.75, l.screenY + 22.0 + 5.0)
        cm.move(k); cm.press(k); cm.release(k)
        cm.keyboardInput.target = timeline
        cm.key(KEY_DELETE)
        assertEquals(listOf(1.0), track.keyframes.map { it.time })
        // a double click in a track adds a keyframe of the value there, one on the ruler adds none
        Thread.sleep(600)
        val d = Vector2(laneX + laneWidth * 0.5, l.screenY + 22.0 + 15.0)
        repeat(2) { cm.move(d); cm.press(d); cm.release(d) }
        assertEquals(listOf(1.0, 2.0), track.keyframes.map { it.time })
        Thread.sleep(600)
        val r = Vector2(laneX + laneWidth * 0.75, l.screenY + 10.0)
        repeat(2) { cm.move(r); cm.press(r); cm.release(r) }
        assertEquals(2, track.keyframes.size)
    }

    @Test
    fun `panels dock into groups and next to them`() {
        lateinit var dock: Dock
        lateinit var a: DockPanel
        lateinit var b: DockPanel
        lateinit var c: DockPanel
        val cm = panel {
            css("dock { width: 600px; height: 400px }")
            layout {
                dock = dock {
                    a = panel("A") { p { "a" } }
                    b = panel("B") { p { "b" } }
                    c = panel("C", DockZone.RIGHT) { p { "c" } }
                }
            }
        }
        cm.frames()
        val root = dock.root as DockNode.Split
        assertEquals(listOf(listOf(a, b), listOf(c)), root.groups().map { it.panels })
        // the splitter starts in the middle
        val split = dock.children.single() as SplitPane
        assertEquals(300.0, split.position, 4.0)
        // dragging the tab of b to the bottom of the group of c
        cm.frames()
        val tab = dock.children.single().children.first().children.single().children.first().children
            .filterIsInstance<DockTab>().single { it.panel === b }
        val groupC = dock.children.single().children.last().children.single() as DockGroup
        val from = tab.screenArea.center
        val to = Vector2(groupC.layout.screenX + groupC.layout.screenWidth / 2.0, groupC.layout.screenY + groupC.layout.screenHeight - 10.0)
        cm.move(from); cm.press(from)
        cm.drag(from + Vector2(20.0, 0.0))
        cm.drag(to)
        assertEquals(DockZone.BOTTOM, groupC.zone)
        cm.release(to)
        cm.frames()
        val groups = dock.root!!.groups()
        assertEquals(listOf(listOf(a), listOf(c), listOf(b)), groups.map { it.panels })
        // moving a panel can be undone and redone
        assertEquals("Move B", cm.history.undoName)
        cm.history.undo()
        cm.frames()
        assertEquals(listOf(listOf(a, b), listOf(c)), dock.root!!.groups().map { it.panels })
        cm.history.redo()
        cm.frames()
        assertEquals(listOf(listOf(a), listOf(c), listOf(b)), dock.root!!.groups().map { it.panels })
        // the group of c is split above and below
        val right = (dock.root as DockNode.Split).second as DockNode.Split
        assertTrue(right.vertical)
        // a panel docked in the middle of a group becomes a tab, and the group it left goes
        dock.dock(b, dock.groupOf(a), DockZone.CENTER)
        cm.frames()
        assertEquals(listOf(listOf(a, b), listOf(c)), dock.root!!.groups().map { it.panels })
        assertSame(b, dock.groupOf(a)!!.selectedPanel)
        assertNotNull(b.content.parent)
    }

    @Test
    fun `the layout of a dock is saved and restored`() {
        fun build(dockInit: Dock.() -> Unit = {}): Pair<ControlManager, Dock> {
            lateinit var dock: Dock
            val cm = panel {
                css("dock { width: 600px; height: 400px }")
                layout {
                    dock = dock {
                        panel("A") { p { "a" } }
                        panel("B") { p { "b" } }
                        panel("C", DockZone.RIGHT) { p { "c" } }
                        dockInit()
                    }
                }
            }
            return cm to dock
        }
        val (cm, dock) = build()
        cm.frames(3)
        // b goes below c, the splitter of the root is dragged
        val b = dock.panels.single { it.title == "B" }
        val c = dock.panels.single { it.title == "C" }
        dock.dock(b, dock.groupOf(c), DockZone.BOTTOM)
        cm.frames(3)
        (dock.children.single() as SplitPane).position = 200.0
        cm.frames()
        val layout = dock.layout()
        val root = layout.root as DockLayoutNode.Split
        assertEquals(200.0, root.position, 1E-9)
        assertEquals(DockLayoutNode.Group(listOf("A")), root.first)
        val right = root.second as DockLayoutNode.Split
        assertTrue(right.vertical)
        assertEquals(listOf("C", "B"), listOf(right.first, right.second).map { (it as DockLayoutNode.Group).panels.single() })

        // a dock with the panels in their first places takes the layout from a file
        val file = java.io.File.createTempFile("dock", ".json")
        file.delete()
        dock.persist(file)
        Thread.sleep(600)
        cm.frames()
        assertTrue(file.exists())
        val (cm2, dock2) = build { persist(file) }
        cm2.frames(3)
        assertEquals(layout.root, dock2.layout().root)
        assertEquals(200.0, (dock2.children.single() as SplitPane).position, 1E-9)
        file.delete()
    }

    class State {
        var radius = 3.0
        var time = 0.0
        var curve = CubicBezier.EASE
    }

    @Test
    fun `bound controls follow their properties and set them`() {
        val state = State()
        lateinit var knob: Knob
        lateinit var field: NumberField
        lateinit var timeline: Timeline
        lateinit var curve: CurveEditor
        val cm = panel {
            layout {
                // bound while they are built, before they are in a document
                knob = knob { range = 0.0..10.0; bind(state::radius) }
                field = numberField { bind(state::radius) }
                timeline = timeline { duration = 4.0; bind(state::time) }
                curve = curveEditor { bind(state::curve) }
            }
        }
        assertEquals(3.0, knob.value)
        assertEquals(3.0, field.value)
        // the property changes, the controls follow in the next frame
        state.radius = 7.0
        state.time = 2.0
        state.curve = CubicBezier.LINEAR
        cm.frames(1)
        assertEquals(7.0, knob.value)
        assertEquals(7.0, field.value)
        assertEquals(2.0, timeline.time)
        assertEquals(CubicBezier.LINEAR, curve.value)
        // a control changes, the property and the other control follow
        cm.keyboardInput.target = knob
        cm.key(KEY_ARROW_UP)
        assertEquals(7.1, state.radius, 1E-9)
        cm.frames(1)
        assertEquals(7.1, field.value, 1E-9)
        // closed bindings let go
        knob.valueBindings.toList().forEach { it.close() }
        state.radius = 1.0
        cm.frames(1)
        assertEquals(7.1, knob.value, 1E-9)
        assertEquals(1.0, field.value)
    }

    @Test
    fun `edits by the user are undone and redone`() {
        lateinit var knob: Knob
        lateinit var list: VirtualList<String>
        val cm = panel {
            css("virtual-list { width: 200px; height: 120px }")
            layout {
                knob = knob { label = "radius"; range = 0.0..10.0; value = 5.0 }
                list = virtualList(listOf("a", "b", "c"), 24.0) { item, _ -> p { item } }
            }
        }
        list.reorderable = true
        list.reordered.listen { (from, to) -> list.items = list.items.toMutableList().apply { add(to, removeAt(from)) } }
        cm.frames()
        // keys pressed one after another are one edit, a value set by the program is none
        cm.keyboardInput.target = knob
        cm.key(KEY_ARROW_UP)
        cm.key(KEY_ARROW_UP)
        knob.value = 8.0
        assertEquals("Change radius", cm.history.undoName)
        cm.history.undo()
        assertEquals(5.0, knob.value, 1E-9)
        assertTrue(!cm.history.canUndo)
        cm.key(KEY_ARROW_UP)
        cm.key(KEY_ARROW_UP)
        // the shortcuts of the commands
        val mod = if (isMac) KeyModifier.SUPER else KeyModifier.CTRL
        cm.commands.handle(KeyEvent(KeyEventType.KEY_DOWN, 0, "z", setOf(mod)))
        assertEquals(5.0, knob.value, 1E-9)
        cm.commands.handle(KeyEvent(KeyEventType.KEY_DOWN, 0, "z", setOf(mod, KeyModifier.SHIFT)))
        assertEquals(5.2, knob.value, 1E-9)
        // a row moved by dragging moves back
        val x = list.layout.screenX + 50.0
        val y = list.layout.screenY
        cm.move(Vector2(x, y + 12.0)); cm.press(Vector2(x, y + 12.0))
        cm.drag(Vector2(x, y + 40.0)); cm.drag(Vector2(x, y + 74.0))
        cm.release(Vector2(x, y + 74.0))
        assertEquals(listOf("b", "c", "a"), list.items)
        cm.history.undo()
        assertEquals(listOf("a", "b", "c"), list.items)
        cm.history.redo()
        assertEquals(listOf("b", "c", "a"), list.items)
    }

    private val isMac = System.getProperty("os.name").lowercase().contains("mac")

    @Test
    fun `menus are used with the keyboard`() {
        val called = mutableListOf<String>()
        val cm = panel {
            layout {
                menuBar {
                    menu("File") {
                        item("Open") { called.add("open") }
                        separator()
                        submenu("Export") {
                            item("PNG") { called.add("png") }
                            item("SVG") { called.add("svg") }
                        }
                        item("Quit") { called.add("quit") }
                    }
                    menu("Edit") { item("Copy") { called.add("copy") } }
                }
            }
        }
        cm.frames()
        fun press(key: Int) = cm.keyboardInput.press(KeyEvent(KeyEventType.KEY_DOWN, key, "", emptySet()))
        // F10 opens the first menu at its first item
        cm.keyboardInput.press(KeyEvent(KeyEventType.KEY_DOWN, 0, "f10", emptySet()))
        cm.frames()
        assertEquals("Open", cm.currentMenu?.highlighted?.label)
        // down skips the separator, right opens the submenu at its first item
        press(KEY_ARROW_DOWN)
        assertEquals("Export", cm.currentMenu?.highlighted?.label)
        press(KEY_ARROW_RIGHT)
        cm.frames()
        assertEquals("PNG", cm.currentMenu?.innermost?.highlighted?.label)
        press(KEY_ARROW_DOWN)
        // left closes the submenu, escape closes the menu
        press(KEY_ARROW_LEFT)
        assertSame(cm.currentMenu, cm.currentMenu?.innermost)
        press(KEY_ARROW_RIGHT)
        press(KEY_ARROW_DOWN)
        press(KEY_ENTER)
        assertEquals(listOf("svg"), called)
        assertNull(cm.currentMenu)
        // right at the end of a menu goes to the next menu of the bar, typing moves to the item that starts with it
        cm.keyboardInput.press(KeyEvent(KeyEventType.KEY_DOWN, 0, "f10", emptySet()))
        cm.frames()
        cm.keyboardInput.character(org.openrndr.CharacterEvent('q', emptySet()))
        assertEquals("Quit", cm.currentMenu?.highlighted?.label)
        press(KEY_ARROW_RIGHT)
        cm.frames()
        assertEquals("Copy", cm.currentMenu?.highlighted?.label)
        press(KEY_ESCAPE)
        assertNull(cm.currentMenu)
    }

    @Test
    fun `nodes are moved, wired, selected and removed`() {
        lateinit var editor: NodeEditor
        lateinit var a: GraphNode
        lateinit var b: GraphNode
        lateinit var c: GraphNode
        val cm = panel {
            css("node-editor { width: 600px; height: 400px }")
            layout {
                editor = nodeEditor {
                    a = add(GraphNode("noise", outputs = listOf(Port("out", "image"))), Vector2(20.0, 20.0))
                    b = add(GraphNode("blur", listOf(Port("in", "image")), listOf(Port("out", "image"))), Vector2(250.0, 20.0))
                    c = add(GraphNode("count", outputs = listOf(Port("n", "number"))), Vector2(20.0, 200.0))
                }
            }
        }
        cm.frames()
        val o = Vector2(editor.layout.screenX, editor.layout.screenY)
        fun drag(from: Vector2, to: Vector2) {
            cm.move(o + from); cm.press(o + from)
            cm.drag(o + (from + to) / 2.0); cm.drag(o + to)
            cm.release(o + to)
        }
        // a wire from the output of a to the input of b
        drag(a.outputPosition(0), b.inputPosition(0))
        assertEquals(listOf(Wire(a, 0, b, 0)), editor.wires)
        // a number does not go into an image
        drag(c.outputPosition(0), b.inputPosition(0))
        assertEquals(listOf(Wire(a, 0, b, 0)), editor.wires)
        // the wire is dragged off the input
        drag(b.inputPosition(0), Vector2(400.0, 300.0))
        assertTrue(editor.wires.isEmpty())
        cm.history.undo()
        assertEquals(listOf(Wire(a, 0, b, 0)), editor.wires)
        // the title of a node moves it
        drag(b.position + Vector2(40.0, 10.0), b.position + Vector2(60.0, 110.0))
        assertEquals(Vector2(270.0, 120.0), b.position)
        cm.history.undo()
        assertEquals(Vector2(250.0, 20.0), b.position)
        // a box selects, delete removes the selected nodes and their wires, undo brings them back
        drag(Vector2(5.0, 5.0), Vector2(450.0, 100.0))
        assertEquals(setOf(a, b), editor.selection)
        cm.keyboardInput.target = editor
        cm.key(KEY_DELETE)
        assertEquals(listOf(c), editor.nodes)
        assertTrue(editor.wires.isEmpty())
        cm.history.undo()
        assertEquals(setOf(a, b, c), editor.nodes.toSet())
        assertEquals(listOf(Wire(a, 0, b, 0)), editor.wires)
        // the wheel zooms around the pointer
        val p = o + Vector2(300.0, 200.0)
        val before = editor.graphPosition(p)
        cm.mouseInput.scroll(org.openrndr.MouseEvent(p, Vector2(0.0, 2.0), Vector2.ZERO, org.openrndr.MouseEventType.SCROLLED, org.openrndr.MouseButton.NONE, emptySet()))
        assertTrue(editor.zoom > 1.0)
        assertEquals(before.x, editor.graphPosition(p).x, 1E-6)
        assertEquals(before.y, editor.graphPosition(p).y, 1E-6)
    }

    @Test
    fun `plots keep the last values and fit their range to them`() {
        assertEquals(0.5, niceStep(2.0, 4))
        assertEquals(20.0, niceStep(73.0, 4))
        lateinit var plot: Plot
        var t = 0.0
        val cm = panel {
            layout {
                plot = plot {
                    capacity = 4
                    watch("t") { t }
                }
            }
        }
        val s = plot.series.single()
        repeat(6) { t += 1.0; cm.frames(1) }
        assertEquals(4, s.size)
        assertEquals(listOf(3.0, 4.0, 5.0, 6.0), List(s.size) { s[it] })
        assertEquals(3.0..6.0, plot.shownRange())
        val histogram = Histogram().apply { bins = 4; range = 0.0..1.0; samples = doubleArrayOf(0.1, 0.2, 0.3, 0.9, 1.0) }
        assertEquals(listOf(2, 1, 0, 2), histogram.counts().toList())
    }

    @Test
    fun `colors are edited in OKLCH, typed as CSS and saved as swatches`() {
        lateinit var editor: ColorEditor
        val cm = panel { layout { editor = colorEditor { } } }
        cm.frames()
        editor.value = ColorRGBa.fromHex("#ff8800")
        assertEquals(ColorRGBa.fromHex("#ff8800").r, editor.value.r, 1E-3)
        assertEquals(ColorRGBa.fromHex("#ff8800").g, editor.value.g, 1E-3)
        // a CSS color typed in the field
        val input = editor.children.filterIsInstance<TextInput>().single()
        cm.keyboardInput.target = input
        input.value = ""
        input.insert("rgb(0 128 255)")
        assertEquals(128.0 / 255.0, editor.value.g, 1E-3)
        assertEquals("Change color", cm.history.undoName)
        // a gray keeps its hue
        editor.value = ColorRGBa.GRAY
        assertEquals(0.5, editor.value.r, 1E-3)
        // swatches are shared
        ColorEditor.swatches.clear()
        lateinit var other: ColorEditor
        val cm2 = panel { layout { other = colorEditor { } } }
        ColorEditor.addSwatch(ColorRGBa.RED)
        cm2.frames()
        assertEquals(1, other.children.last().children.filterIsInstance<ColorSwatch>().size)
        assertEquals(1, editor.children.last().children.filterIsInstance<ColorSwatch>().size)
    }

    @Test
    fun `rows slide to their new places`() {
        lateinit var list: VirtualList<String>
        val cm = panel {
            css("virtual-list { width: 200px; height: 200px }")
            layout { list = virtualList(listOf("a", "b", "c"), 24.0) { item, _ -> p { item } } }
        }
        cm.frames()
        list.items = listOf("c", "a", "b")
        cm.frames(1)
        val rowOf = { item: String -> list.children.filter { it.children.any { p -> (p.children.firstOrNull() as? TextNode)?.text == item } }.single() }
        val c = rowOf("c")
        // c was two rows lower, and starts there
        assertTrue(c.layoutOffset.y > 24.0, "${c.layoutOffset}")
        assertEquals(c.layout.screenY + c.layoutOffset.y, list.layout.screenY + 48.0, 1.0)
        Thread.sleep(300)
        cm.frames(1)
        assertEquals(Vector2.ZERO, c.layoutOffset)
    }

    @Test
    fun `text areas edit lines, indent, select by words and undo`() {
        lateinit var area: TextArea
        val cm = panel { layout { area = textArea { lineNumbers = true; highlighter = Highlighters.glsl } } }
        cm.frames()
        cm.keyboardInput.target = area
        fun type(text: String) = text.forEach { c ->
            if (c == '\n') cm.key(KEY_ENTER) else cm.keyboardInput.character(org.openrndr.CharacterEvent(c, emptySet()))
        }
        type("void main() {\n")
        // enter keeps the indentation
        cm.key(KEY_TAB)
        type("float x = 1.0;\nfloat y;")
        assertEquals("void main() {\n    float x = 1.0;\n    float y;", area.value)
        // shift-tab outdents the line, home goes to the start of the text of the line
        cm.key(KEY_TAB, KeyModifier.SHIFT)
        assertEquals("float y;", area.value.lines()[2])
        // alt-backspace deletes a word
        cm.key(KEY_BACKSPACE, KeyModifier.ALT)
        assertEquals("float ", area.value.lines()[2])
        // shift-up selects up to the line above
        cm.key(KEY_ARROW_UP, KeyModifier.SHIFT)
        assertTrue(area.selectedText.endsWith("float "), area.selectedText)
        // the typing is one edit, undone at once
        assertEquals("Edit text", cm.history.undoName)
        cm.history.undo()
        assertEquals("", area.value)
        // the highlighter
        val spans = Highlighters.glsl("vec3 c = vec3(1.0); // gray")
        assertTrue(spans.any { it.start == 0 && it.end == 4 && it.color == Highlighters.typeColor })
        assertTrue(spans.any { it.color == Highlighters.numberColor })
        assertTrue(spans.any { it.color == Highlighters.commentColor && it.end == 27 })
    }
}
