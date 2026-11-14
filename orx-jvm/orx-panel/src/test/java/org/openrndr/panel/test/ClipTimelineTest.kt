package org.openrndr.panel.test

import org.openrndr.KEY_DELETE
import org.openrndr.KeyModifier
import org.openrndr.math.Vector2
import org.openrndr.panel.ControlManager
import org.openrndr.panel.elements.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ClipTimelineTest : PanelTestFixture() {
    private fun ControlManager.frames(n: Int = 2) = repeat(n) { afterDraw(program.drawer, program) }

    private class Setup(val cm: ControlManager, val timeline: ClipTimeline) {
        val changes = mutableListOf<ValueChange<ClipTimelineValue>>()
        val recorded = mutableListOf<Triple<String, () -> Unit, () -> Unit>>()
        val clips get() = timeline.value.clips
    }

    private fun timeline(): Setup {
        lateinit var timeline: ClipTimeline
        val cm = panel {
            css("clip-timeline { width: 600px; height: 200px }")
            layout {
                timeline = clipTimeline {
                    value = ClipTimelineValue(
                        listOf("scenes", "lights", "camera"),
                        listOf(TimelineClip(0, 1.0, 2.0, "one", tag = 7), TimelineClip(1, 4.0, 1.0, "two"))
                    )
                }
            }
        }
        cm.frames()
        val s = Setup(cm, timeline)
        timeline.valueChanged.listen { s.changes.add(it) }
        timeline.recordEdit = { name, undo, redo -> s.recorded += Triple(name, undo, redo) }
        return s
    }

    /** a drag from [from] to [to] in steps */
    private fun ControlManager.drag(from: Vector2, to: Vector2, modifiers: Array<KeyModifier> = emptyArray()) {
        move(from); press(from, org.openrndr.MouseButton.LEFT, *modifiers)
        for (k in 1..4) drag(from + (to - from) * (k / 4.0))
        release(to)
    }

    private fun ControlManager.click(p: Vector2, vararg modifiers: KeyModifier) {
        // more than the time of a double click after the last one
        Thread.sleep(550)
        move(p); press(p, org.openrndr.MouseButton.LEFT, *modifiers); release(p)
    }

    @Test
    fun `clips move in time and across tracks, as one edit that is undone and redone`() {
        val s = timeline()
        val t = s.timeline
        // one second later and a track down: 40 pixels per second
        s.cm.drag(t.screenPosition(2.0, 0), t.screenPosition(3.0, 1))
        assertEquals(TimelineClip(1, 2.0, 2.0, "one", tag = 7), s.clips[0])
        assertEquals(1, s.changes.size, "one change for the drag")
        assertEquals(listOf("Move clip"), s.recorded.map { it.first })
        s.recorded.last().second()
        assertEquals(TimelineClip(0, 1.0, 2.0, "one", tag = 7), s.clips[0])
        s.recorded.last().third()
        assertEquals(2.0, s.clips[0].start, 1E-9)
        assertEquals(3, s.changes.size, "the undo and the redo are changes too")

        // both selected: they move together, not before 0, not past the last track
        t.selection = setOf(0, 1)
        s.cm.drag(t.screenPosition(4.5, 1), t.screenPosition(-3.0, 2))
        assertEquals(0.0, s.clips[0].start, 1E-9)
        assertEquals(2.0, s.clips[1].start, 1E-9)
        assertEquals(listOf(2, 2), s.clips.map { it.track })
        assertEquals("Move clips", s.recorded.last().first)
    }

    @Test
    fun `clips snap to the grid, to the edges of other clips and to the playhead`() {
        val s = timeline()
        val t = s.timeline
        t.snap = 0.5
        // 1.3 seconds later would start at 2.3, the grid makes it 2.5
        s.cm.drag(t.screenPosition(2.0, 0), t.screenPosition(3.3, 0))
        assertEquals(2.5, s.clips[0].start, 1E-9)
        t.snap = 0.0
        // from 2.5 to 2.1, its end at 4.1 is close to the start of the other clip: the end snaps to 4.0
        s.cm.drag(t.screenPosition(3.5, 0), t.screenPosition(3.1, 0))
        assertEquals(2.0, s.clips[0].start, 1E-9, "${s.clips[0]}")
        // to the playhead
        t.playhead = 0.5
        s.cm.drag(t.screenPosition(3.0, 0), t.screenPosition(1.6, 0))
        assertEquals(0.5, s.clips[0].start, 1E-9, "${s.clips[0]}")
    }

    @Test
    fun `the edges of a clip trim it`() {
        val s = timeline()
        val t = s.timeline
        // the right edge, 0.6 seconds later
        s.cm.drag(t.screenPosition(3.0, 0) - Vector2(2.0, 0.0), t.screenPosition(3.6, 0) - Vector2(2.0, 0.0))
        assertEquals(1.0, s.clips[0].start, 1E-9)
        assertEquals(2.55, s.clips[0].duration, 0.06)
        assertEquals("Trim clip", s.recorded.last().first)
        // the left edge: the start moves, the end stays
        val end = s.clips[0].end
        s.cm.drag(t.screenPosition(1.0, 0) + Vector2(2.0, 0.0), t.screenPosition(1.5, 0) + Vector2(2.0, 0.0))
        assertEquals(1.5, s.clips[0].start, 0.06)
        assertEquals(end, s.clips[0].end, 1E-9)
        // not shorter than the shortest
        t.minimumDuration = 0.25
        s.cm.drag(t.screenPosition(1.5, 0) + Vector2(2.0, 0.0), t.screenPosition(5.0, 0))
        assertEquals(0.25, s.clips[0].duration, 1E-9)
        assertEquals(end, s.clips[0].end, 1E-9)
    }

    @Test
    fun `clicks select, delete removes, a double click adds, and the ruler scrubs`() {
        val s = timeline()
        val t = s.timeline
        val selections = mutableListOf<Set<Int>>()
        t.selectionChanged.listen { selections.add(it) }
        s.cm.click(t.screenPosition(4.5, 1))
        assertEquals(setOf(1), t.selection)
        s.cm.click(t.screenPosition(2.0, 0), KeyModifier.SHIFT)
        assertEquals(setOf(0, 1), t.selection)
        s.cm.click(t.screenPosition(2.0, 0), KeyModifier.SHIFT)
        assertEquals(setOf(1), t.selection)
        s.cm.click(t.screenPosition(8.0, 2))
        assertEquals(emptySet(), t.selection)
        assertEquals(listOf(setOf(1), setOf(0, 1), setOf(1), emptySet()), selections)
        assertTrue(s.changes.isEmpty(), "selecting changes nothing")

        // delete
        s.cm.click(t.screenPosition(4.5, 1))
        s.cm.key(KEY_DELETE)
        assertEquals(listOf("one"), s.clips.map { it.label })
        assertEquals("Delete clip", s.recorded.last().first)
        s.recorded.last().second()
        assertEquals(listOf("one", "two"), s.clips.map { it.label })

        // a double click on empty space asks for a clip
        val asked = mutableListOf<Pair<Int, Double>>()
        t.newClip = { track, time -> asked.add(track to time); TimelineClip(track, time, 1.0, "new") }
        t.snap = 0.5
        val p = t.screenPosition(6.1, 2)
        Thread.sleep(550)
        s.cm.move(p); s.cm.press(p); s.cm.release(p); s.cm.press(p); s.cm.release(p)
        assertEquals(listOf(2 to 6.0), asked)
        assertEquals(TimelineClip(2, 6.0, 1.0, "new"), s.clips.last())
        assertEquals(setOf(2), t.selection)
        assertEquals("Add clip", s.recorded.last().first)

        // dragging in the ruler scrubs, the clips stay
        val times = mutableListOf<Double>()
        t.scrubbed.listen { times.add(it) }
        val before = t.value
        val ruler = t.screenPosition(2.0, 0).copy(y = t.layout.screenY + 10.0)
        s.cm.drag(ruler, ruler + Vector2(80.0, 0.0))
        assertEquals(2.0, times.first(), 0.05)
        assertEquals(4.0, times.last(), 0.05)
        assertEquals(before, t.value)
    }

    @Test
    fun `clips on tracks past the last are shown on the last`() {
        val s = timeline()
        val t = s.timeline
        t.value = ClipTimelineValue(listOf("only"), listOf(TimelineClip(3, 1.0, 1.0)))
        assertEquals(t.screenPosition(1.5, 0), t.screenPosition(1.5, 3))
        // and are on it once moved
        s.cm.drag(t.screenPosition(1.5, 0), t.screenPosition(2.5, 0))
        assertEquals(TimelineClip(0, 2.0, 1.0), s.clips.single())
    }

    @Test
    fun `a drag on empty space selects the clips in a rectangle, shift adds them`() {
        val s = timeline()
        val t = s.timeline
        s.cm.drag(t.screenPosition(0.5, 0), t.screenPosition(1.5, 0))
        assertEquals(setOf(0), t.selection)
        // a new rectangle replaces the selection, with shift it adds to it
        s.cm.drag(t.screenPosition(6.0, 2), t.screenPosition(4.5, 1))
        assertEquals(setOf(1), t.selection)
        s.cm.drag(t.screenPosition(0.5, 0), t.screenPosition(2.0, 0), arrayOf(KeyModifier.SHIFT))
        assertEquals(setOf(0, 1), t.selection)
        assertTrue(s.changes.isEmpty(), "selecting changes nothing")
        // alt on empty space pans
        val before = t.screenPosition(1.0, 0)
        s.cm.drag(t.screenPosition(7.0, 2), t.screenPosition(6.0, 2), arrayOf(KeyModifier.ALT))
        assertEquals(before.x - 40.0, t.screenPosition(1.0, 0).x, 1E-6)
        assertEquals(setOf(0, 1), t.selection)
    }

    @Test
    fun `copied clips are pasted at the pointer, cut removes them, and they keep what they were`() {
        val s = timeline()
        val t = s.timeline
        t.clipboard = ClipTimelineClipboard()
        s.cm.click(t.screenPosition(2.0, 0))
        s.cm.key(0, KeyModifier.SUPER, name = "a")
        s.cm.key(0, KeyModifier.SUPER, name = "c")
        assertEquals(2, t.clipboard.clips.size)
        // the earliest at the pointer, the topmost on the track under it
        s.cm.move(t.screenPosition(6.0, 1))
        s.cm.key(0, KeyModifier.SUPER, name = "v")
        assertEquals(listOf(TimelineClip(1, 6.0, 2.0, "one", tag = 7), TimelineClip(2, 9.0, 1.0, "two")), s.clips.drop(2))
        assertEquals(setOf(2, 3), t.selection)
        assertEquals(1, s.changes.size)
        assertEquals("Paste clips", s.recorded.last().first)
        // within the tracks
        s.cm.move(t.screenPosition(12.0, 2))
        s.cm.key(0, KeyModifier.SUPER, name = "v")
        assertEquals(listOf(1, 2), s.clips.drop(4).map { it.track })
        s.recorded.last().second()
        assertEquals(4, s.clips.size)
        assertEquals(setOf(2, 3), t.selection, "the undo restores the selection")

        // cut, then paste at the playhead when the pointer is not over the tracks
        s.cm.key(0, KeyModifier.SUPER, name = "x")
        assertEquals(2, s.clips.size)
        assertEquals("Cut clips", s.recorded.last().first)
        t.playhead = 10.0
        s.cm.move(t.screenPosition(2.0, 0).copy(y = t.layout.screenY + 5.0))
        s.cm.key(0, KeyModifier.SUPER, name = "v")
        assertEquals(listOf(10.0, 13.0), s.clips.drop(2).map { it.start })
        // and where asked, the second copied clip a track lower keeps the first on the one before the last
        t.paste(0.5, 2)
        assertEquals(listOf(TimelineClip(1, 0.5, 2.0, "one", tag = 7), TimelineClip(2, 3.5, 1.0, "two")), s.clips.drop(4))
        // the clipboard is shared by timelines unless replaced
        assertTrue(ClipTimeline().clipboard === ClipTimelineClipboard.shared)
    }

    @Test
    fun `alt drags copies of the selected clips`() {
        val s = timeline()
        val t = s.timeline
        s.cm.click(t.screenPosition(2.0, 0))
        s.cm.drag(t.screenPosition(2.0, 0), t.screenPosition(3.0, 2), arrayOf(KeyModifier.ALT))
        assertEquals(TimelineClip(0, 1.0, 2.0, "one", tag = 7), s.clips[0], "the original stays")
        assertEquals(TimelineClip(2, 2.0, 2.0, "one", tag = 7), s.clips[2])
        assertEquals(setOf(2), t.selection)
        assertEquals(1, s.changes.size)
        assertEquals("Duplicate clip", s.recorded.last().first)
        s.recorded.last().second()
        assertEquals(2, s.clips.size)
        assertEquals(setOf(0), t.selection)
    }

    @Test
    fun `the razor cuts clips in two`() {
        val s = timeline()
        val t = s.timeline
        val tools = mutableListOf<ClipTimelineTool>()
        t.toolChanged.listen { tools.add(it) }
        s.cm.click(t.screenPosition(8.0, 2))
        s.cm.key(0, name = "c")
        assertEquals(listOf(ClipTimelineTool.RAZOR), tools)
        s.cm.click(t.screenPosition(2.0, 0))
        assertEquals(listOf(TimelineClip(0, 1.0, 1.0, "one", tag = 7), TimelineClip(0, 2.0, 1.0, "one", tag = 7)), s.clips.take(2))
        assertEquals(setOf(1), t.selection)
        assertEquals("Split clip", s.recorded.last().first)
        assertEquals(1, s.changes.size)
        // not closer to an edge than the shortest a clip can be
        s.cm.click(t.screenPosition(2.02, 0))
        assertEquals(3, s.clips.size)
        // undone
        s.recorded.last().second()
        assertEquals(2, s.clips.size)

        // shift cuts all clips there, on all tracks, with what the hook makes of them
        t.value = ClipTimelineValue(listOf("a", "b"), listOf(TimelineClip(0, 0.0, 4.0, "x"), TimelineClip(1, 1.0, 4.0, "y"), TimelineClip(1, 6.0, 1.0, "z")))
        t.splitClip = { c, at -> c.copy(duration = at - c.start, label = c.label + "1") to c.copy(start = at, duration = c.end - at, label = c.label + "2") }
        s.cm.click(t.screenPosition(3.0, 1), KeyModifier.SHIFT)
        assertEquals(listOf("x1", "x2", "y1", "y2", "z"), s.clips.map { it.label })
        assertEquals(listOf(0.0, 3.0, 1.0, 3.0, 6.0), s.clips.map { it.start })
        assertEquals("Split clips", s.recorded.last().first)

        // by code: the selected clips at a time
        s.cm.key(0, name = "v")
        assertEquals(ClipTimelineTool.SELECT, t.tool)
        t.selection = setOf(3)
        assertTrue(t.splitAt(4.0))
        assertEquals(listOf("x1", "x2", "y1", "y21", "y22", "z"), s.clips.map { it.label })
        assertTrue(!t.splitAt(10.0))
    }
}
