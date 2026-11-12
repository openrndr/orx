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
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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

    @Test
    fun `a panel that a restored layout has in a window of its own is not lost`() {
        lateinit var dock: Dock
        val saved = DockLayout(DockLayoutNode.Group(listOf("A", "C")), listOf(FloatingPanel("B", 900.0, 120.0, 320.0, 240.0)))
        val cm = panel {
            css("dock { width: 600px; height: 400px }")
            layout {
                dock = dock {
                    panel("A") { p { "a" } }
                    panel("B") { p { "b" } }
                    panel("C") { p { "c" } }
                    // restored before the dock is in a control manager, like by persist
                    restore(saved)
                }
            }
        }
        // until its window is made it is a panel of the dock, and a layout that is saved keeps it in its window
        assertTrue(dock.panels.any { it.title == "B" })
        assertEquals(listOf(saved.floating.single()), dock.layout().floating)
        cm.frames(3)
        // then it is in a window of its own, or docked when the window can not be made
        val b = dock.panels.single { it.title == "B" }
        val layout = dock.layout()
        if (b.window != null) {
            assertEquals(listOf("B"), layout.floating.map { it.title })
            dock.redock(b)
        } else {
            assertTrue("B" in (layout.root as DockLayoutNode.Group).panels)
        }
    }

    @Test
    fun `closable panels close and open again where they were`() {
        lateinit var dock: Dock
        lateinit var a: DockPanel
        lateinit var b: DockPanel
        lateinit var c: DockPanel
        lateinit var d: DockPanel
        val cm = panel {
            css("dock { width: 600px; height: 400px }")
            layout {
                dock = dock {
                    a = panel("A") { p { "a" } }
                    b = panel("B", closable = true) { p { "b" } }
                    c = panel("C", DockZone.RIGHT, closable = true) { p { "c" } }
                    d = panel("D", DockZone.BOTTOM, c) { p { "d" } }
                }
            }
        }
        val events = mutableListOf<String>()
        dock.panelClosed.listen { events.add("closed ${it.title}") }
        dock.panelOpened.listen { events.add("opened ${it.title}") }
        cm.frames()
        fun groups() = dock.root!!.groups().map { it.panels }
        assertEquals(listOf(listOf(a, b), listOf(c), listOf(d)), groups())

        // a panel that is not closable stays
        dock.close(a)
        assertTrue(dock.isOpen(a))
        assertEquals(listOf(listOf(a, b), listOf(c), listOf(d)), groups())

        // a closed panel is out of the layout and not drawn, but kept with its content
        val content = b.content.children.toList()
        dock.close(b)
        cm.frames()
        assertFalse(dock.isOpen(b))
        assertEquals(listOf(b), dock.closedPanels)
        assertTrue(b in dock.panels)
        assertEquals(listOf(listOf(a), listOf(c), listOf(d)), groups())
        assertNull(b.content.parent)
        assertEquals(content, b.content.children.toList())
        // and opens in its group, where it was, showing
        dock.open(b)
        cm.frames()
        assertEquals(listOf(listOf(a, b), listOf(c), listOf(d)), groups())
        assertSame(b, dock.groupOf(a)!!.selectedPanel)
        assertNotNull(b.content.parent)

        // a panel alone in its group: the group goes, and comes back on the same side of its neighbour
        dock.close(c)
        cm.frames()
        assertEquals(listOf(listOf(a, b), listOf(d)), groups())
        dock.open(c)
        cm.frames()
        assertEquals(listOf(listOf(a, b), listOf(c), listOf(d)), groups())
        val right = (dock.root as DockNode.Split).second as DockNode.Split
        assertTrue(right.vertical)
        assertEquals(listOf(c), (right.first as DockNode.Group).panels)
        assertEquals(listOf("closed B", "opened B", "closed C", "opened C"), events)

        // the × on the tab closes it, a click elsewhere on the tab selects it
        fun tab(panel: DockPanel): DockTab {
            var found: DockTab? = null
            fun visit(e: Element) { if (e is DockTab && e.panel === panel) found = e else e.children.forEach(::visit) }
            visit(dock)
            return found!!
        }
        dock.select(a)
        cm.frames()
        val tabB = tab(b)
        assertTrue(tabB.layout.screenWidth > tab(a).layout.screenWidth, "the tab of a closable panel has room for its ×")
        val middle = Vector2(tabB.layout.screenX + 14.0, tabB.layout.screenY + tabB.layout.screenHeight / 2.0)
        // clicks more than the time of a double click apart
        Thread.sleep(600)
        cm.move(middle); cm.press(middle); cm.release(middle)
        cm.frames()
        assertTrue(dock.isOpen(b))
        assertSame(b, dock.groupOf(a)!!.selectedPanel)
        val cross = Vector2(tabB.layout.screenX + tabB.layout.screenWidth - 13.0, middle.y)
        Thread.sleep(600)
        cm.move(cross); cm.press(cross); cm.release(cross)
        cm.frames()
        assertFalse(dock.isOpen(b))
        dock.open(b)
        cm.frames()

        // mod+w closes the panel with the keyboard focus, when it can be closed
        val mod = Shortcut.parse("mod+w")!!.modifiers.single()
        cm.keyboardInput.target = a.content.children.first()
        cm.key(0, mod, name = "w")
        assertTrue(dock.isOpen(a))
        cm.keyboardInput.target = b.content.children.first()
        cm.key(0, mod, name = "w")
        assertFalse(dock.isOpen(b))
        assertNull(cm.keyboardInput.target, "the focus leaves the closed panel")
        dock.open(b)
        cm.frames()
        assertTrue(dock.closedPanels.isEmpty())
    }

    @Test
    fun `panels that share a group open into it again`() {
        lateinit var dock: Dock
        lateinit var a: DockPanel
        lateinit var b: DockPanel
        lateinit var e: DockPanel
        val cm = panel {
            css("dock { width: 600px; height: 400px }")
            layout {
                dock = dock {
                    a = panel("A") { p { "a" } }
                    b = panel("B", DockZone.RIGHT, closable = true) { p { "b" } }
                    e = panel("E", DockZone.CENTER, b, closable = true) { p { "e" } }
                }
            }
        }
        cm.frames()
        fun groups() = dock.root!!.groups().map { it.panels }
        assertEquals(listOf(listOf(a), listOf(b, e)), groups())
        dock.close(b)
        dock.close(e)
        cm.frames()
        assertEquals(listOf(listOf(a)), groups())
        // the first back is right of a again, the other joins it
        dock.open(b)
        cm.frames()
        assertEquals(listOf(listOf(a), listOf(b)), groups())
        dock.open(e)
        cm.frames()
        assertEquals(listOf(listOf(a), listOf(b, e)), groups())
        // either way round
        dock.close(e)
        dock.close(b)
        dock.open(e)
        dock.open(b)
        cm.frames()
        assertEquals(listOf(listOf(a), listOf(b, e)), groups())
    }

    @Test
    fun `closed panels stay closed in a saved layout`() {
        fun build(dockInit: Dock.() -> Unit = {}): Pair<ControlManager, Dock> {
            lateinit var dock: Dock
            val cm = panel {
                css("dock { width: 600px; height: 400px }")
                layout {
                    dock = dock {
                        panel("A") { p { "a" } }
                        panel("B", closable = true) { p { "b" } }
                        panel("C", DockZone.RIGHT, closable = true) { p { "c" } }
                        dockInit()
                    }
                }
            }
            return cm to dock
        }
        val (cm, dock) = build()
        cm.frames()
        dock.close(dock.panels.single { it.title == "C" })
        cm.frames()
        val layout = dock.layout()
        assertEquals(listOf("C"), layout.closed)

        val file = java.io.File.createTempFile("dock", ".json")
        file.delete()
        dock.persist(file)
        Thread.sleep(600)
        cm.frames()
        val (cm2, dock2) = build { persist(file) }
        cm2.frames()
        val c2 = dock2.panels.single { it.title == "C" }
        assertFalse(dock2.isOpen(c2))
        assertEquals(layout, dock2.layout())
        // and it opens where it was added
        dock2.open(c2)
        cm2.frames()
        assertEquals(listOf(listOf("A", "B"), listOf("C")), dock2.root!!.groups().map { g -> g.panels.map { it.title } })

        // a layout from before panels could be closed
        file.writeText("""{ "root": { "type": "group", "panels": [ "B", "A" ], "selected": 1 } }""")
        val (cm3, dock3) = build { persist(file) }
        cm3.frames()
        assertEquals(listOf(listOf("B", "A", "C")), dock3.root!!.groups().map { g -> g.panels.map { it.title } })
        assertTrue(dock3.closedPanels.isEmpty())
        file.delete()

        // a panel that the layout has closed but that can not be closed is docked
        val (cm4, dock4) = build { restore(DockLayout(DockLayoutNode.Group(listOf("B", "C")), closed = listOf("A"))) }
        cm4.frames()
        assertTrue(dock4.closedPanels.isEmpty())
    }

    @Test
    fun `a panel in a window of its own is dragged back into a zone of the dock`() {
        lateinit var dock: Dock
        val cm = panel {
            css("dock { width: 600px; height: 400px }")
            layout {
                dock = dock {
                    panel("A") { p { "a" } }
                    panel("B") { p { "b" } }
                    panel("C", DockZone.RIGHT) { p { "c" } }
                }
            }
        }
        cm.frames(3)
        val b = dock.panels.single { it.title == "B" }
        dock.float(b)
        cm.frames(2)
        // without windows in this environment there is nothing to drag back
        if (b.window == null) return
        // over the right part of the group of C the zone shows, releasing docks B to the right of C
        val c = dock.panels.single { it.title == "C" }
        fun groupElement(e: Element): DockGroup? = if (e is DockGroup && c in e.group.panels) e else e.children.firstNotNullOfOrNull(::groupElement)
        val group = groupElement(dock)!!
        val point = program.window.position + Vector2(group.layout.screenX + group.layout.screenWidth * 0.95, group.layout.screenY + group.layout.screenHeight / 2.0)
        dock.windowDragged(b, point)
        assertEquals(DockZone.RIGHT, group.zone)
        dock.windowReleased(b, point)
        assertNull(group.zone)
        cm.frames(3)
        assertNull(b.window)
        val root = dock.layout().root as DockLayoutNode.Split
        val right = root.second as DockLayoutNode.Split
        assertEquals(listOf("C", "B"), listOf(right.first, right.second).map { (it as DockLayoutNode.Group).panels.single() })

        // released outside the dock it stays in its window, a double click on the title puts it back in the last group
        dock.float(b)
        cm.frames(2)
        dock.windowReleased(b, program.window.position + Vector2(-500.0, -500.0))
        cm.frames(2)
        assertNotNull(b.window)
        dock.dockFromWindow(b)
        cm.frames(2)
        assertNull(b.window)
        assertTrue(dock.groupOf(b) != null)
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
    fun `menus are as wide as their labels and shortcuts`() {
        val cm = panel {
            layout {
                menuBar {
                    menu("Layout") {
                        item("Arrange selection with a long label", "shift+mod+l") { }
                        item("Short") { }
                    }
                    menu("Edit") { item("Copy") { } }
                }
            }
        }
        cm.frames()
        cm.keyboardInput.press(KeyEvent(KeyEventType.KEY_DOWN, 0, "f10", emptySet()))
        cm.frames()
        val menu = cm.currentMenu!!
        val item = menu.highlighted!!
        val needed = item.panelText!!.width(item.computedStyle, item.label) + 24.0 +
                item.panelText!!.width(item.computedStyle, org.openrndr.panel.Shortcut.parse("shift+mod+l").toString())
        assertTrue(item.layout.screenWidth >= needed, "${item.layout.screenWidth} < $needed")
        // a menu of short items keeps its smallest width
        cm.keyboardInput.press(KeyEvent(KeyEventType.KEY_DOWN, KEY_ARROW_RIGHT, "", emptySet()))
        cm.frames()
        assertEquals(180.0, cm.currentMenu!!.layout.screenWidth, 0.5)
    }

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
    fun `every change of the nodes and wires fires an event, also undo and redo`() {
        lateinit var editor: NodeEditor
        lateinit var a: GraphNode
        lateinit var b: GraphNode
        val log = mutableListOf<String>()
        val cm = panel {
            css("node-editor { width: 600px; height: 400px }")
            layout {
                editor = nodeEditor {
                    events.added.listen { log += "added ${it.map { n -> n.title }}" }
                    events.removed.listen { log += "removed ${it.map { n -> n.title }}" }
                    events.moved.listen { log += "moved ${it.map { n -> n.title }}" }
                    events.connected.listen { log += "connected ${it.from.title}-${it.to.title}" }
                    events.disconnected.listen { log += "disconnected ${it.from.title}-${it.to.title}" }
                    a = add(GraphNode("a", outputs = listOf(Port("out", "image"))), Vector2(20.0, 20.0))
                    b = add(GraphNode("b", listOf(Port("in", "image"))), Vector2(250.0, 20.0))
                    connect(a, 0, b, 0)
                }
            }
        }
        cm.frames()
        assertEquals(listOf("added [a]", "added [b]", "connected a-b"), log)
        val o = Vector2(editor.layout.screenX, editor.layout.screenY)
        fun drag(from: Vector2, to: Vector2) {
            cm.move(o + from); cm.press(o + from)
            cm.drag(o + (from + to) / 2.0); cm.drag(o + to)
            cm.release(o + to)
        }
        log.clear()
        drag(b.position + Vector2(40.0, 10.0), b.position + Vector2(60.0, 110.0))
        cm.history.undo()
        cm.history.redo()
        assertEquals(listOf("moved [b]", "moved [b]", "moved [b]"), log)
        log.clear()
        cm.keyboardInput.target = editor
        cm.key(KEY_DELETE)
        assertEquals(listOf("disconnected a-b", "removed [b]"), log)
        // a node the host adds after the removal stays when the removal is undone
        val c = editor.add(GraphNode("c"), Vector2(20.0, 300.0))
        log.clear()
        cm.history.undo()
        assertEquals(listOf("added [b]", "connected a-b"), log)
        assertEquals(listOf(a, b, c), editor.nodes)
        log.clear()
        cm.history.redo()
        assertEquals(listOf("disconnected a-b", "removed [b]"), log)
        assertEquals(listOf(a, c), editor.nodes)
    }

    @Test
    fun `the graph of a node editor is saved and restored`() {
        lateinit var editor: NodeEditor
        val cm = panel {
            layout {
                editor = nodeEditor {
                    val a = add(GraphNode("a", outputs = listOf(Port("out", "image", ColorRGBa.RED)), data = "1"), Vector2(20.0, 20.0))
                    val b = add(GraphNode("b", listOf(Port("in", "image")), data = "2"), Vector2(250.0, 20.0))
                    b.width = 200.0
                    connect(a, 0, b, 0)
                }
            }
        }
        cm.frames()
        val model = editor.graph()
        val text = kotlinx.serialization.json.Json.encodeToString(NodeGraphModel.serializer(), model)
        val back = kotlinx.serialization.json.Json.decodeFromString(NodeGraphModel.serializer(), text)
        assertEquals(model, back)
        val removed = mutableListOf<GraphNode>()
        editor.events.removed.listen { removed += it }
        val nodes = editor.restore(back)
        assertEquals(2, removed.size)
        assertEquals(listOf("a", "b"), editor.nodes.map { it.title })
        assertEquals(listOf("1", "2"), nodes.map { it?.data })
        assertEquals(200.0, nodes[1]!!.width)
        assertEquals(ColorRGBa.RED, nodes[0]!!.outputs[0].color)
        assertEquals(listOf(Wire(nodes[0]!!, 0, nodes[1]!!, 0)), editor.wires)
    }

    @Test
    fun `the body of a node takes the pointer when it handles it`() {
        lateinit var editor: NodeEditor
        lateinit var n: GraphNode
        val events = mutableListOf<org.openrndr.MouseEventType>()
        var value = 0.0
        val cm = panel {
            css("node-editor { width: 600px; height: 400px }")
            layout {
                editor = nodeEditor {
                    n = add(GraphNode("slider", listOf(Port("in"))), Vector2(20.0, 20.0))
                    n.bodyHeight = 30.0
                    n.body = { _, _ -> }
                    n.bodyPointer = { e -> events += e.type; value = e.fraction.x; true }
                }
            }
        }
        cm.frames()
        val o = Vector2(editor.layout.screenX, editor.layout.screenY)
        val box = n.bodyBounds
        val from = Vector2(box.x + box.width * 0.25, box.center.y)
        val to = Vector2(box.x + box.width * 0.75, box.center.y)
        cm.move(o + from); cm.press(o + from); cm.drag(o + to); cm.release(o + to)
        assertEquals(listOf(org.openrndr.MouseEventType.BUTTON_DOWN, org.openrndr.MouseEventType.DRAGGED, org.openrndr.MouseEventType.BUTTON_UP), events)
        assertEquals(0.75, value, 1E-9)
        // the node did not move and was not selected
        assertEquals(Vector2(20.0, 20.0), n.position)
        assertTrue(editor.selection.isEmpty())
        // a body that does not take the press lets the node move
        n.bodyPointer = { false }
        cm.move(o + from); cm.press(o + from); cm.drag(o + to); cm.release(o + to)
        assertEquals(Vector2(20.0 + box.width * 0.5, 20.0), n.position)
    }

    @Test
    fun `compact nodes show their connected ports only and expand under a dragged wire`() {
        lateinit var editor: NodeEditor
        lateinit var a: GraphNode
        lateinit var b: GraphNode
        val changed = mutableListOf<List<GraphNode>>()
        val cm = panel {
            css("node-editor { width: 600px; height: 400px }")
            layout {
                editor = nodeEditor {
                    a = add(GraphNode("a", outputs = listOf(Port("x"), Port("y"))), Vector2(20.0, 20.0))
                    b = add(GraphNode("b", listOf(Port("p"), Port("q"), Port("r"))), Vector2(300.0, 20.0))
                    b.body = { _, _ -> }
                    b.bodyHeight = 40.0
                    b.status = "ok"
                    connect(a, 1, b, 2)
                    events.compactChanged.listen { changed += it }
                }
            }
        }
        cm.frames()
        val full = b.height
        editor.setCompact(listOf(b), true)
        assertEquals(listOf(listOf(b)), changed)
        // one row, for the connected input r, without body and status
        assertEquals(NodeEditor.TITLE_HEIGHT + NodeEditor.PORT_SPACING + 8.0, b.height)
        assertTrue(b.isInputShown(2) && !b.isInputShown(0) && !b.isInputShown(1))
        assertEquals(b.position.y + NodeEditor.TITLE_HEIGHT + 0.5 * NodeEditor.PORT_SPACING + 4.0, b.inputPosition(2).y)
        assertEquals(b.position.y + NodeEditor.TITLE_HEIGHT / 2.0, b.inputPosition(0).y)
        assertEquals(0.0, b.bodyBounds.height)
        // undo expands it again
        cm.history.undo()
        assertEquals(full, b.height)
        cm.history.redo()
        assertTrue(b.isCompact)

        // a wire dragged over b shows all its inputs, and connects to one that was hidden
        val o = Vector2(editor.layout.screenX, editor.layout.screenY)
        val from = a.outputPosition(0)
        cm.move(o + from); cm.press(o + from)
        cm.drag(o + b.position + Vector2(40.0, 10.0))
        assertFalse(b.isCompact)
        val target = b.inputPosition(0)
        cm.drag(o + target)
        cm.release(o + target)
        assertTrue(b.isCompact)
        assertTrue(Wire(a, 0, b, 0) in editor.wires)
        assertTrue(b.isInputShown(0) && !b.isInputShown(1))

        // the editor wide default, for nodes that follow it
        changed.clear()
        a.compact = null
        editor.compactNodes = true
        assertEquals(listOf(listOf(a)), changed)
        assertTrue(a.isCompact)
    }

    @Test
    fun `hovering over a port highlights its upstream or downstream`() {
        lateinit var editor: NodeEditor
        lateinit var a: GraphNode
        lateinit var b: GraphNode
        lateinit var c: GraphNode
        lateinit var d: GraphNode
        lateinit var e: GraphNode
        val cm = panel {
            css("node-editor { width: 900px; height: 400px }")
            layout {
                editor = nodeEditor {
                    // a and d go into b, b into c and e
                    a = add(GraphNode("a", outputs = listOf(Port("o"))), Vector2(20.0, 20.0))
                    d = add(GraphNode("d", outputs = listOf(Port("o"))), Vector2(20.0, 200.0))
                    b = add(GraphNode("b", listOf(Port("x"), Port("y")), listOf(Port("o"))), Vector2(250.0, 20.0))
                    c = add(GraphNode("c", listOf(Port("i"))), Vector2(480.0, 20.0))
                    e = add(GraphNode("e", listOf(Port("i"))), Vector2(480.0, 200.0))
                    connect(a, 0, b, 0); connect(d, 0, b, 1); connect(b, 0, c, 0); connect(b, 0, e, 0)
                }
            }
        }
        cm.frames()
        val o = Vector2(editor.layout.screenX, editor.layout.screenY)
        // off by default
        cm.move(o + c.inputPosition(0))
        assertTrue(editor.highlightedNodes.isEmpty())
        editor.highlightUpstream = true
        editor.highlightDownstream = true
        // the input of c: b, and through all inputs of b, a and d
        cm.move(o + c.inputPosition(0) + Vector2(1.0, 0.0))
        assertEquals(setOf(b, a, d), editor.highlightedNodes)
        assertEquals(3, editor.highlightedWires.size)
        // the second input of b: d only
        cm.move(o + b.inputPosition(1))
        assertEquals(setOf(d), editor.highlightedNodes)
        // the output of a: b, and through b, c and e
        cm.move(o + a.outputPosition(0))
        assertEquals(setOf(b, c, e), editor.highlightedNodes)
        assertEquals(setOf(Wire(a, 0, b, 0), Wire(b, 0, c, 0), Wire(b, 0, e, 0)), editor.highlightedWires)
        // away from the ports nothing is highlighted
        cm.move(o + Vector2(700.0, 380.0))
        assertTrue(editor.highlightedNodes.isEmpty() && editor.highlightedWires.isEmpty())
    }

    @Test
    fun `dragging past the edges of the node editor pans the view`() {
        lateinit var editor: NodeEditor
        lateinit var a: GraphNode
        val cm = panel {
            css("node-editor { width: 600px; height: 400px }")
            layout { editor = nodeEditor { a = add(GraphNode("a", listOf(Port("in"))), Vector2(200.0, 150.0)) } }
        }
        cm.frames()
        val o = Vector2(editor.layout.screenX, editor.layout.screenY)
        val grab = Vector2(240.0, 160.0)
        // the node is dragged past the right edge and held there
        val past = Vector2(620.0, 160.0)
        cm.move(o + grab); cm.press(o + grab); cm.drag(o + Vector2(400.0, 160.0)); cm.drag(o + past)
        val panBefore = editor.pan
        repeat(10) { Thread.sleep(20); cm.frames(1) }
        assertTrue(editor.pan.x < panBefore.x - 20.0, "${editor.pan}")
        assertEquals(panBefore.y, editor.pan.y, 1E-9)
        // the node stays under the pointer
        assertEquals(o.x + past.x - (grab.x - 200.0), o.x + editor.pan.x + a.position.x * editor.zoom, 1E-6)
        cm.release(o + past)
        // it stops on release, one edit moves the node back
        val pan = editor.pan
        repeat(3) { Thread.sleep(20); cm.frames(1) }
        assertEquals(pan, editor.pan)
        assertEquals("Move nodes", cm.history.undoName)
        cm.history.undo()
        assertEquals(Vector2(200.0, 150.0), a.position)

        // a box dragged over the top edge pans up and grows with it, without edgePan the view stays
        val p0 = Vector2(500.0, 300.0)
        cm.move(o + p0); cm.press(o + p0); cm.drag(o + Vector2(500.0, 100.0)); cm.drag(o + Vector2(500.0, 4.0))
        val y = editor.pan.y
        repeat(5) { Thread.sleep(20); cm.frames(1) }
        assertTrue(editor.pan.y > y, "${editor.pan}")
        cm.release(o + Vector2(500.0, 4.0))
        editor.edgePan = false
        cm.move(o + grab); cm.press(o + grab); cm.drag(o + Vector2(400.0, 160.0)); cm.drag(o + past)
        val still = editor.pan
        repeat(5) { Thread.sleep(20); cm.frames(1) }
        assertEquals(still, editor.pan)
        cm.release(o + past)
    }

    @Test
    fun `a program records the edits of the node and fcurve editors itself`() {
        lateinit var editor: NodeEditor
        lateinit var curves: FCurveEditor
        lateinit var a: GraphNode
        val recorded = mutableListOf<Triple<String, () -> Unit, () -> Unit>>()
        val cm = panel {
            css("node-editor { width: 600px; height: 300px } fcurve-editor { width: 600px; height: 300px }")
            layout {
                editor = nodeEditor { a = add(GraphNode("a"), Vector2(20.0, 20.0)) }
                curves = fcurveEditor { value = org.openrndr.extra.fcurve.fcurve("M0 L1,1 L1,0") }
            }
        }
        editor.recordEdit = { name, undo, redo -> recorded += Triple(name, undo, redo) }
        curves.recordEdit = { name, undo, redo -> recorded += Triple(name, undo, redo) }
        cm.frames()
        val o = Vector2(editor.layout.screenX, editor.layout.screenY)
        val from = o + Vector2(40.0, 30.0)
        cm.move(from); cm.press(from); cm.drag(from + Vector2(20.0, 20.0)); cm.drag(from + Vector2(50.0, 60.0)); cm.release(from + Vector2(50.0, 60.0))
        assertEquals(listOf("Move nodes"), recorded.map { it.first })
        // nothing went into the history of the control manager
        assertFalse(cm.history.canUndo)
        // the program undoes and redoes it
        val moved = a.position
        recorded.last().second()
        assertEquals(Vector2(20.0, 20.0), a.position)
        recorded.last().third()
        assertEquals(moved, a.position)

        // a key of the curve dragged up
        val key = curves.screenPosition(Vector2(1.0, 1.0))
        cm.move(key); cm.press(key); cm.drag(key - Vector2(0.0, 20.0)); cm.drag(key - Vector2(0.0, 40.0)); cm.release(key - Vector2(0.0, 40.0))
        assertEquals("Move keys", recorded.last().first)
        val edited = curves.value
        recorded.last().second()
        assertEquals(1.0, curves.value.segments[0].end.y, 1E-9)
        recorded.last().third()
        assertEquals(edited, curves.value)
        assertFalse(cm.history.canUndo)
    }

    @Test
    fun `the body and the status of a node are under its ports`() {
        val n = GraphNode("n", listOf(Port("a"), Port("b")))
        val plain = n.height
        n.body = { _, _ -> }
        n.bodyHeight = 40.0
        assertEquals(plain + 40.0, n.height)
        assertEquals(NodeEditor.TITLE_HEIGHT + 2 * NodeEditor.PORT_SPACING + 8.0, n.bodyBounds.y)
        n.status = "division by zero"
        assertEquals(plain + 40.0 + NodeEditor.STATUS_HEIGHT, n.height)
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

    @Test
    fun `fcurves are edited by keys, cut, broken and sculpted, and stay functions of time`() {
        lateinit var editor: FCurveEditor
        val cm = panel {
            css("fcurve-editor { width: 600px; height: 300px }")
            layout {
                editor = fcurveEditor {
                    value = org.openrndr.extra.fcurve.fcurve("M0 L1,1 L1,0 L1,1 L1,0")
                }
            }
        }
        cm.frames()
        val c0 = editor.value
        assertEquals(4, c0.segments.size)
        fun at(t: Double, v: Double) = editor.screenPosition(Vector2(t, v))
        fun drag(from: Vector2, to: Vector2) {
            cm.move(from); cm.press(from)
            cm.drag((from + to) / 2.0); cm.drag(to)
            cm.release(to)
        }
        // the key at time 1 is moved up
        drag(at(1.0, 1.0), at(1.0, 1.5))
        assertEquals(1.5, editor.value.segments[0].end.y, 1E-6)
        assertEquals(1.5, editor.value.segments[1].start.y, 1E-6)
        cm.history.undo()
        assertEquals(1.0, editor.value.segments[0].end.y, 1E-6)

        // a cut at 0.5 adds a key on the curve
        editor.tool = FCurveTool.CUT
        cm.move(at(0.5, 0.5)); cm.press(at(0.5, 0.5)); cm.release(at(0.5, 0.5))
        assertEquals(5, editor.value.segments.size)
        assertEquals(0.5, editor.value.segments[1].start.x, 1E-3)
        assertEquals(0.5, editor.value.value(0.5), 1E-3)

        // breaking at 3 makes a jump
        editor.tool = FCurveTool.BREAK
        drag(at(3.0, 1.0), at(3.0, 0.5))
        val jumped = editor.value
        val i = jumped.segments.indexOfFirst { abs(it.start.x - 3.0) < 1E-6 }
        assertEquals(1.0, jumped.segments[i - 1].end.y, 1E-6)
        assertEquals(0.5, jumped.segments[i].start.y, 1E-3)
        assertEquals(2, jumped.contours().size)

        // grabbing at 2 lowers the values around it, less to the edges, and nothing outside the brush
        editor.tool = FCurveTool.GRAB
        editor.brushRadius = editor.screenPosition(Vector2(1.5, 0.0)).x - editor.screenPosition(Vector2(1.0, 0.0)).x
        val before = editor.value
        drag(at(2.0, 0.0), at(2.0, -0.4))
        val grabbed = editor.value
        assertEquals(-0.4, grabbed.value(2.0), 1E-2)
        assertTrue(grabbed.value(2.25) < before.value(2.25) && grabbed.value(2.25) > before.value(2.25) - 0.4)
        assertEquals(before.value(1.25), grabbed.value(1.25), 1E-6)
        assertEquals(before.value(2.75), grabbed.value(2.75), 1E-6)
        fun functionShaped(c: org.openrndr.extra.fcurve.FCurve) = c.segments.all { s ->
            (0..50).map { s.position(it / 50.0).x }.zipWithNext().all { (a, b) -> b >= a - 1E-9 }
        }
        assertTrue(functionShaped(grabbed))

        // smoothing over the jump keeps it
        editor.tool = FCurveTool.SMOOTH
        editor.brushStrength = 1.0
        drag(at(3.0, 0.7), at(3.05, 0.7))
        val smoothed = editor.value
        val j = smoothed.segments.indexOfFirst { abs(it.start.x - 3.0) < 1E-6 }
        assertTrue(abs(smoothed.segments[j - 1].end.y - smoothed.segments[j].start.y) > 0.3)
        assertTrue(functionShaped(smoothed))

        // the selected keys are removed
        editor.tool = FCurveTool.SELECT
        val count = editor.value.segments.size
        cm.move(at(0.5, 0.5)); cm.press(at(0.5, 0.5)); cm.release(at(0.5, 0.5))
        cm.keyboardInput.target = editor
        cm.key(KEY_DELETE)
        assertEquals(count - 1, editor.value.segments.size)
        // the whole curve is undone
        while (cm.history.canUndo) cm.history.undo()
        assertEquals(c0.segments.map { it.start }, editor.value.segments.map { it.start })
    }

    @Test
    fun `a selection of an fcurve is simplified, keeping its jumps and its ends`() {
        // a sine in 400 short segments, with a jump in the middle
        val segments = mutableListOf<org.openrndr.shape.Segment2D>()
        val n = 400
        fun f(x: Double) = kotlin.math.sin(x * 2.0) + if (x >= 2.0) 1.0 else 0.0
        fun df(x: Double) = 2.0 * kotlin.math.cos(x * 2.0)
        for (i in 0 until n) {
            val x0 = i * 4.0 / n
            val x1 = (i + 1) * 4.0 / n
            val h = (x1 - x0) / 3.0
            val a = Vector2(x0, f(x0))
            val b = Vector2(x1, if (i == n / 2 - 1) kotlin.math.sin(x1 * 2.0) else f(x1))
            segments.add(org.openrndr.shape.Segment2D(a, a + Vector2(h, df(x0) * h), b - Vector2(h, df(x1) * h), b))
        }
        lateinit var editor: FCurveEditor
        val cm = panel {
            css("fcurve-editor { width: 800px; height: 400px }")
            layout { editor = fcurveEditor { value = org.openrndr.extra.fcurve.FCurve(segments) } }
        }
        cm.frames()
        val original = editor.value
        assertEquals(2, original.contours().size)
        // a box over everything selects all keys
        val a = Vector2(editor.layout.screenX + 50.0, editor.layout.screenY + 25.0)
        val b = Vector2(editor.layout.screenX + editor.layout.screenWidth - 1.0, editor.layout.screenY + editor.layout.screenHeight - 1.0)
        cm.move(a); cm.press(a); cm.drag((a + b) / 2.0); cm.drag(b); cm.release(b)
        assertEquals(n + 1, editor.selection.size)
        editor.simplifySelection(0.5)
        val simple = editor.value
        assertTrue(simple.segments.size < 40, "${simple.segments.size}")
        // the jump and the ends stay
        assertEquals(2, simple.contours().size)
        assertEquals(original.segments.first().start, simple.segments.first().start)
        assertEquals(original.segments.last().end, simple.segments.last().end)
        // within half a pixel, in values: the view shows about 3 units in 400 pixels
        val pixel = 1.0 / editor.zoomY
        for (i in 0..200) {
            val x = i * 4.0 / 200
            if (abs(x - 2.0) < 0.02) continue
            assertEquals(original.value(x), simple.value(x), pixel * 1.5)
        }
        assertTrue(simple.segments.all { s -> (0..50).map { s.position(it / 50.0).x }.zipWithNext().all { (p, q) -> q >= p - 1E-9 } })
        assertEquals("Simplify curve", cm.history.undoName)
        cm.history.undo()
        assertEquals(n, editor.value.segments.size)
    }

    @Test
    fun `the view of the fcurve editor widens to keys that were dragged out of it`() {
        lateinit var editor: FCurveEditor
        val cm = panel {
            css("fcurve-editor { width: 800px; height: 400px }")
            layout { editor = fcurveEditor { value = org.openrndr.extra.fcurve.fcurve("M0 L1,0 L1,1 L1,0") } }
        }
        cm.frames()
        val area = org.openrndr.shape.Rectangle(editor.layout.screenX, editor.layout.screenY, editor.layout.screenWidth, editor.layout.screenHeight)
        fun inView(p: Vector2) = editor.screenPosition(p).let { s -> s.x > area.x && s.x < area.x + area.width && s.y > area.y && s.y < area.y + area.height }
        // the key at 2 goes far over the top of the editor
        val from = editor.screenPosition(Vector2(2.0, 1.0))
        val to = from - Vector2(0.0, 700.0)
        cm.move(from); cm.press(from); cm.drag(from - Vector2(0.0, 300.0)); cm.drag(to); cm.release(to)
        val moved = editor.value.segments[1].end
        assertTrue(moved.y > 2.0, "$moved")
        assertFalse(inView(moved))
        // after a short animation it is in view again, as are the keys that were
        Thread.sleep(300)
        cm.frames(2)
        assertTrue(inView(moved), "${editor.screenPosition(moved)} $area")
        assertTrue(inView(Vector2(0.0, 0.0)) && inView(Vector2(3.0, 0.0)))
        // without followEdits the view stays
        editor.followEdits = false
        val zoom = editor.zoomY
        val from2 = editor.screenPosition(moved)
        cm.move(from2); cm.press(from2); cm.drag(from2 + Vector2(0.0, 300.0)); cm.drag(from2 + Vector2(0.0, 900.0)); cm.release(from2 + Vector2(0.0, 900.0))
        Thread.sleep(300)
        cm.frames(2)
        assertEquals(zoom, editor.zoomY)
    }

    @Test
    fun `fcurves are built from nothing with the pen and by drawing, and removed again`() {
        lateinit var editor: FCurveEditor
        val cm = panel {
            css("fcurve-editor { width: 600px; height: 300px }")
            layout { editor = fcurveEditor { } }
        }
        cm.frames()
        assertTrue(editor.value.segments.isEmpty())
        editor.viewX = 0.0; editor.viewY = 0.0; editor.zoomX = 100.0; editor.zoomY = 100.0
        fun at(t: Double, v: Double) = editor.screenPosition(Vector2(t, v))
        fun click(p: Vector2) { cm.move(p); cm.press(p); cm.release(p) }
        editor.tool = FCurveTool.PEN
        // the first key has no segment yet, the second makes one
        click(at(1.0, 0.0))
        assertTrue(editor.value.segments.isEmpty())
        click(at(2.0, 1.0))
        assertEquals(1, editor.value.segments.size)
        // after the end and before the start the curve gets longer
        click(at(3.0, 0.0))
        click(at(0.5, 0.5))
        val c = editor.value
        assertEquals(listOf(0.5, 1.0, 2.0), c.segments.map { it.start.x })
        assertEquals(3.0, c.end, 1E-9)
        assertEquals(1.0, c.value(2.0), 1E-9)
        // a key on the curve takes the value of the pointer
        click(at(2.5, 0.8))
        assertEquals(0.8, editor.value.value(2.5), 1E-6)
        // dragging pulls out the handles of a new key
        val p = at(4.0, 0.0)
        cm.move(p); cm.press(p); cm.drag(p + Vector2(20.0, -20.0)); cm.drag(p + Vector2(40.0, -40.0)); cm.release(p + Vector2(40.0, -40.0))
        val last = editor.value.segments.last()
        assertTrue(last.cubic.control[1].y < 0.0, "${last.cubic.control[1]}")
        // constant keys hold their value and jump
        cm.keyboardInput.target = editor
        editor.tool = FCurveTool.SELECT
        click(at(1.0, 0.0))
        editor.setInterpolation(FCurveEditor.Interpolation.CONSTANT)
        assertEquals(0.0, editor.value.value(1.9), 1E-9)
        assertEquals(2, editor.value.contours().size)
        // the first key takes the first segment with it
        click(at(0.5, 0.5))
        cm.key(KEY_DELETE)
        assertEquals(1.0, editor.value.start, 1E-9)
        // all keys removed leave an empty curve, which can be drawn again
        cm.key(0, if (System.getProperty("os.name").lowercase().contains("mac")) KeyModifier.SUPER else KeyModifier.CTRL, name = "a")
        assertEquals(editor.value.segments.size + 1, editor.selection.size)
        cm.key(KEY_DELETE)
        assertTrue(editor.value.segments.isEmpty())
        editor.tool = FCurveTool.DRAW
        val from = at(0.0, 0.0)
        cm.move(from); cm.press(from)
        for (i in 1..40) cm.drag(at(i * 0.1, kotlin.math.sin(i * 0.1 * 2.0)))
        cm.release(at(4.0, kotlin.math.sin(8.0)))
        val drawn = editor.value
        assertTrue(drawn.segments.isNotEmpty())
        assertEquals(kotlin.math.sin(2.0), drawn.value(1.0), 0.05)
        // a stroke over part of the curve replaces that part and joins the curve at its ends
        val count = drawn.segments.size
        val a = at(1.0, drawn.value(1.0))
        cm.move(a); cm.press(a)
        for (i in 1..10) cm.drag(at(1.0 + i * 0.1, drawn.value(1.0 + i * 0.1) + if (i < 10) 0.5 * kotlin.math.sin(i / 10.0 * kotlin.math.PI) else 0.0))
        cm.release(at(2.0, drawn.value(2.0)))
        val redrawn = editor.value
        assertEquals(1, redrawn.contours().size)
        assertEquals(drawn.value(0.5), redrawn.value(0.5), 1E-6)
        assertEquals(drawn.value(3.0), redrawn.value(3.0), 1E-6)
        assertTrue(redrawn.value(1.5) > drawn.value(1.5) + 0.3)
        // everything is undone, back to the empty curve
        while (cm.history.canUndo) cm.history.undo()
        assertTrue(editor.value.segments.isEmpty())
    }
}
