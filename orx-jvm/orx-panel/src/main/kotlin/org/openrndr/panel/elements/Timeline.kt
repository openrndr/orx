package org.openrndr.panel.elements

import org.openrndr.KEY_BACKSPACE
import org.openrndr.KEY_DELETE
import org.openrndr.KEY_SPACEBAR
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.events.Event
import org.openrndr.extra.style.TimingFunction
import org.openrndr.math.Vector2
import org.openrndr.panel.style.*
import org.openrndr.shape.Rectangle
import org.openrndr.shape.contour
import kotlin.reflect.KMutableProperty0

/** a keyframe of a [Track]: its [value] at [time], in seconds */
data class Keyframe(val time: Double, val value: Double)

/**
 * A track of a [Timeline]: [name], the [keyframes] of a value in [range], interpolated with [easing] between them, and
 * [apply], which sets the value, like a property it is bound to
 */
class Track(
    val name: String,
    var range: ClosedRange<Double>,
    var easing: TimingFunction = TimingFunction.LINEAR,
    val apply: ((Double) -> Unit)? = null,
    val current: (() -> Double)? = null
) {
    /** the keyframes, in the order of their times */
    var keyframes: List<Keyframe> = emptyList()
        set(value) {
            field = value.sortedBy { it.time }
        }

    /** the value at [time]: that of the keyframes around it, interpolated; null without keyframes */
    fun valueAt(time: Double): Double? {
        val k = keyframes
        if (k.isEmpty()) return null
        if (time <= k.first().time) return k.first().value
        if (time >= k.last().time) return k.last().value
        val i = k.indexOfFirst { it.time >= time }
        val a = k[i - 1]
        val b = k[i]
        val t = if (b.time > a.time) easing.ease((time - a.time) / (b.time - a.time)) else 1.0
        return a.value + (b.value - a.value) * t
    }
}

/**
 * A timeline of [tracks] of keyframes over [duration] seconds, with a playhead at [time]. Tracks bound to properties,
 * see [track], set them when the playhead moves: while [playing], and while the pointer scrubs the ruler at the top.
 * Keyframes are dragged in time and value, a double click in a track adds one of the value the track has, delete
 * removes the selected one; space plays and pauses.
 */
class Timeline : Element(ElementType("timeline")) {
    override val handlesDoubleClick = true
    override val handlesKeyboardFocus = true

    val tracks = mutableListOf<Track>()

    /** the length in seconds */
    var duration = 10.0

    /** the time of the playhead in seconds */
    var time = 0.0
        set(v) {
            val clean = if (loop && playing) ((v % duration) + duration) % duration else v.coerceIn(0.0, duration)
            if (clean != field) {
                field = clean
                draw.dirty = true
                applyTracks()
                timeChanged.trigger(clean)
            }
        }

    /** true while the playhead moves on with time */
    var playing = false
        set(v) {
            field = v
            lastFrame = null
            draw.dirty = true
        }

    /** true when the playhead goes back to the start at the end */
    var loop = true

    val timeChanged = Event<Double>("timeline-time-changed")

    /** the keyframes of a track changed */
    val keyframesChanged = Event<Track>("timeline-keyframes-changed")

    private val rulerHeight = 22.0
    private val trackHeight = 30.0
    private val nameWidth = 110.0
    private var lastFrame: Long? = null
    private var selected: Pair<Track, Keyframe>? = null
    private var scrubbing = false

    override val heightHint: Double get() = rulerHeight + tracks.size * trackHeight

    /** adds a track bound to [property], in [range], which the timeline sets */
    fun track(name: String, property: KMutableProperty0<Double>, range: ClosedRange<Double>, easing: TimingFunction = TimingFunction.LINEAR): Track =
        track(name, range, easing, { property.get() }) { property.set(it) }

    /** adds a track in [range] of which the value is [current] and that [apply] sets */
    fun track(name: String, range: ClosedRange<Double>, easing: TimingFunction = TimingFunction.LINEAR, current: (() -> Double)? = null, apply: (Double) -> Unit): Track =
        Track(name, range, easing, apply, current).also { tracks.add(it); requestLayout() }

    private fun applyTracks() {
        for (track in tracks) track.valueAt(time)?.let { v -> track.apply?.invoke(v) }
    }

    // the playhead moves on with the time of the program
    override fun prepare() {
        if (!playing) return
        val now = System.nanoTime()
        lastFrame?.let { time += (now - it) / 1E9 }
        lastFrame = now
        if (!loop && time >= duration) playing = false
    }

    private val lane: Rectangle get() = Rectangle(nameWidth, rulerHeight, layout.screenWidth - nameWidth - 8.0, tracks.size * trackHeight)
    private fun xOf(t: Double) = lane.x + t / duration * lane.width
    private fun timeAt(x: Double) = ((x - lane.x) / lane.width * duration).coerceIn(0.0, duration)
    private fun yOf(track: Int, v: Double): Double {
        val r = tracks[track].range
        val f = ((v - r.start) / (r.endInclusive - r.start)).coerceIn(0.0, 1.0)
        return rulerHeight + track * trackHeight + trackHeight - 5.0 - f * (trackHeight - 10.0)
    }
    private fun valueAt(track: Int, y: Double): Double {
        val r = tracks[track].range
        val f = ((rulerHeight + track * trackHeight + trackHeight - 5.0 - y) / (trackHeight - 10.0)).coerceIn(0.0, 1.0)
        return r.start + f * (r.endInclusive - r.start)
    }

    private fun local(p: Vector2) = p - Vector2(layout.screenX, layout.screenY)

    private fun replace(track: Track, old: Keyframe?, new: Keyframe?) {
        track.keyframes = track.keyframes.filter { it !== old && it != old } + listOfNotNull(new)
        selected = new?.let { track to it }
        draw.dirty = true
        applyTracks()
        keyframesChanged.trigger(track)
    }

    init {
        mouse.pressed.listen {
            val p = local(it.position)
            scrubbing = p.y < rulerHeight
            if (scrubbing) {
                time = timeAt(p.x)
            } else {
                val index = ((p.y - rulerHeight) / trackHeight).toInt()
                val track = tracks.getOrNull(index)
                selected = track?.keyframes?.firstOrNull { k -> p.distanceTo(Vector2(xOf(k.time), yOf(index, k.value))) < 8.0 }?.let { k -> track to k }
                draw.dirty = true
            }
            it.cancelPropagation()
        }
        mouse.dragged.listen {
            val p = local(it.position)
            if (scrubbing) {
                time = timeAt(p.x)
            } else selected?.let { (track, keyframe) ->
                replace(track, keyframe, Keyframe(timeAt(p.x), valueAt(tracks.indexOf(track), p.y)))
            }
            it.cancelPropagation()
        }
        mouse.clicked.listen { scrubbing = false }
        mouse.doubleClicked.listen {
            val p = local(it.position)
            if (p.y < rulerHeight || p.x < lane.x) return@listen
            val index = ((p.y - rulerHeight) / trackHeight).toInt()
            val track = tracks.getOrNull(index) ?: return@listen
            // a double click on a keyframe keeps it selected
            if (track.keyframes.any { k -> p.distanceTo(Vector2(xOf(k.time), yOf(index, k.value))) < 8.0 }) return@listen
            val t = timeAt(p.x)
            replace(track, null, Keyframe(t, track.valueAt(t) ?: track.current?.invoke() ?: valueAt(index, p.y)))
            it.cancelPropagation()
        }
        keyboard.pressed.listen {
            when (it.key) {
                KEY_SPACEBAR -> playing = !playing
                KEY_DELETE, KEY_BACKSPACE -> selected?.let { (track, keyframe) -> replace(track, keyframe, null) }
                else -> return@listen
            }
            it.cancelPropagation()
        }
    }

    override fun draw(drawer: Drawer) {
        val color = computedStyle.effectiveColor ?: ColorRGBa.WHITE
        val text = panelText ?: return
        val l = lane
        // the ruler, with a tick every second
        drawer.stroke = color.opacify(0.3)
        drawer.strokeWeight = 1.0
        val seconds = duration.toInt()
        val every = maxOf(1, seconds / 10)
        for (s in 0..seconds) {
            val x = xOf(s.toDouble())
            drawer.lineSegment(x, rulerHeight - if (s % every == 0) 8.0 else 4.0, x, rulerHeight)
            if (s % every == 0) text.drawLine(drawer, this, "tick $s", computedStyle, "${s}s", x + 3.0, rulerHeight - 9.0, color.opacify(0.6))
        }
        for ((i, track) in tracks.withIndex()) {
            val top = rulerHeight + i * trackHeight
            drawer.stroke = null
            drawer.fill = color.opacify(if (i % 2 == 0) 0.05 else 0.03)
            drawer.rectangle(0.0, top, layout.screenWidth, trackHeight)
            text.drawInBox(drawer, this, "name $i", computedStyle, text.ellipsized(computedStyle, track.name, nameWidth - 16.0),
                Rectangle(8.0, top, nameWidth - 16.0, trackHeight), 0.0, 0.5, color)
            // the value between the keyframes, then the keyframes
            if (track.keyframes.isNotEmpty()) {
                drawer.fill = null
                drawer.stroke = color.opacify(0.5)
                drawer.strokeWeight = 1.5
                val steps = 100
                drawer.lineStrip(List(steps + 1) { s ->
                    val t = duration * s / steps
                    Vector2(xOf(t), yOf(i, track.valueAt(t)!!))
                })
            }
            for (k in track.keyframes) {
                val c = Vector2(xOf(k.time), yOf(i, k.value))
                val isSelected = selected?.let { it.first === track && it.second == k } == true
                drawer.fill = if (isSelected) ColorRGBa.fromHex("#5ab0ff") else color
                drawer.stroke = null
                drawer.contour(contour { moveTo(c.x, c.y - 5.0); lineTo(c.x + 5.0, c.y); lineTo(c.x, c.y + 5.0); lineTo(c.x - 5.0, c.y); close() })
            }
        }
        // the playhead
        drawer.stroke = ColorRGBa.fromHex("#f88379")
        drawer.strokeWeight = 1.5
        drawer.lineSegment(xOf(time), 0.0, xOf(time), l.y + l.height)
        drawer.fill = ColorRGBa.fromHex("#f88379")
        drawer.stroke = null
        drawer.contour(contour { moveTo(xOf(time) - 5.0, 0.0); lineTo(xOf(time) + 5.0, 0.0); lineTo(xOf(time), 7.0); close() })
        if (playing) draw.dirty = true
    }
}

fun Element.timeline(vararg classes: String, init: Timeline.() -> Unit): Timeline = initElement(classes, Timeline(), init)
