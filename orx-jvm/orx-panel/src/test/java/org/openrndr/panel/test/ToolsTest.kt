package org.openrndr.panel.test

import org.openrndr.KEY_ARROW_UP
import org.openrndr.KEY_DELETE
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
}
