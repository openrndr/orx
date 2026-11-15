package org.openrndr.panel.elements

import org.openrndr.KEY_BACKSPACE
import org.openrndr.KEY_DELETE
import org.openrndr.KeyModifier
import org.openrndr.MouseButton
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.isolated
import org.openrndr.events.Event
import org.openrndr.math.Vector2
import org.openrndr.panel.Edit
import org.openrndr.panel.editHistory
import org.openrndr.panel.style.Cursor
import org.openrndr.panel.style.cursor
import org.openrndr.panel.style.effectiveColor
import org.openrndr.shape.Rectangle
import org.openrndr.shape.ShapeContour
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.round

/**
 * a clip of a [ClipTimeline]: on the track at index [track], from [start] for [duration] seconds; [label] and [color]
 * are how it is shown, the accent of the timeline when null, [tag] is the host's, like the number of a scene, and is
 * kept as it is; [fadeIn] and [fadeOut] are the seconds it fades in over at its start and out over at its end
 */
data class TimelineClip(
    val track: Int,
    val start: Double,
    val duration: Double,
    val label: String = "",
    val color: ColorRGBa? = null,
    val tag: Int = 0,
    val fadeIn: Double = 0.0,
    val fadeOut: Double = 0.0
) {
    val end: Double get() = start + duration

    /**
     * this clip with its fades within its duration, together too: [fadeIn] is clamped to the duration and [fadeOut]
     * to what is left when [keepsFadeIn], else the other way around
     */
    fun withFadesClamped(keepsFadeIn: Boolean = true): TimelineClip {
        val d = maxOf(0.0, duration)
        return if (keepsFadeIn) {
            val i = fadeIn.coerceIn(0.0, d)
            copy(fadeIn = i, fadeOut = fadeOut.coerceIn(0.0, d - i))
        } else {
            val o = fadeOut.coerceIn(0.0, d)
            copy(fadeIn = fadeIn.coerceIn(0.0, d - o), fadeOut = o)
        }
    }
}

/** what a [ClipTimeline] shows and edits: the names of its tracks, top to bottom, and its clips */
data class ClipTimelineValue(val tracks: List<String>, val clips: List<TimelineClip>)

/** the tools of a [ClipTimeline], with the [key] that chooses one */
enum class ClipTimelineTool(val key: String, val title: String) {
    /** selects, moves, trims and copies clips */
    SELECT("v", "Select"),

    /** cuts a clip in two where it is clicked, with shift all clips there */
    RAZOR("c", "Razor")
}

/** the clips that are copied, to paste them in the same or another [ClipTimeline], see [ClipTimeline.clipboard] */
class ClipTimelineClipboard {
    /** the clips, with their tracks and times as they were copied */
    var clips: List<TimelineClip> = emptyList()

    companion object {
        /** the clipboard that timelines share unless they are given another */
        val shared = ClipTimelineClipboard()
    }
}

/**
 * A timeline of clips on tracks, like the scenes of a sequencer. The host sets [value]; the user edits it:
 *
 * - a click selects a clip, shift adds it to or removes it from the selection, a click on empty space clears it;
 *   dragging on empty space selects the clips in a rectangle, with shift adding them
 * - dragging a clip moves the selected clips in time and across tracks, with alt it moves copies of them; dragging an
 *   edge of a clip trims it, keeping its fades within it
 * - the handles at the top left and the top right of a clip set how long the selected clips fade in and out, see
 *   [TimelineClip.fadeIn] and [TimelineClip.fadeOut]
 * - mod+c, mod+x and mod+v copy, cut and paste the selected clips, see [clipboard]; pasted clips start at the pointer
 * - with the [ClipTimelineTool.RAZOR] a click cuts a clip in two, see [splitClip]
 * - a double click on empty space adds the clip that [newClip] gives; delete and backspace remove the selected clips
 * - the wheel zooms in time around the pointer, sideways scrolling, the middle button and alt on empty space pan,
 *   shift and the wheel scroll the tracks; dragging in the ruler scrubs, see [scrubbed]
 *
 * Clips snap to multiples of [snap], to the edges of other clips and to the [playhead]. [valueChanged] fires once per
 * finished edit, which is recorded by [recordEdit], so that it can be undone. Clips on one track may overlap; clips on
 * tracks past the last are shown on the last.
 */
class ClipTimeline : Element(ElementType("clip-timeline")) {
    override val handlesKeyboardFocus = true
    override val handlesDoubleClick = true

    /** the value changed: at the end of an edit by the user, or set by the program */
    val valueChanged = Event<ValueChange<ClipTimelineValue>>("clip-timeline-value-changed")

    /** the selection changed */
    val selectionChanged = Event<Set<Int>>("clip-timeline-selection-changed")

    /** the time under the pointer while it drags in the ruler, for the host to move its transport to */
    val scrubbed = Event<Double>("clip-timeline-scrubbed")

    private var tracks: List<String> = emptyList()
    private var clips: MutableList<TimelineClip> = mutableListOf()

    /** the tracks and clips */
    var value: ClipTimelineValue
        get() = ClipTimelineValue(tracks, clips.toList())
        set(v) {
            val old = value
            apply(v)
            valueChanged.trigger(ValueChange(old, v, false))
        }

    /** sets [v] without an event, keeping the selection of the clips that are still there */
    private fun apply(v: ClipTimelineValue) {
        tracks = v.tracks
        clips = v.clips.toMutableList()
        selection = selection.filter { it in clips.indices }.toSet()
        requestRedraw()
    }

    /** the indices of the selected clips in the clips of [value] */
    var selection: Set<Int> = emptySet()
        set(v) {
            if (v != field) {
                field = v
                requestRedraw()
                selectionChanged.trigger(v)
            }
        }

    /** the time of the playhead, drawn over the ruler and the tracks, null for none */
    var playhead: Double? = null
        set(v) { field = v; requestRedraw() }

    /** the time where the timeline ends or loops, drawn as a marker, null for none */
    var end: Double? = null
        set(v) { field = v; requestRedraw() }

    /** the seconds that clips snap to, 0 for none; they also snap to the edges of other clips and to the playhead */
    var snap = 0.0

    /** the shortest a clip can be trimmed or cut to, in seconds */
    var minimumDuration = 0.05

    /** the tool that the pointer uses */
    var tool = ClipTimelineTool.SELECT
        set(v) {
            if (v != field) {
                field = v
                requestRedraw()
                toolChanged.trigger(v)
            }
        }

    /** the tool changed */
    val toolChanged = Event<ClipTimelineTool>("clip-timeline-tool-changed")

    /** the clipboard of copy, cut and paste, shared by all timelines unless it is replaced */
    var clipboard: ClipTimelineClipboard = ClipTimelineClipboard.shared

    /**
     * the two clips that [clip] is cut into at the time [at], like to have the second start where the first ends in
     * what it shows; null for two clips with the same label, color and tag as [clip]
     */
    var splitClip: ((clip: TimelineClip, at: Double) -> Pair<TimelineClip, TimelineClip>)? = null

    /** the pointer over the element, in its coordinates, null when it is not over it */
    private var pointer: Vector2? = null

    /** the clip under the pointer, whose fade handles are shown */
    private var hoveredClip: Int? = null
    private var shownCursor: Cursor = Cursor.Default

    /**
     * the clip to add for a double click on empty space, at [track] and the [time] under the pointer, snapped; null,
     * or a null result, adds nothing
     */
    var newClip: ((track: Int, time: Double) -> TimelineClip?)? = null

    /** the color of clips without a color of their own, of the outline of the selected clips and of the playhead */
    var accent: ColorRGBa = ColorRGBa.fromHex("#5ab0ff")
        set(v) { field = v; requestRedraw() }

    /**
     * Records an edit of this element, named [name], in the history so that it can be undone and redone, by default
     * in [org.openrndr.panel.editHistory]. A program can take this over, like to keep the edits in a history of its own.
     */
    var recordEdit: (name: String, undo: () -> Unit, redo: () -> Unit) -> Unit = { name, undo, redo ->
        editHistory?.record(Edit(name, null, undo, redo))
    }

    //<editor-fold desc="the view">
    /** the time at the left edge of the tracks */
    var viewX = 0.0
        set(v) { field = v; requestRedraw() }

    /** pixels per second */
    var zoomX = 40.0
        set(v) { field = v.coerceIn(1E-3, 1E6); requestRedraw() }

    /** how far the tracks are scrolled down, in pixels */
    var scrollY = 0.0
        set(v) { field = v.coerceIn(0.0, maxOf(0.0, rows * TRACK_HEIGHT - plot.height)); requestRedraw() }

    companion object {
        const val RULER_HEIGHT = 22.0
        const val HEADER_WIDTH = 96.0
        const val TRACK_HEIGHT = 28.0

        /** how close in pixels the pointer has to be to the edge of a clip to trim it */
        const val EDGE = 6.0
        /** how far down from the top of a clip its fade handles are grabbed */
        const val FADE_HANDLE = 8.0

        /** how close in pixels clips snap */
        const val SNAP_DISTANCE = 8.0
    }

    /** the part of the element where the tracks are drawn, in its own coordinates */
    private val plot: Rectangle get() = Rectangle(HEADER_WIDTH, RULER_HEIGHT, layout.screenWidth - HEADER_WIDTH, layout.screenHeight - RULER_HEIGHT)

    /** the number of rows: the tracks, at least one */
    private val rows: Int get() = maxOf(1, tracks.size)

    private fun sx(t: Double) = plot.x + (t - viewX) * zoomX
    private fun timeAt(x: Double) = viewX + (x - plot.x) / zoomX
    private fun row(track: Int) = track.coerceIn(0, rows - 1)
    private fun rowY(row: Int) = plot.y + row * TRACK_HEIGHT - scrollY
    private fun trackAt(y: Double) = floor((y - plot.y + scrollY) / TRACK_HEIGHT).toInt()
    private fun local(p: Vector2) = p - Vector2(layout.screenX, layout.screenY)

    /** where [clip] is drawn, in the coordinates of the element */
    private fun clipArea(clip: TimelineClip): Rectangle =
        Rectangle(sx(clip.start), rowY(row(clip.track)) + 2.0, maxOf(1.0, clip.duration * zoomX), TRACK_HEIGHT - 4.0)

    /** the point of the window where [time] is on the track at [track], in the middle of its row */
    fun screenPosition(time: Double, track: Int): Vector2 =
        Vector2(layout.screenX + sx(time), layout.screenY + rowY(row(track)) + TRACK_HEIGHT / 2.0)

    /** shows all clips, and the end marker */
    fun frame() {
        if (plot.width <= 0.0) return
        val starts = clips.map { it.start } + listOfNotNull(end)
        val ends = clips.map { it.end } + listOfNotNull(end)
        if (starts.isEmpty()) return
        val t0 = minOf(0.0, starts.min())
        val span = (ends.max() - t0).takeIf { it > 1E-9 } ?: 1.0
        zoomX = plot.width * 0.94 / span
        viewX = t0 - span * 0.03
    }

    private fun zoomAround(x: Double, f: Double) {
        val before = timeAt(x)
        zoomX *= f
        viewX += before - timeAt(x)
    }
    //</editor-fold>

    //<editor-fold desc="editing">
    /** what the pointer does */
    private sealed class Gesture {
        class Move(val time: Double, val track: Int, val grabbed: Int, val from: List<TimelineClip>, val moving: Set<Int>, val copy: Boolean) : Gesture()
        class Select(val start: Vector2, val base: Set<Int>, var end: Vector2) : Gesture()
        class Trim(val index: Int, val left: Boolean, val from: List<TimelineClip>) : Gesture()
        class Fade(val index: Int, val fadeIn: Boolean, val from: List<TimelineClip>) : Gesture()
        class Pan(val start: Vector2, val viewX: Double, val scrollY: Double) : Gesture()
        object Scrub : Gesture()
    }

    private var gesture: Gesture? = null

    /** where the handles of the fades of [clip] are, at the top of the clip, in the coordinates of the element */
    private fun fadeHandles(clip: TimelineClip): Pair<Double, Double> {
        val (fadeIn, fadeOut) = shownFades(clip)
        val a = clipArea(clip)
        return (a.x + fadeIn * zoomX) to (a.x + a.width - fadeOut * zoomX)
    }

    /** the fades of [clip] as they are shown: each within the duration, and when together longer, meeting in it */
    private fun shownFades(clip: TimelineClip): Pair<Double, Double> {
        val d = maxOf(0.0, clip.duration)
        val fadeIn = clip.fadeIn.coerceIn(0.0, d)
        val fadeOut = clip.fadeOut.coerceIn(0.0, d)
        val k = if (fadeIn + fadeOut > d && d > 0.0) d / (fadeIn + fadeOut) else 1.0
        return (fadeIn * k) to (fadeOut * k)
    }

    /**
     * the clip at [p], in the coordinates of the element, the topmost, with whether [p] is on its left (-1) or right (1)
     * edge, on the handle of its fade in (-2) or out (2), or in it (0)
     */
    private fun clipAt(p: Vector2): Pair<Int, Int>? {
        if (p.x < plot.x || p.y < plot.y) return null
        for (i in clips.indices.reversed()) {
            val a = clipArea(clips[i])
            if (p.y < a.y || p.y > a.y + a.height || p.x < a.x - EDGE || p.x > a.x + a.width + EDGE) continue
            // the handles of the fades, along the top, before the edges, where the clip is wide enough to grab both
            if (p.y <= a.y + FADE_HANDLE && a.width >= 4.0 * EDGE && p.x in a.x..(a.x + a.width)) {
                val (h0, h1) = fadeHandles(clips[i])
                val d0 = abs(p.x - h0)
                val d1 = abs(p.x - h1)
                if (minOf(d0, d1) <= EDGE) return i to if (d0 < d1 || (d0 == d1 && p.x < a.x + a.width / 2.0)) -2 else 2
            }
            // the edges where the clip is wide enough to have a middle
            val edge = minOf(EDGE, a.width / 3.0)
            return when {
                p.x <= a.x + edge -> i to -1
                p.x >= a.x + a.width - edge -> i to 1
                p.x in a.x..(a.x + a.width) -> i to 0
                else -> continue
            }
        }
        return null
    }

    /** the times clips snap to, other than the clips in [moving]: the edges of the other clips and the playhead */
    private fun snapTargets(moving: Set<Int>): List<Double> =
        clips.withIndex().filter { it.index !in moving }.flatMap { listOf(it.value.start, it.value.end) } + listOfNotNull(playhead)

    /** [t] snapped to the nearest of [targets] within [SNAP_DISTANCE] pixels, else to the grid of [snap] */
    private fun snapped(t: Double, targets: List<Double>): Double {
        val near = targets.minByOrNull { abs(it - t) }?.takeIf { abs(it - t) * zoomX <= SNAP_DISTANCE }
        return near ?: if (snap > 0.0) round(t / snap) * snap else t
    }

    /** sets the clips while an edit goes on, the event and the record follow when it ends */
    private fun update(edited: List<TimelineClip>) {
        clips = edited.toMutableList()
        requestRedraw()
    }

    /** the edit from [from] to the clips as they are: an event and a record that undoes and redoes it */
    private fun finish(name: String, from: List<TimelineClip>, selectionBefore: Set<Int> = selection) {
        val to = clips.toList()
        if (to == from) return
        val old = ClipTimelineValue(tracks, from)
        val new = ClipTimelineValue(tracks, to)
        val selectionAfter = selection
        valueChanged.trigger(ValueChange(old, new, false))
        recordEdit(name,
            { val before = value; apply(ClipTimelineValue(tracks, from)); selection = selectionBefore.filter { it in from.indices }.toSet(); valueChanged.trigger(ValueChange(before, value, false)) },
            { val before = value; apply(ClipTimelineValue(tracks, to)); selection = selectionAfter.filter { it in to.indices }.toSet(); valueChanged.trigger(ValueChange(before, value, false)) })
    }

    /** removes the selected clips */
    fun deleteSelected() = removeSelected("Delete")

    private fun removeSelected(verb: String) {
        if (selection.isEmpty()) return
        val from = clips.toList()
        val before = selection
        val removed = selection
        selection = emptySet()
        update(from.filterIndexed { i, _ -> i !in removed })
        finish(if (removed.size == 1) "$verb clip" else "$verb clips", from, before)
    }

    /** copies the selected clips to the [clipboard] */
    fun copySelected() {
        if (selection.isEmpty()) return
        clipboard.clips = selection.sorted().map { i -> clips[i].let { it.copy(track = row(it.track)) } }
    }

    /** copies the selected clips to the [clipboard] and removes them */
    fun cutSelected() {
        if (selection.isEmpty()) return
        copySelected()
        removeSelected("Cut")
    }

    /**
     * Pastes the clips of the [clipboard] such that the earliest starts at [time] and the topmost is on [track], the
     * others where they were from those, all within the tracks; the pasted clips are selected. Without a time: at the
     * pointer when it is over the tracks, snapped, else at the [playhead], else right after the copied clips. Without a
     * track: at the pointer, else where they were.
     */
    fun paste(time: Double? = null, track: Int? = null) {
        val copied = clipboard.clips
        if (copied.isEmpty()) return
        val earliest = copied.minOf { it.start }
        val top = copied.minOf { it.track }
        val bottom = copied.maxOf { it.track }
        val p = pointer?.takeIf { it.x >= plot.x && it.y >= plot.y }
        val at = time ?: p?.let { snapped(timeAt(it.x), snapTargets(emptySet())) } ?: playhead ?: copied.maxOf { it.end }
        val toTrack = track ?: p?.let { trackAt(it.y) } ?: top
        val dt = (at - earliest).coerceAtLeast(-earliest)
        val dRow = (toTrack - top).coerceIn(-top, rows - 1 - bottom)
        val from = clips.toList()
        val before = selection
        update(from + copied.map { it.copy(start = it.start + dt, track = it.track + dRow) })
        selection = (from.size until clips.size).toSet()
        finish(if (copied.size == 1) "Paste clip" else "Paste clips", from, before)
    }

    /** the two clips that [c] is cut into at [at] */
    private fun parts(c: TimelineClip, at: Double): Pair<TimelineClip, TimelineClip> =
        splitClip?.invoke(c, at) ?: (c.copy(duration = at - c.start, fadeOut = 0.0).withFadesClamped() to
                c.copy(start = at, duration = c.end - at, fadeIn = 0.0).withFadesClamped(false))

    /**
     * cuts the selected clips that [time] is in, or all clips it is in when [all] is set or none is selected, in two at
     * [time], those that are long enough to leave both parts [minimumDuration]; the parts after the cut are selected.
     * True when a clip was cut
     */
    fun splitAt(time: Double, all: Boolean = false): Boolean {
        val under = clips.indices.filter { clips[it].start < time && time < clips[it].end }
        return split(if (all || selection.isEmpty()) under else under.filter { it in selection }, time)
    }

    private fun split(indices: Collection<Int>, time: Double): Boolean {
        val cut = indices.filter { time - clips[it].start >= minimumDuration && clips[it].end - time >= minimumDuration }.toSet()
        if (cut.isEmpty()) return false
        val from = clips.toList()
        val before = selection
        val edited = mutableListOf<TimelineClip>()
        val seconds = mutableSetOf<Int>()
        for ((i, c) in from.withIndex()) {
            if (i in cut) {
                val (a, b) = parts(c, time)
                edited.add(a)
                seconds.add(edited.size)
                edited.add(b)
            } else edited.add(c)
        }
        update(edited)
        selection = seconds
        finish(if (cut.size == 1) "Split clip" else "Split clips", from, before)
        return true
    }

    /** the time a razor cuts at for the pointer at [x]: snapped, but not to the edges of the clips [except] it cuts */
    private fun razorTime(x: Double, except: Set<Int>) = snapped(timeAt(x), snapTargets(except))

    private fun press(position: Vector2, button: MouseButton, modifiers: Set<KeyModifier>) {
        val p = local(position)
        val inTracks = p.x >= plot.x && p.y >= plot.y
        if (button == MouseButton.CENTER) {
            gesture = Gesture.Pan(p, viewX, scrollY)
            return
        }
        if (p.y < RULER_HEIGHT && p.x >= plot.x) {
            gesture = Gesture.Scrub
            scrubbed.trigger(timeAt(p.x))
            return
        }
        val hit = clipAt(p)
        if (tool == ClipTimelineTool.RAZOR) {
            gesture = null
            if (!inTracks) return
            if (KeyModifier.SHIFT in modifiers) {
                val time = timeAt(p.x)
                splitAt(razorTime(p.x, clips.indices.filter { clips[it].start < time && time < clips[it].end }.toSet()), all = true)
            } else if (hit != null) {
                split(listOf(hit.first), razorTime(p.x, setOf(hit.first)))
            }
            return
        }
        if (hit == null) {
            if (KeyModifier.ALT in modifiers) {
                gesture = Gesture.Pan(p, viewX, scrollY)
                return
            }
            if (KeyModifier.SHIFT !in modifiers) selection = emptySet()
            gesture = if (inTracks) Gesture.Select(p, selection, p) else null
            return
        }
        val (index, edge) = hit
        val shift = KeyModifier.SHIFT in modifiers
        selection = when {
            shift -> if (index in selection) selection - index else selection + index
            index in selection -> selection
            else -> setOf(index)
        }
        if (index !in selection) {
            gesture = null
            return
        }
        val copy = KeyModifier.ALT in modifiers
        gesture = when {
            edge == 0 || copy -> Gesture.Move(timeAt(p.x), trackAt(p.y), index, clips.toList(), selection, copy)
            abs(edge) == 2 -> Gesture.Fade(index, edge < 0, clips.toList())
            else -> Gesture.Trim(index, edge < 0, clips.toList())
        }
    }

    private fun drag(position: Vector2) {
        val p = local(position)
        when (val g = gesture) {
            is Gesture.Pan -> {
                viewX = g.viewX - (p.x - g.start.x) / zoomX
                scrollY = g.scrollY - (p.y - g.start.y)
            }
            Gesture.Scrub -> scrubbed.trigger(timeAt(p.x).coerceAtLeast(0.0))
            is Gesture.Select -> {
                g.end = p
                val box = Rectangle(minOf(g.start.x, p.x), minOf(g.start.y, p.y), abs(p.x - g.start.x), abs(p.y - g.start.y))
                selection = g.base + clips.indices.filter { clipArea(clips[it]).intersects(box) }
            }
            is Gesture.Move -> {
                val moving = g.moving
                val grabbed = g.from[g.grabbed]
                // the start of the grabbed clip, snapped by its start or by its end
                val raw = grabbed.start + (timeAt(p.x) - g.time)
                val targets = snapTargets(moving)
                fun near(t: Double) = targets.minByOrNull { abs(it - t) }?.takeIf { abs(it - t) * zoomX <= SNAP_DISTANCE }
                val byStart = near(raw)
                val byEnd = near(raw + grabbed.duration)
                // the edge that is closest to a target, else the grid
                val start = when {
                    byStart != null && (byEnd == null || abs(byStart - raw) <= abs(byEnd - raw - grabbed.duration)) -> byStart
                    byEnd != null -> byEnd - grabbed.duration
                    snap > 0.0 -> round(raw / snap) * snap
                    else -> raw
                }
                // the selected clips move together, none to before 0
                val earliest = moving.minOf { g.from[it].start }
                val dt = (start - grabbed.start).coerceAtLeast(-earliest)
                val dTrack = trackAt(p.y) - g.track
                val lowest = moving.minOf { row(g.from[it].track) }
                val highest = moving.maxOf { row(g.from[it].track) }
                val dRow = dTrack.coerceIn(-lowest, rows - 1 - highest)
                fun moved(c: TimelineClip) = c.copy(start = c.start + dt, track = row(c.track) + dRow)
                if (g.copy) {
                    // copies of the selected clips, the originals stay
                    update(g.from + moving.sorted().map { moved(g.from[it]) })
                    selection = (g.from.size until clips.size).toSet()
                } else {
                    update(g.from.mapIndexed { i, c -> if (i in moving) moved(c) else c })
                }
            }
            is Gesture.Trim -> {
                val c = g.from[g.index]
                val t = snapped(timeAt(p.x), snapTargets(setOf(g.index)))
                // the fades stay as long as they were, within the clip, the one at the edge that moves gives way first
                val trimmed = if (g.left) {
                    val start = t.coerceIn(0.0, maxOf(0.0, c.end - minimumDuration))
                    c.copy(start = start, duration = c.end - start).withFadesClamped(false)
                } else {
                    c.copy(duration = maxOf(minimumDuration, t - c.start)).withFadesClamped(true)
                }
                update(g.from.mapIndexed { i, x -> if (i == g.index) trimmed else x })
            }
            is Gesture.Fade -> {
                val c = g.from[g.index]
                // snapped like the rest, also to the start and end of the clip itself
                val t = snapped(timeAt(p.x), snapTargets(setOf(g.index)) + listOf(c.start, c.end))
                val seconds = maxOf(0.0, if (g.fadeIn) t - c.start else c.end - t)
                // the same fade for the selected clips, each within its duration and its other fade
                update(g.from.mapIndexed { i, x ->
                    when {
                        i !in selection -> x
                        g.fadeIn -> x.copy(fadeIn = seconds.coerceIn(0.0, maxOf(0.0, x.duration - x.fadeOut)))
                        else -> x.copy(fadeOut = seconds.coerceIn(0.0, maxOf(0.0, x.duration - x.fadeIn)))
                    }
                })
            }
            null -> {}
        }
    }

    private fun release() {
        when (val g = gesture) {
            is Gesture.Move -> if (g.copy) {
                finish(if (g.moving.size == 1) "Duplicate clip" else "Duplicate clips", g.from, g.moving)
            } else {
                finish(if (selection.size == 1) "Move clip" else "Move clips", g.from)
            }
            is Gesture.Select -> requestRedraw()
            is Gesture.Trim -> finish("Trim clip", g.from)
            is Gesture.Fade -> finish(if (selection.size == 1) "Fade clip" else "Fade clips", g.from)
            else -> {}
        }
        gesture = null
    }

    init {
        mouse.moved.listen {
            val p = local(it.position)
            pointer = p
            // a resize pointer over the edges and the fade handles of clips
            val over = if (tool == ClipTimelineTool.SELECT) clipAt(p) else null
            val hovered = over?.first
            if (hovered != hoveredClip) {
                hoveredClip = hovered
                requestRedraw()
            }
            val cursor = if (over != null && over.second != 0) Cursor.ColResize else Cursor.Default
            if (cursor != shownCursor) {
                shownCursor = cursor
                style { this.cursor = cursor }
            }
            if (tool == ClipTimelineTool.RAZOR) requestRedraw()
        }
        mouse.exited.listen { pointer = null; hoveredClip = null; requestRedraw() }
        mouse.pressed.listen { pointer = local(it.position); press(it.position, it.button, it.modifiers); it.cancelPropagation() }
        mouse.dragged.listen { pointer = local(it.position); drag(it.position); it.cancelPropagation() }
        mouse.released.listen { release() }
        mouse.clicked.listen { it.cancelPropagation() }
        mouse.doubleClicked.listen {
            val p = local(it.position)
            if (p.x >= plot.x && p.y >= plot.y && clipAt(p) == null) {
                val track = trackAt(p.y).coerceIn(0, rows - 1)
                val time = snapped(timeAt(p.x), snapTargets(emptySet())).coerceAtLeast(0.0)
                newClip?.invoke(track, time)?.let { clip ->
                    val from = clips.toList()
                    val before = selection
                    update(from + clip)
                    selection = setOf(clips.lastIndex)
                    finish("Add clip", from, before)
                }
            }
            it.cancelPropagation()
        }
        mouse.scrolled.listen {
            val p = local(it.position)
            if (KeyModifier.SHIFT in it.modifiers) {
                scrollY -= it.rotation.y * TRACK_HEIGHT / 2.0
            } else {
                zoomAround(p.x, 1.1.pow(it.rotation.y))
            }
            // sideways scrolling pans
            if (it.rotation.x != 0.0) viewX -= it.rotation.x * 20.0 / zoomX
            it.cancelPropagation()
        }
        keyboard.pressed.listen { e ->
            val name = e.name.lowercase()
            val mod = KeyModifier.SUPER in e.modifiers || KeyModifier.CTRL in e.modifiers
            when {
                e.key == KEY_DELETE || e.key == KEY_BACKSPACE -> deleteSelected()
                name == "a" && mod -> selection = clips.indices.toSet()
                name == "c" && mod -> copySelected()
                name == "x" && mod -> cutSelected()
                name == "v" && mod -> paste()
                name == "f" && e.modifiers.isEmpty() -> frame()
                e.modifiers.isEmpty() && ClipTimelineTool.entries.any { it.key == name } -> tool = ClipTimelineTool.entries.first { it.key == name }
                else -> return@listen
            }
            e.cancelPropagation()
        }
        contextMenu {
            item("Copy") { copySelected() }
            item("Cut") { cutSelected() }
            item("Paste") { paste() }
            playhead?.let { t -> item("Split at playhead") { splitAt(t) } }
            separator()
            item("Delete clips") { deleteSelected() }
            item("Select all") { selection = clips.indices.toSet() }
            item("Frame all") { frame() }
        }
    }
    //</editor-fold>

    override fun draw(drawer: Drawer) {
        val color = computedStyle.effectiveColor ?: ColorRGBa.WHITE
        val text = panelText ?: return
        val area = plot
        if (area.width <= 0.0 || area.height <= 0.0) return
        val t0 = timeAt(area.x)
        val t1 = timeAt(area.x + area.width)
        fun screenClip(r: Rectangle) = Rectangle(layout.screenX + r.x, layout.screenY + r.y, r.width, r.height).let { c ->
            drawer.drawStyle.clip?.let { o -> intersect(o, c) } ?: c
        }

        // the rows, every other one a little lighter
        drawer.isolated {
            drawer.drawStyle.clip = screenClip(Rectangle(0.0, area.y, layout.screenWidth, area.height))
            drawer.stroke = null
            for (r in 0 until rows) if (r % 2 == 1) {
                drawer.fill = color.opacify(0.03)
                drawer.rectangle(0.0, rowY(r), layout.screenWidth, TRACK_HEIGHT)
            }
            drawer.stroke = color.opacify(0.08)
            drawer.strokeWeight = 1.0
            for (r in 0..rows) drawer.lineSegment(0.0, rowY(r), layout.screenWidth, rowY(r))
        }

        // the ruler and the grid
        val step = niceStep(t1 - t0, (area.width / 80.0).toInt().coerceAtLeast(1))
        drawer.isolated {
            drawer.drawStyle.clip = screenClip(Rectangle(area.x, 0.0, area.width, layout.screenHeight))
            var t = ceil(t0 / step) * step
            while (t <= t1) {
                drawer.stroke = color.opacify(if (abs(t) < step * 1E-6) 0.25 else 0.08)
                drawer.lineSegment(sx(t), RULER_HEIGHT - 6.0, sx(t), layout.screenHeight)
                text.drawLine(drawer, this@ClipTimeline, "t $t", computedStyle, timeLabel(t, step), sx(t) + 3.0, RULER_HEIGHT - 8.0, color.opacify(0.55))
                t += step
            }
        }
        drawer.stroke = color.opacify(0.15)
        drawer.lineSegment(0.0, RULER_HEIGHT, layout.screenWidth, RULER_HEIGHT)
        drawer.lineSegment(HEADER_WIDTH, 0.0, HEADER_WIDTH, layout.screenHeight)

        // the clips
        drawer.isolated {
            drawer.drawStyle.clip = screenClip(area)
            for ((i, clip) in clips.withIndex()) {
                val a = clipArea(clip)
                if (a.x > area.x + area.width || a.x + a.width < area.x) continue
                val fill = clip.color ?: accent.shade(0.75)
                drawer.fill = fill.opacify(if (i in selection) 1.0 else 0.85)
                drawer.stroke = if (i in selection) ColorRGBa.WHITE else fill.shade(0.6)
                drawer.strokeWeight = if (i in selection) 2.0 else 1.0
                drawer.box(a, 4.0)
                // the fades: darker above a ramp from the bottom to the top, those that would overlap meet
                val (fadeIn, fadeOut) = shownFades(clip)
                if (fadeIn > 0.0 || fadeOut > 0.0) {
                    val wIn = fadeIn * zoomX
                    val wOut = fadeOut * zoomX
                    val bottom = a.y + a.height
                    drawer.fill = ColorRGBa.BLACK.opacify(0.45)
                    drawer.stroke = null
                    if (wIn > 0.0) drawer.contour(ShapeContour.fromPoints(listOf(Vector2(a.x, bottom), Vector2(a.x, a.y), Vector2(a.x + wIn, a.y)), true))
                    if (wOut > 0.0) drawer.contour(ShapeContour.fromPoints(listOf(Vector2(a.x + a.width, bottom), Vector2(a.x + a.width - wOut, a.y), Vector2(a.x + a.width, a.y)), true))
                    drawer.stroke = ColorRGBa.WHITE.opacify(0.7)
                    drawer.strokeWeight = 1.0
                    if (wIn > 0.0) drawer.lineSegment(a.x, bottom, a.x + wIn, a.y)
                    if (wOut > 0.0) drawer.lineSegment(a.x + a.width, bottom, a.x + a.width - wOut, a.y)
                }
                // the handles of the fades, on the clip under the pointer and on selected clips
                if ((i == hoveredClip || i in selection) && tool == ClipTimelineTool.SELECT && a.width >= 4.0 * EDGE) {
                    val (h0, h1) = fadeHandles(clip)
                    drawer.fill = ColorRGBa.WHITE
                    drawer.stroke = fill.shade(0.5)
                    drawer.strokeWeight = 1.0
                    for (h in listOf(h0, h1)) drawer.rectangle(h.coerceIn(a.x + 3.0, a.x + a.width - 3.0) - 3.0, a.y + 1.0, 6.0, 6.0)
                }
                if (clip.label.isNotEmpty() && a.width > 14.0) {
                    val light = 0.2126 * fill.r + 0.7152 * fill.g + 0.0722 * fill.b > 0.55
                    val shown = text.ellipsized(computedStyle, clip.label, a.width - 10.0)
                    val visible = Rectangle(maxOf(a.x, area.x) + 5.0, a.y, minOf(a.x + a.width, area.x + area.width) - maxOf(a.x, area.x) - 10.0, a.height)
                    if (visible.width > 4.0) text.drawInBox(drawer, this@ClipTimeline, "clip $i", computedStyle, shown, visible, 0.0, 0.5,
                        if (light) ColorRGBa.BLACK.opacify(0.85) else ColorRGBa.WHITE.opacify(0.92))
                }
            }
        }

        // the names of the tracks
        drawer.isolated {
            drawer.drawStyle.clip = screenClip(Rectangle(0.0, area.y, HEADER_WIDTH, area.height))
            for ((r, name) in tracks.withIndex()) {
                text.drawInBox(drawer, this@ClipTimeline, "track $r", computedStyle, text.ellipsized(computedStyle, name, HEADER_WIDTH - 14.0),
                    Rectangle(8.0, rowY(r), HEADER_WIDTH - 14.0, TRACK_HEIGHT), 0.0, 0.5, color.opacify(0.8))
            }
        }

        // the box that selects, and where the razor cuts
        (gesture as? Gesture.Select)?.let { g ->
            drawer.isolated {
                drawer.drawStyle.clip = screenClip(area)
                drawer.fill = accent.opacify(0.12)
                drawer.stroke = accent
                drawer.strokeWeight = 1.0
                drawer.rectangle(Rectangle(minOf(g.start.x, g.end.x), minOf(g.start.y, g.end.y), abs(g.end.x - g.start.x), abs(g.end.y - g.start.y)))
            }
        }
        pointer?.takeIf { tool == ClipTimelineTool.RAZOR && it.x >= area.x && it.y >= area.y }?.let { p ->
            val hit = clipAt(p)?.first
            val t = razorTime(p.x, setOfNotNull(hit))
            drawer.isolated {
                drawer.drawStyle.clip = screenClip(area)
                drawer.stroke = ColorRGBa.WHITE.opacify(0.8)
                drawer.strokeWeight = 1.0
                val r = row(trackAt(p.y))
                drawer.lineSegment(sx(t), rowY(r), sx(t), rowY(r) + TRACK_HEIGHT)
            }
        }

        // the end and the playhead, through the ruler and the tracks
        drawer.isolated {
            drawer.drawStyle.clip = screenClip(Rectangle(area.x, 0.0, area.width, layout.screenHeight))
            end?.let { e ->
                drawer.stroke = color.opacify(0.6)
                drawer.strokeWeight = 1.0
                var y = 0.0
                while (y < layout.screenHeight) {
                    drawer.lineSegment(sx(e), y, sx(e), minOf(y + 4.0, layout.screenHeight))
                    y += 8.0
                }
            }
            playhead?.let { t ->
                drawer.stroke = accent
                drawer.strokeWeight = 1.5
                drawer.lineSegment(sx(t), 0.0, sx(t), layout.screenHeight)
                drawer.fill = accent
                drawer.stroke = null
                drawer.contour(org.openrndr.shape.ShapeContour.fromPoints(listOf(Vector2(sx(t) - 5.0, 0.0), Vector2(sx(t) + 5.0, 0.0), Vector2(sx(t), 7.0)), true))
            }
        }
    }

    private fun timeLabel(t: Double, step: Double): String {
        val decimals = maxOf(0, -floor(log10(step)).toInt())
        return String.format("%.${decimals}f", t).let { if (it == "-0") "0" else it }
    }

    private fun intersect(a: Rectangle, b: Rectangle): Rectangle {
        val x0 = maxOf(a.x, b.x)
        val y0 = maxOf(a.y, b.y)
        val x1 = minOf(a.x + a.width, b.x + b.width)
        val y1 = minOf(a.y + a.height, b.y + b.height)
        return Rectangle(x0, y0, maxOf(0.0, x1 - x0), maxOf(0.0, y1 - y0))
    }

    override fun close() {
        super.close()
        valueChanged.close()
        selectionChanged.close()
        scrubbed.close()
        toolChanged.close()
    }
}

/** a [ClipTimeline] */
fun Element.clipTimeline(vararg classes: String, init: ClipTimeline.() -> Unit): ClipTimeline = initElement(classes, ClipTimeline(), init)
