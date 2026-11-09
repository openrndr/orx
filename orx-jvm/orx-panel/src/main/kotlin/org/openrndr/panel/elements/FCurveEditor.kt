package org.openrndr.panel.elements

import org.openrndr.KEY_BACKSPACE
import org.openrndr.KEY_DELETE
import org.openrndr.KeyModifier
import org.openrndr.MouseButton
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.isolated
import org.openrndr.events.Event
import org.openrndr.extra.fcurve.FCurve
import org.openrndr.extra.fcurve.scaleTangents
import org.openrndr.extra.shapes.distort.distort
import org.openrndr.math.Vector2
import org.openrndr.panel.Edit
import org.openrndr.panel.binding.PropertyBinding
import org.openrndr.panel.binding.bindValue
import org.openrndr.panel.editHistory
import org.openrndr.panel.style.*
import org.openrndr.shape.Rectangle
import org.openrndr.shape.Segment2D
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.pow
import kotlin.reflect.KMutableProperty0

/** the tools of a [FCurveEditor] */
enum class FCurveTool(val key: String, val title: String) {
    /** selects and moves keys and their handles */
    SELECT("v", "Select"),

    /**
     * adds keys where it is clicked, also to an empty curve: after the end and before the start of the curve it
     * lengthens the curve, on the curve it adds a key at the time and value of the pointer; dragging pulls out the handles
     */
    PEN("p", "Pen"),

    /** draws the curve by hand: the stroke is fit with Bézier segments and replaces the curve over its time */
    DRAW("d", "Draw"),

    /** splits the curve where it is clicked, adding a key */
    CUT("c", "Cut"),

    /** tears the curve apart where it is pressed: dragging makes a jump; alt-click on a jump joins it again */
    BREAK("b", "Break"),

    /** moves the values under the brush with the pointer, less to the edges of the brush */
    GRAB("g", "Grab"),

    /** smooths the values under the brush */
    SMOOTH("s", "Smooth"),

    /** levels the values under the brush to the height of the pointer */
    FLATTEN("l", "Flatten"),

    /** adds noise to the values under the brush */
    NOISE("n", "Noise")
}

/** a part of a key that the pointer can take: the key itself, one side of a jump, or a handle */
private enum class Part { KEY, LEFT, RIGHT, HANDLE_IN, HANDLE_OUT }

/**
 * Edits an [FCurve]: a value over time, made of Bézier segments, with jumps where a segment does not start where the
 * previous one ended. Made for long and complex curves: only the segments in view are drawn and hit, and many
 * segments per pixel are drawn as the range of their values.
 *
 * The view zooms horizontally with the wheel, vertically with shift and the wheel, both with ctrl and the wheel, around
 * the pointer; it pans with the middle button or alt and a drag, and horizontally by scrolling sideways; F frames the
 * whole curve. The tools, see [FCurveTool], are chosen with [tool] or their keys:
 * - select (V): a click selects a key, shift adds, a drag on the background selects with a box; dragging a key moves the
 *   selected keys, dragging a handle turns the segments around it, the other handle follows unless alt is held;
 *   delete removes the selected keys, also the first and the last, and all of them, see [deleteSelected]; mod+A
 *   selects all keys; a double click adds a key; R simplifies the curve between the selected keys, see
 *   [simplifySelection]
 *
 * The context menu sets the interpolation of the selected keys, smooth, linear or constant, see [setInterpolation],
 * and clears the curve, see [clear].
 * - pen (P): a click adds a key, also to an empty curve: after its end or before its start the curve gets longer, on the
 *   curve a key is added at the time and value of the pointer; dragging pulls out the handles of the key
 * - draw (D): a stroke by hand replaces the curve over its time, joining the curve where it starts or ends near it
 * - cut (C): a click splits the curve, adding a key
 * - break (B): a drag on the curve tears it, making a jump; alt and a click on a jump joins it again
 * - grab (G), smooth (S), flatten (L), noise (N): sculpt the values under a brush of [brushRadius] pixels around the
 *   pointer with [brushStrength], less to its edges; [ and ] change the radius. Sculpting only moves values, so the
 *   curve stays a function of time; it is fit again with the distortion of orx-shapes, see [Segment2D.distort], to
 *   [tolerance] pixels, and smoothing does not smooth across jumps
 *
 * A drag is one edit that can be undone, see [org.openrndr.panel.History].
 */
class FCurveEditor : Element(ElementType("fcurve-editor")) {
    override val handlesKeyboardFocus = true
    override val handlesDoubleClick = true

    /** the curve changed, by the user while they edit it, or by the program */
    val valueChanged = Event<ValueChange<FCurve>>("fcurve-editor-value-changed")

    /** the tool changed */
    val toolChanged = Event<FCurveTool>("fcurve-editor-tool-changed")

    private var segments = mutableListOf<Segment2D>()
    private var cached: FCurve? = null

    /** the curve */
    var value: FCurve
        get() = cached ?: FCurve(segments.toList()).also { cached = it }
        set(v) {
            val old = value
            segments = v.segments.toMutableList()
            cached = v
            lone = null
            selection = emptySet()
            requestRedraw()
            valueChanged.trigger(ValueChange(old, v, false))
        }

    var tool = FCurveTool.SELECT
        set(v) {
            if (v != field) {
                field = v
                requestRedraw()
                toolChanged.trigger(v)
            }
        }

    /** the radius of the brushes of the sculpting tools, in pixels */
    var brushRadius = 60.0
        set(v) { field = v.coerceIn(4.0, 2000.0); requestRedraw() }

    /** how strongly the brushes smooth, flatten and add noise, from 0 to 1 */
    var brushStrength = 0.5

    /** the largest distance in pixels between the sculpted curve and the curve that is fit to it */
    var tolerance = 0.25

    /** the time of a playhead that is drawn, null for none */
    var playhead: Double? = null
        set(v) { field = v; requestRedraw() }

    /** the color of the curve, the color of the style when null */
    var curveColor: ColorRGBa? = null

    /** the color of the selection and the brush */
    var accent: ColorRGBa = ColorRGBa.fromHex("#5ab0ff")

    /** the indices of the selected keys: key i is where segment i starts, the last key where the last segment ends */
    var selection: Set<Int> = emptySet()
        private set(v) { field = v; requestRedraw() }

    //<editor-fold desc="the view">
    /** the time at the left edge */
    var viewX = 0.0

    /** the value at the vertical middle */
    var viewY = 0.0

    /** pixels per unit of time */
    var zoomX = 100.0
        set(v) { field = v.coerceIn(1E-6, 1E9) }

    /** pixels per unit of value */
    var zoomY = 100.0
        set(v) { field = v.coerceIn(1E-6, 1E9) }

    private val rulerHeight = 22.0
    private val rulerWidth = 44.0

    /** the part of the element where the curve is drawn, in its own coordinates */
    private val plot: Rectangle get() = Rectangle(rulerWidth, rulerHeight, layout.screenWidth - rulerWidth, layout.screenHeight - rulerHeight)

    private fun sx(x: Double) = plot.x + (x - viewX) * zoomX
    private fun sy(y: Double) = plot.y + plot.height / 2.0 - (y - viewY) * zoomY
    private fun toScreen(p: Vector2) = Vector2(sx(p.x), sy(p.y))
    private fun fromScreen(p: Vector2) = Vector2(viewX + (p.x - plot.x) / zoomX, viewY - (p.y - plot.y - plot.height / 2.0) / zoomY)
    private fun local(p: Vector2) = p - Vector2(layout.screenX, layout.screenY)

    /** the point of the window where the point [p] of the curve, a time and a value, is drawn */
    fun screenPosition(p: Vector2): Vector2 = toScreen(p) + Vector2(layout.screenX, layout.screenY)

    /** the time and value of the curve at [p], a point of the window */
    fun curvePosition(p: Vector2): Vector2 = fromScreen(local(p))

    /** shows the whole curve */
    fun frame() {
        viewAnimation = null
        if (segments.isEmpty() || plot.width <= 0.0) return
        val c = value
        val w = c.duration.takeIf { it > 0.0 } ?: 1.0
        // the values of the curve, between its keys too
        val lo = segments.minOf { it.bounds.y }
        val hi = segments.maxOf { it.bounds.y + it.bounds.height }
        val h = (hi - lo).takeIf { it > 1E-9 } ?: 1.0
        zoomX = plot.width * 0.94 / w
        viewX = c.start - w * 0.03
        zoomY = plot.height * 0.8 / h
        viewY = (lo + hi) / 2.0
        requestRedraw()
    }

    /** the times and values that [showRange] and [showValueRange] are to show once the editor has a size */
    private var pendingRange: ClosedFloatingPointRange<Double>? = null
    private var pendingValueRange: ClosedFloatingPointRange<Double>? = null

    /** shows the times from [t0] to [t1] over the width of the editor, instead of framing the curve */
    fun showRange(t0: Double, t1: Double) {
        require(t1 > t0) { "the range must not be empty" }
        framed = true
        if (plot.width <= 0.0) { pendingRange = t0..t1; return }
        viewAnimation = null
        pendingRange = null
        zoomX = plot.width / (t1 - t0)
        viewX = t0
        requestRedraw()
    }

    /** shows the values from [v0] to [v1] over the height of the editor */
    fun showValueRange(v0: Double, v1: Double) {
        require(v1 > v0) { "the range must not be empty" }
        framed = true
        if (plot.height <= 0.0) { pendingValueRange = v0..v1; return }
        viewAnimation = null
        pendingValueRange = null
        zoomY = plot.height / (v1 - v0)
        viewY = (v0 + v1) / 2.0
        requestRedraw()
    }

    private var framed = false

    /** true when the view widens to show the keys and handles that a drag left outside it, once the drag ends */
    var followEdits = true

    /** the margin around the points that [followEdits] keeps in view, in pixels */
    var followMargin = 24.0

    /** the view that the editor moves to, from the view it started at, over a short time */
    private class ViewAnimation(val start: Double, val from: DoubleArray, val to: DoubleArray)
    private var viewAnimation: ViewAnimation? = null

    /** widens the view, if needed, so that [points] of the curve are in it, [followMargin] from its edges */
    private fun reveal(points: List<Vector2>) {
        if (points.isEmpty() || plot.width <= 0.0 || plot.height <= 0.0) return
        val mx = followMargin / zoomX
        val my = followMargin / zoomY
        val x0 = viewX
        val x1 = viewX + plot.width / zoomX
        val y0 = viewY - plot.height / 2.0 / zoomY
        val y1 = viewY + plot.height / 2.0 / zoomY
        val inside = points.all { it.x in (x0 + mx)..(x1 - mx) && it.y in (y0 + my)..(y1 - my) }
        if (inside) return
        // the view and the points with their margins, whose margins grow with the zoom that shrinks
        val nx0 = minOf(x0, points.minOf { it.x } - mx)
        val nx1 = maxOf(x1, points.maxOf { it.x } + mx)
        val ny0 = minOf(y0, points.minOf { it.y } - my)
        val ny1 = maxOf(y1, points.maxOf { it.y } + my)
        val zx = plot.width / (nx1 - nx0)
        val zy = plot.height / (ny1 - ny0)
        // the margins at the new zoom, around the points that are outside
        val ax0 = minOf(x0, points.minOf { it.x } - followMargin / zx)
        val ax1 = maxOf(x1, points.maxOf { it.x } + followMargin / zx)
        val ay0 = minOf(y0, points.minOf { it.y } - followMargin / zy)
        val ay1 = maxOf(y1, points.maxOf { it.y } + followMargin / zy)
        val to = doubleArrayOf(ax0, (ay0 + ay1) / 2.0, plot.width / (ax1 - ax0), plot.height / (ay1 - ay0))
        viewAnimation = ViewAnimation(System.nanoTime() / 1E9, doubleArrayOf(viewX, viewY, zoomX, zoomY), to)
        requestRedraw()
    }

    /** the next step of the animation of the view */
    private fun animateView() {
        val a = viewAnimation ?: return
        val t = ((System.nanoTime() / 1E9 - a.start) / 0.25).coerceIn(0.0, 1.0)
        val e = 1.0 - (1.0 - t) * (1.0 - t) * (1.0 - t)
        // the zoom changes geometrically, the edges of the view move evenly with it
        zoomX = a.from[2] * Math.pow(a.to[2] / a.from[2], e)
        zoomY = a.from[3] * Math.pow(a.to[3] / a.from[3], e)
        viewX = a.from[0] + (a.to[0] - a.from[0]) * e
        viewY = a.from[1] + (a.to[1] - a.from[1]) * e
        if (t >= 1.0) viewAnimation = null
        requestRedraw()
    }

    override fun prepare() {
        animateView()
        if (layout.screenWidth > rulerWidth && layout.screenHeight > rulerHeight) {
            pendingRange?.let { showRange(it.start, it.endInclusive) }
            pendingValueRange?.let { showValueRange(it.start, it.endInclusive) }
        }
        // the curve is framed once the editor has a size
        if (!framed && layout.screenWidth > 0.0 && segments.isNotEmpty()) {
            framed = true
            frame()
        }
    }

    private fun zoomAround(p: Vector2, fx: Double, fy: Double) {
        viewAnimation = null
        val before = fromScreen(p)
        zoomX *= fx
        zoomY *= fy
        val after = fromScreen(p)
        viewX += before.x - after.x
        viewY += before.y - after.y
        requestRedraw()
    }
    //</editor-fold>

    //<editor-fold desc="the segments">
    /** the index of the first segment that ends after [x] */
    private fun firstAfter(x: Double): Int {
        var lo = 0
        var hi = segments.size
        while (lo < hi) {
            val mid = (lo + hi) / 2
            if (segments[mid].end.x < x) lo = mid + 1 else hi = mid
        }
        return lo
    }

    /** the indices of the segments that overlap [x0] to [x1] */
    private fun overlapping(x0: Double, x1: Double): IntRange {
        val first = firstAfter(x0)
        var last = first
        while (last < segments.size && segments[last].start.x <= x1) last++
        return first until last
    }

    /** the parameter of [s] where its time is [x], by bisection: function shaped segments move forward in time */
    private fun tAt(s: Segment2D, x: Double): Double {
        if (x <= s.start.x) return 0.0
        if (x >= s.end.x) return 1.0
        if (s.linear) return (x - s.start.x) / (s.end.x - s.start.x)
        var lo = 0.0
        var hi = 1.0
        repeat(40) {
            val mid = (lo + hi) / 2.0
            if (s.position(mid).x < x) lo = mid else hi = mid
        }
        return (lo + hi) / 2.0
    }

    private val keyCount get() = if (segments.isEmpty()) 0 else segments.size + 1
    private fun keyX(k: Int) = if (k < segments.size) segments[k].start.x else segments.last().end.x

    /** the point of key [k] at the end of the segment before it, null for the first key */
    private fun leftPoint(k: Int) = segments.getOrNull(k - 1)?.end

    /** the point of key [k] at the start of the segment after it, null for the last key */
    private fun rightPoint(k: Int) = segments.getOrNull(k)?.start

    private fun isJump(k: Int): Boolean {
        val l = leftPoint(k) ?: return false
        val r = rightPoint(k) ?: return false
        return abs(l.y - r.y) * zoomY > 0.5
    }

    private fun changed(interactive: Boolean) {
        val old = cached
        cached = null
        requestRedraw()
        valueChanged.trigger(ValueChange(old ?: value, value, interactive))
    }

    /** the segments with their tangents scaled such that they stay function shaped */
    private fun fix(s: Segment2D) = s.scaleTangents()
    //</editor-fold>

    //<editor-fold desc="the pointer">
    private sealed class Gesture {
        class Box(val start: Vector2, var end: Vector2, val before: Set<Int>) : Gesture()
        class Move(val start: Vector2, val keys: Set<Int>, val part: Part, val minDx: Double, val maxDx: Double) : Gesture()
        class Handle(val key: Int, val part: Part) : Gesture()
        class Tear(val key: Int, val start: Vector2) : Gesture()
        class Sculpt(val start: Vector2, var last: Vector2) : Gesture()
        class Pan(val start: Vector2, val viewX: Double, val viewY: Double) : Gesture()
        /** the pen added [key], the curve with it is [base] */
        class Pen(val key: Int, val start: Vector2, val base: List<Segment2D>) : Gesture()
        class Draw(val points: MutableList<Vector2>, var direction: Int = 0) : Gesture()
    }

    private var gesture: Gesture? = null

    /** the segments as they were when the gesture started, which the edits of the gesture start from */
    private var before: List<Segment2D> = emptyList()
    private var beforeLone: Vector2? = null

    /** the edits start from the curve as it is */
    private fun snapshot() {
        before = segments.toList()
        beforeLone = lone
    }

    /**
     * the only key of a curve that has one key, which has no segments yet; the next key that the pen adds makes the
     * first segment
     */
    private var lone: Vector2? = null

    /** the pointer, in the coordinates of the element */
    private var pointer: Vector2? = null

    private fun near(a: Vector2, b: Vector2, d: Double = 7.0) = a.distanceTo(b) < d

    /** the key and its part under [p], in the coordinates of the element; handles first, of the selected keys */
    private fun partAt(p: Vector2): Pair<Int, Part>? {
        val t = fromScreen(p).x
        // the handles of the selected keys around the pointer: a handle reaches at most a segment away from its key
        if (selection.isNotEmpty()) {
            val around = overlapping(t - 1.0 / zoomX * plot.width, t + 1.0 / zoomX * plot.width)
            for (k in (around.first..(around.last + 1))) {
                if (k !in selection) continue
                handleIn(k)?.let { if (near(toScreen(it), p)) return k to Part.HANDLE_IN }
                handleOut(k)?.let { if (near(toScreen(it), p)) return k to Part.HANDLE_OUT }
            }
        }
        val tolerance = 7.0 / zoomX
        val range = overlapping(t - tolerance, t + tolerance)
        val candidates = (range.first..(range.last + 1)).filter { it in 0 until keyCount && abs(keyX(it) - t) <= tolerance }
        for (k in candidates) {
            val l = leftPoint(k)
            val r = rightPoint(k)
            if (isJump(k)) {
                if (l != null && near(toScreen(l), p)) return k to Part.LEFT
                if (r != null && near(toScreen(r), p)) return k to Part.RIGHT
            } else {
                val point = r ?: l ?: continue
                if (near(toScreen(point), p)) return k to Part.KEY
            }
        }
        return null
    }

    private fun handleIn(k: Int): Vector2? = segments.getOrNull(k - 1)?.let { it.cubic.control[1] }
    private fun handleOut(k: Int): Vector2? = segments.getOrNull(k)?.let { it.cubic.control[0] }

    /** the distance in pixels from [p] to the curve at its time, and the index of that segment */
    private fun curveAt(p: Vector2): Pair<Int, Double>? {
        val x = fromScreen(p).x
        val i = firstAfter(x).takeIf { it < segments.size && segments[it].start.x <= x } ?: return null
        val s = segments[i]
        val y = sy(s.position(tAt(s, x)).y)
        return i to abs(y - p.y)
    }

    /** splits segment [i] at time [x], the index of the new key; null when [x] is at a key */
    private fun split(i: Int, x: Double): Int? {
        val s = segments[i]
        if ((x - s.start.x) * zoomX < 2.0 || (s.end.x - x) * zoomX < 2.0) return null
        val parts = s.split(tAt(s, x))
        if (parts.size != 2) return null
        segments[i] = fix(parts[0])
        segments.add(i + 1, fix(parts[1]))
        return i + 1
    }

    private fun key(k: Int, dx: Double, dyLeft: Double, dyRight: Double) {
        segments.getOrNull(k - 1)?.let { s ->
            val c = s.cubic
            segments[k - 1] = c.copy(control = listOf(c.control[0], c.control[1] + Vector2(dx, dyLeft)), end = c.end + Vector2(dx, dyLeft))
        }
        segments.getOrNull(k)?.let { s ->
            val c = s.cubic
            segments[k] = c.copy(start = c.start + Vector2(dx, dyRight), control = listOf(c.control[0] + Vector2(dx, dyRight), c.control[1]))
        }
    }

    init {
        mouse.moved.listen {
            pointer = local(it.position)
            if (tool.ordinal >= FCurveTool.GRAB.ordinal || tool == FCurveTool.PEN) requestRedraw()
        }
        mouse.exited.listen { pointer = null; requestRedraw() }
        mouse.pressed.listen { press(it.position, it.button, it.modifiers); it.cancelPropagation() }
        mouse.dragged.listen { drag(it.position, it.modifiers); it.cancelPropagation() }
        mouse.released.listen { release() }
        // the view that the user moves stops the animation of the view
        mouse.pressed.listen { viewAnimation = null }
        mouse.clicked.listen { it.cancelPropagation() }
        mouse.doubleClicked.listen {
            // a double click adds a key on the curve
            val p = local(it.position)
            if (tool == FCurveTool.SELECT) curveAt(p)?.let { (i, d) -> if (d < 8.0) split(i, fromScreen(p).x)?.let { k -> selection = setOf(k); changed(true); record("Add key") } }
            it.cancelPropagation()
        }
        mouse.scrolled.listen {
            val p = local(it.position)
            val f = 1.1.pow(it.rotation.y)
            when {
                KeyModifier.CTRL in it.modifiers || KeyModifier.SUPER in it.modifiers -> zoomAround(p, f, f)
                KeyModifier.SHIFT in it.modifiers -> zoomAround(p, 1.0, f)
                else -> zoomAround(p, f, 1.0)
            }
            // sideways scrolling pans
            if (it.rotation.x != 0.0) { viewX -= it.rotation.x * 20.0 / zoomX; requestRedraw() }
            it.cancelPropagation()
        }
        contextMenu {
            item("Smooth keys") { setInterpolation(Interpolation.SMOOTH) }
            item("Linear keys") { setInterpolation(Interpolation.LINEAR) }
            item("Constant keys") { setInterpolation(Interpolation.CONSTANT) }
            separator()
            item("Simplify selection") { simplifySelection() }
            item("Select all") { selectAll() }
            item("Frame all") { frame() }
            separator()
            item("Delete keys") { deleteSelected() }
            item("Clear curve") { clear() }
        }
        keyboard.pressed.listen { e ->
            val name = e.name.lowercase()
            when {
                e.key == KEY_DELETE || e.key == KEY_BACKSPACE -> deleteSelected()
                name == "a" && (KeyModifier.SUPER in e.modifiers || KeyModifier.CTRL in e.modifiers) -> selectAll()
                name == "f" && e.modifiers.isEmpty() -> frame()
                name == "r" && e.modifiers.isEmpty() -> simplifySelection()
                name == "[" -> brushRadius /= 1.2
                name == "]" -> brushRadius *= 1.2
                e.modifiers.isEmpty() && FCurveTool.entries.any { it.key == name } -> tool = FCurveTool.entries.first { it.key == name }
                else -> return@listen
            }
            e.cancelPropagation()
        }
    }

    private fun press(position: Vector2, button: MouseButton, modifiers: Set<KeyModifier>) {
        val p = local(position)
        pointer = p
        snapshot()
        if (button == MouseButton.CENTER || KeyModifier.ALT in modifiers && tool != FCurveTool.BREAK && tool != FCurveTool.SELECT) {
            gesture = Gesture.Pan(p, viewX, viewY)
            return
        }
        if (p.x < plot.x || p.y < plot.y) {
            gesture = Gesture.Pan(p, viewX, viewY)
            return
        }
        if (segments.isEmpty() && tool != FCurveTool.PEN && tool != FCurveTool.DRAW) {
            gesture = null
            return
        }
        val shift = KeyModifier.SHIFT in modifiers
        gesture = when (tool) {
            FCurveTool.SELECT -> {
                val hit = partAt(p)
                when {
                    hit == null -> {
                        if (KeyModifier.ALT in modifiers) Gesture.Pan(p, viewX, viewY)
                        else {
                            if (!shift) selection = emptySet()
                            Gesture.Box(p, p, selection)
                        }
                    }
                    hit.second == Part.HANDLE_IN || hit.second == Part.HANDLE_OUT -> Gesture.Handle(hit.first, hit.second)
                    else -> {
                        val k = hit.first
                        selection = when {
                            shift -> if (k in selection) selection - k else selection + k
                            k in selection -> selection
                            else -> setOf(k)
                        }
                        // the selected keys move together, between the keys that are not selected
                        var minDx = Double.NEGATIVE_INFINITY
                        var maxDx = Double.POSITIVE_INFINITY
                        val gap = 1.0 / zoomX
                        for (s in selection) {
                            if (s - 1 >= 0 && s - 1 !in selection) minDx = maxOf(minDx, keyX(s - 1) - keyX(s) + gap)
                            if (s + 1 < keyCount && s + 1 !in selection) maxDx = minOf(maxDx, keyX(s + 1) - keyX(s) - gap)
                        }
                        Gesture.Move(p, selection, if (selection.size == 1) hit.second else Part.KEY, minDx, maxDx)
                    }
                }
            }
            FCurveTool.PEN -> penPress(fromScreen(p), p)
            FCurveTool.DRAW -> Gesture.Draw(mutableListOf(p))
            FCurveTool.CUT -> {
                curveAt(p)?.takeIf { it.second < 10.0 }?.let { (i, _) -> split(i, fromScreen(p).x)?.let { k -> selection = setOf(k); changed(true); record("Cut curve") } }
                null
            }
            FCurveTool.BREAK -> {
                val hit = partAt(p)
                if (KeyModifier.ALT in modifiers) {
                    // a jump is joined at the middle
                    hit?.first?.takeIf { isJump(it) }?.let { k ->
                        val mid = (leftPoint(k)!!.y + rightPoint(k)!!.y) / 2.0
                        key(k, 0.0, mid - leftPoint(k)!!.y, mid - rightPoint(k)!!.y)
                        changed(true)
                        record("Join curve")
                    }
                    null
                } else {
                    val k = hit?.first?.takeIf { it in 1 until keyCount - 1 }
                        ?: curveAt(p)?.takeIf { it.second < 10.0 }?.let { (i, _) -> split(i, fromScreen(p).x) }
                    if (k != null) { selection = setOf(k); changed(true); Gesture.Tear(k, p) } else null
                }
            }
            else -> Gesture.Sculpt(p, p).also { sculptStep(p, p, first = true) }
        }
        requestRedraw()
    }

    private fun drag(position: Vector2, modifiers: Set<KeyModifier>) {
        val p = local(position)
        pointer = p
        when (val g = gesture) {
            is Gesture.Pan -> {
                viewX = g.viewX - (p.x - g.start.x) / zoomX
                viewY = g.viewY + (p.y - g.start.y) / zoomY
                requestRedraw()
            }
            is Gesture.Box -> {
                g.end = p
                val a = fromScreen(Vector2(minOf(g.start.x, p.x), maxOf(g.start.y, p.y)))
                val b = fromScreen(Vector2(maxOf(g.start.x, p.x), minOf(g.start.y, p.y)))
                val inside = mutableSetOf<Int>()
                val range = overlapping(a.x, b.x)
                for (k in range.first..(range.last + 1)) {
                    if (k !in 0 until keyCount) continue
                    val x = keyX(k)
                    if (x < a.x || x > b.x) continue
                    if (listOfNotNull(leftPoint(k), rightPoint(k)).any { it.y in a.y..b.y }) inside.add(k)
                }
                selection = g.before + inside
            }
            is Gesture.Move -> {
                segments = before.toMutableList()
                val d = fromScreen(p) - fromScreen(g.start)
                val dx = d.x.coerceIn(g.minDx, g.maxDx)
                for (k in g.keys) {
                    when (g.part) {
                        Part.LEFT -> key(k, dx, d.y, 0.0)
                        Part.RIGHT -> key(k, dx, 0.0, d.y)
                        else -> key(k, dx, d.y, d.y)
                    }
                }
                for (k in g.keys) {
                    segments.getOrNull(k - 1)?.let { segments[k - 1] = fix(it) }
                    segments.getOrNull(k)?.let { segments[k] = fix(it) }
                }
                changed(true)
            }
            is Gesture.Handle -> {
                segments = before.toMutableList()
                moveHandle(g.key, g.part, fromScreen(p), KeyModifier.ALT !in modifiers)
                changed(true)
            }
            is Gesture.Tear -> {
                // the side after the key follows the pointer, making a jump
                segments = before.toMutableList()
                val dy = (fromScreen(p) - fromScreen(g.start)).y
                key(g.key, 0.0, 0.0, dy)
                segments.getOrNull(g.key)?.let { segments[g.key] = fix(it) }
                changed(true)
            }
            is Gesture.Sculpt -> {
                sculptStep(g.start, p, first = false, last = g.last)
                g.last = p
            }
            is Gesture.Pen -> {
                // the handles of the new key follow the pointer, mirrored
                if (p.distanceTo(g.start) > 3.0) {
                    segments = g.base.toMutableList()
                    pullHandles(g.key, fromScreen(p) - fromScreen(g.start))
                    changed(true)
                }
            }
            is Gesture.Draw -> {
                // a stroke goes one way in time
                val last = g.points.last()
                if (g.direction == 0 && abs(p.x - g.points.first().x) > 3.0) g.direction = if (p.x > g.points.first().x) 1 else -1
                if (g.direction != 0 && (p.x - last.x) * g.direction > 0.5) g.points.add(p)
                requestRedraw()
            }
            null -> {}
        }
    }

    /** the points that [g] moved: the keys with both their sides, or the handle */
    private fun movedPoints(g: Gesture?): List<Vector2> = when (g) {
        is Gesture.Move -> g.keys.flatMap { listOfNotNull(leftPoint(it), rightPoint(it)) }
        is Gesture.Tear -> listOfNotNull(leftPoint(g.key), rightPoint(g.key))
        is Gesture.Pen -> listOfNotNull(leftPoint(g.key), rightPoint(g.key), segments.getOrNull(g.key - 1)?.cubic?.control?.get(1), segments.getOrNull(g.key)?.cubic?.control?.get(0))
        is Gesture.Handle -> listOfNotNull(
            if (g.part == Part.HANDLE_OUT) segments.getOrNull(g.key)?.cubic?.control?.get(0) else segments.getOrNull(g.key - 1)?.cubic?.control?.get(1)
        )
        else -> emptyList()
    }

    private fun release() {
        // the points that were dragged out of the view come into it again
        if (followEdits) reveal(movedPoints(gesture))
        (gesture as? Gesture.Draw)?.let { finishStroke(it) }
        when (gesture) {
            is Gesture.Pan, is Gesture.Box, is Gesture.Draw, null -> {}
            else -> record(when (gesture) {
                is Gesture.Move -> "Move keys"
                is Gesture.Handle -> "Move handle"
                is Gesture.Tear -> "Break curve"
                is Gesture.Sculpt -> "${tool.title} curve"
                is Gesture.Pen -> "Add key"
                else -> "Edit curve"
            })
        }
        gesture = null
        requestRedraw()
    }

    /** the edit from [before] to the segments as they are, which can be undone */
    private fun record(name: String) {
        val from = before
        val fromLone = beforeLone
        val to = segments.toList()
        val toLone = lone
        if (from == to && fromLone == toLone) return
        editHistory?.record(Edit(name, null,
            { segments = from.toMutableList(); lone = fromLone; selection = emptySet(); changed(false) },
            { segments = to.toMutableList(); lone = toLone; selection = emptySet(); changed(false) }))
        before = to
        beforeLone = toLone
    }

    private fun moveHandle(k: Int, part: Part, to: Vector2, mirror: Boolean) {
        val x = keyX(k)
        val jump = isJump(k)
        if (part == Part.HANDLE_OUT) {
            val s = segments[k].cubic
            val c0 = Vector2(to.x.coerceIn(x, s.end.x), to.y)
            segments[k] = fix(s.copy(control = listOf(c0, s.control[1])))
            // the handle on the other side turns along, keeping its length on the screen
            if (mirror && !jump && k > 0) {
                val p = segments[k - 1].cubic
                val dir = toScreen(s.start) - toScreen(c0)
                val length = toScreen(p.control[1]).distanceTo(toScreen(p.end))
                if (dir.length > 1E-6) {
                    val c1 = fromScreen(toScreen(p.end) + dir.normalized * length)
                    segments[k - 1] = fix(p.copy(control = listOf(p.control[0], c1)))
                }
            }
        } else {
            val s = segments[k - 1].cubic
            val c1 = Vector2(to.x.coerceIn(s.start.x, x), to.y)
            segments[k - 1] = fix(s.copy(control = listOf(s.control[0], c1)))
            if (mirror && !jump && k < segments.size) {
                val n = segments[k].cubic
                val dir = toScreen(s.end) - toScreen(c1)
                val length = toScreen(n.control[0]).distanceTo(toScreen(n.start))
                if (dir.length > 1E-6) {
                    val c0 = fromScreen(toScreen(n.start) + dir.normalized * length)
                    segments[k] = fix(n.copy(control = listOf(c0, n.control[1])))
                }
            }
        }
    }

    /**
     * Removes the selected keys: the segments around a key in the curve become one, the first and the last key take the
     * segment to the next key with them, and removing all keys leaves an empty curve
     */
    fun deleteSelected() {
        if (selection.isEmpty() && lone == null) return
        snapshot()
        if (segments.isEmpty()) {
            lone = null
        } else {
            val kept = (0 until keyCount).filter { it !in selection }
            val rebuilt = mutableListOf<Segment2D>()
            for ((a, b) in kept.zipWithNext()) {
                if (b == a + 1) {
                    rebuilt.add(segments[a])
                } else {
                    // the handles are lengthened in proportion to the longer segment
                    val first = segments[a].cubic
                    val last = segments[b - 1].cubic
                    val w = last.end.x - first.start.x
                    val c0 = first.start + (first.control[0] - first.start) * (w / (first.end.x - first.start.x).coerceAtLeast(1E-12))
                    val c1 = last.end + (last.control[1] - last.end) * (w / (last.end.x - last.start.x).coerceAtLeast(1E-12))
                    rebuilt.add(fix(Segment2D(first.start, c0, c1, last.end)))
                }
            }
            lone = if (kept.size == 1) (rightPoint(kept[0]) ?: leftPoint(kept[0])) else null
            segments = rebuilt
        }
        selection = emptySet()
        changed(true)
        record("Remove keys")
    }

    /** removes the whole curve */
    fun clear() {
        snapshot()
        segments = mutableListOf()
        lone = null
        selection = emptySet()
        changed(true)
        record("Clear curve")
    }

    /** selects all keys */
    fun selectAll() {
        selection = (0 until keyCount).toSet()
    }
    //</editor-fold>

    //<editor-fold desc="building curves">
    /** the pen at [c], a point of the curve, and [p], on the screen: a key is added, see [FCurveTool.PEN] */
    private fun penPress(c: Vector2, p: Vector2): Gesture? {
        val first = lone
        if (segments.isEmpty()) {
            // the first key, then the first segment
            if (first == null) {
                lone = c
                selection = emptySet()
                changed(true)
                record("Add key")
                return null
            }
            if (abs(c.x - first.x) * zoomX < 3.0) return null
            val (a, b) = if (c.x > first.x) first to c else c to first
            segments = mutableListOf(Segment2D(a, a + (b - a) / 3.0, b - (b - a) / 3.0, b))
            lone = null
            val k = if (c.x > first.x) 1 else 0
            autoTangent(0); autoTangent(1)
            selection = setOf(k)
            changed(true)
            return Gesture.Pen(k, p, segments.toList())
        }
        val start = segments.first().start
        val end = segments.last().end
        val margin = 3.0 / zoomX
        val k = when {
            // after the end the curve gets longer, the last key continues smoothly
            c.x > end.x + margin -> {
                segments.add(Segment2D(end, end + (c - end) / 3.0, c - (c - end) / 3.0, c))
                autoTangent(keyCount - 2, inHandle = false)
                autoTangent(keyCount - 1)
                keyCount - 1
            }
            c.x < start.x - margin -> {
                segments.add(0, Segment2D(c, c + (start - c) / 3.0, start - (start - c) / 3.0, start))
                autoTangent(1, outHandle = false)
                autoTangent(0)
                0
            }
            else -> {
                // a key at the time and value of the pointer
                val i = firstAfter(c.x).coerceAtMost(segments.lastIndex)
                val added = split(i, c.x) ?: return null
                val l = leftPoint(added)!!
                key(added, 0.0, c.y - l.y, c.y - l.y)
                autoTangent(added)
                added
            }
        }
        selection = setOf(k)
        changed(true)
        return Gesture.Pen(k, p, segments.toList())
    }

    /** the handles of key [k] point along [d], in the coordinates of the curve, and back, forward in time */
    private fun pullHandles(k: Int, d: Vector2) {
        val forward = if (d.x < 0.0) -d else d
        segments.getOrNull(k)?.let { s -> val c = s.cubic; segments[k] = fix(c.copy(control = listOf(c.start + forward, c.control[1]))) }
        segments.getOrNull(k - 1)?.let { s -> val c = s.cubic; segments[k - 1] = fix(c.copy(control = listOf(c.control[0], c.end - forward))) }
    }

    /**
     * Gives key [k] automatic handles, like a smooth key: a third of the segments long, along the slope between the
     * keys around it, and flat where the key is higher or lower than both, and at the ends and the sides of jumps
     */
    private fun autoTangent(k: Int, inHandle: Boolean = true, outHandle: Boolean = true) {
        val l = leftPoint(k)
        val r = rightPoint(k)
        val here = r ?: l ?: return
        val prev = segments.getOrNull(k - 1)?.start
        val next = segments.getOrNull(k)?.end
        val slope = if (prev != null && next != null && !isJump(k) && (here.y - prev.y) * (next.y - here.y) > 0.0)
            (next.y - prev.y) / (next.x - prev.x) else 0.0
        if (inHandle) segments.getOrNull(k - 1)?.let { s ->
            val c = s.cubic
            val w = (c.end.x - c.start.x) / 3.0
            segments[k - 1] = fix(c.copy(control = listOf(c.control[0], c.end - Vector2(w, slope * w))))
        }
        if (outHandle) segments.getOrNull(k)?.let { s ->
            val c = s.cubic
            val w = (c.end.x - c.start.x) / 3.0
            segments[k] = fix(c.copy(control = listOf(c.start + Vector2(w, slope * w), c.control[1])))
        }
    }

    /** the time of a key at [x]: the curve is split there when [x] is inside a segment, a key near it is taken */
    private fun cutAt(x: Double): Double {
        if (segments.isEmpty() || x <= segments.first().start.x || x >= segments.last().end.x) return x
        val i = firstAfter(x).coerceAtMost(segments.lastIndex)
        val k = split(i, x) ?: return listOf(segments[i].start.x, segments[i].end.x).minBy { abs(it - x) }
        return keyX(k)
    }

    /** the stroke of the draw tool becomes the curve over its time */
    private fun finishStroke(g: Gesture.Draw) {
        val points = if (g.direction < 0) g.points.reversed() else g.points.toList()
        if (points.size < 3 || abs(points.last().x - points.first().x) < 4.0) return
        val fitted = try {
            org.openrndr.extra.shapes.fit.fitCubicBeziers(points, 1.5, cornerAngle = 70.0)
        } catch (e: IllegalArgumentException) {
            return
        }.map { f -> fix(Segment2D(fromScreen(f.start), f.cubic.control.map(::fromScreen), fromScreen(f.end))) }.toMutableList()
        if (fitted.isEmpty()) return
        if (segments.isEmpty()) {
            segments = fitted
            lone = null
            selection = (0..fitted.size).toSet()
        } else {
            val x0 = cutAt(fitted.first().start.x)
            val x1 = cutAt(fitted.last().end.x)
            fitted[0] = fitted[0].copy(start = fitted[0].start.copy(x = x0))
            fitted[fitted.lastIndex] = fitted.last().copy(end = fitted.last().end.copy(x = x1))
            val left = segments.filter { it.end.x <= x0 + 1E-9 }
            val right = segments.filter { it.start.x >= x1 - 1E-9 }
            // the stroke joins the curve where it starts or ends near it, a jump remains elsewhere
            left.lastOrNull()?.let { s ->
                val dy = s.end.y - fitted[0].start.y
                if (abs(dy) * zoomY < 12.0) fitted[0].cubic.let { c -> fitted[0] = c.copy(start = c.start + Vector2(0.0, dy), control = listOf(c.control[0] + Vector2(0.0, dy), c.control[1])) }
            }
            right.firstOrNull()?.let { s ->
                val dy = s.start.y - fitted.last().end.y
                if (abs(dy) * zoomY < 12.0) fitted.last().cubic.let { c -> fitted[fitted.lastIndex] = c.copy(end = c.end + Vector2(0.0, dy), control = listOf(c.control[0], c.control[1] + Vector2(0.0, dy))) }
            }
            segments = (left + fitted + right).toMutableList()
            selection = (left.size..left.size + fitted.size).toSet()
        }
        changed(true)
        record("Draw curve")
    }

    /** the interpolation of the segments after the selected keys, see [setInterpolation] */
    enum class Interpolation {
        /** curved, with automatic handles on the keys */
        SMOOTH,

        /** straight to the next key */
        LINEAR,

        /** the value of the key until the next key, where the curve jumps */
        CONSTANT
    }

    /** gives the segments after the selected keys [interpolation]; smooth gives the keys automatic handles */
    fun setInterpolation(interpolation: Interpolation) {
        if (selection.isEmpty()) return
        snapshot()
        for (k in selection.sorted()) {
            val s = segments.getOrNull(k)
            when (interpolation) {
                Interpolation.SMOOTH -> autoTangent(k)
                Interpolation.LINEAR -> if (s != null) segments[k] = Segment2D(s.start, s.end)
                Interpolation.CONSTANT -> if (s != null) segments[k] = Segment2D(s.start, Vector2(s.end.x, s.start.y))
            }
        }
        changed(true)
        record("${interpolation.name.lowercase().replaceFirstChar { it.uppercase() }} keys")
    }
    //</editor-fold>

    /** the largest distance in pixels between the curve and the curve that [simplifySelection] fits to it */
    var simplifyTolerance = 1.0

    /**
     * Fits the curve between the selected keys again with as few Bézier segments as stay within [tolerance] pixels of
     * it, at the zoom of the view: every stretch of selected keys that follow each other is fit on its own, jumps and
     * kinks of more than [cornerAngle] degrees stay; the keys at the ends of a stretch keep their places
     */
    fun simplifySelection(tolerance: Double = simplifyTolerance, cornerAngle: Double = 20.0) {
        if (selection.size < 2) return
        snapshot()
        // the stretches of selected keys that follow each other, as ranges of segments, from the last
        val keys = selection.sorted()
        val stretches = mutableListOf<IntRange>()
        var first = keys[0]
        for (i in 1..keys.size) {
            if (i == keys.size || keys[i] != keys[i - 1] + 1) {
                val last = keys[i - 1]
                if (last > first) stretches.add(first until last)
                if (i < keys.size) first = keys[i]
            }
        }
        val newKeys = mutableSetOf<Int>()
        var shift = 0
        for (stretch in stretches) {
            val from = stretch.first + shift
            val to = stretch.last + shift
            // jumps are not fit over
            val parts = mutableListOf<IntRange>()
            var start = from
            for (i in from + 1..to + 1) {
                if (i == to + 1 || abs(segments[i].start.y - segments[i - 1].end.y) > 1E-9) {
                    parts.add(start until i)
                    start = i
                }
            }
            val fitted = mutableListOf<Segment2D>()
            for (part in parts) fitted.addAll(refit(segments.subList(part.first, part.last + 1), tolerance, cornerAngle))
            val removed = to - from + 1
            for (i in 0 until removed) segments.removeAt(from)
            segments.addAll(from, fitted)
            for (k in from..from + fitted.size) newKeys.add(k)
            shift += fitted.size - removed
        }
        if (stretches.isEmpty()) return
        selection = newKeys
        changed(true)
        record("Simplify curve")
    }

    /**
     * [part], a continuous run of segments, with fewer keys, see [simplifySelection]: on the screen, the keys from one
     * key to as far as possible are replaced by one segment that leaves and arrives in the directions of the curve
     * there, of which the lengths of the handles are fit by least squares to samples of the curve, as long as its values
     * stay within [tolerance] pixels of the curve. Kinks of more than [cornerAngle] degrees stay keys.
     */
    private fun refit(part: List<Segment2D>, tolerance: Double, cornerAngle: Double): List<Segment2D> {
        if (part.size < 2) return part
        val screen = part.map { s -> Segment2D(toScreen(s.start), s.cubic.control.map(::toScreen), toScreen(s.end)).cubic }
        // samples of every segment, a few pixels apart
        val samples = screen.map { s -> List((s.length / 2.0).toInt().coerceIn(8, 400) + 1) { i -> s.position(i / ((s.length / 2.0).toInt().coerceIn(8, 400)).toDouble()) } }
        fun outTangent(i: Int): Vector2 = screen[i].let { s -> (s.control[0] - s.start).takeIf { it.length > 1E-9 } ?: (s.end - s.start) }.normalized
        fun inTangent(i: Int): Vector2 = screen[i].let { s -> (s.control[1] - s.end).takeIf { it.length > 1E-9 } ?: (s.start - s.end) }.normalized

        /** one segment from the start of segment [i] to the end of segment [j - 1], and how far its values miss */
        fun fitRange(i: Int, j: Int): Pair<Segment2D, Double> {
            val p0 = screen[i].start
            val p3 = screen[j - 1].end
            val t0 = outTangent(i)
            val t1 = inTangent(j - 1)
            val points = (i until j).flatMap { k -> if (k == i) samples[k] else samples[k].drop(1) }
            val width = p3.x - p0.x
            var u = DoubleArray(points.size) { ((points[it].x - p0.x) / width).coerceIn(0.0, 1.0) }
            var fitted = Segment2D(p0, p0 + (p3 - p0) / 3.0, p3 - (p3 - p0) / 3.0, p3)
            repeat(4) {
                var c11 = 0.0; var c12 = 0.0; var c22 = 0.0; var x1 = 0.0; var x2 = 0.0
                for ((k, q) in points.withIndex()) {
                    val t = u[k]
                    val b0 = (1 - t) * (1 - t) * (1 - t); val b1 = 3 * t * (1 - t) * (1 - t); val b2 = 3 * t * t * (1 - t); val b3 = t * t * t
                    val a1 = t0 * b1
                    val a2 = t1 * b2
                    val r = q - p0 * (b0 + b1) - p3 * (b2 + b3)
                    c11 += a1.dot(a1); c12 += a1.dot(a2); c22 += a2.dot(a2)
                    x1 += r.dot(a1); x2 += r.dot(a2)
                }
                val det = c11 * c22 - c12 * c12
                val chord = p0.distanceTo(p3) / 3.0
                var a = if (abs(det) > 1E-12) (x1 * c22 - x2 * c12) / det else chord
                var b = if (abs(det) > 1E-12) (c11 * x2 - c12 * x1) / det else chord
                if (a <= 1E-6 || b <= 1E-6) { a = chord; b = chord }
                fitted = Segment2D(p0, p0 + t0 * a, p3 + t1 * b, p3).scaleTangents()
                // the samples move to the parameters of the fit at their times
                u = DoubleArray(points.size) { k -> tAt(fitted, points[k].x) }
            }
            // the largest difference of the values, at the times of the samples
            var worst = 0.0
            for (q in points) worst = maxOf(worst, abs(fitted.position(tAt(fitted, q.x)).y - q.y))
            return fitted to worst
        }

        // from every key that stays, as many keys as one segment fits are left out
        fun recurse(i: Int, j: Int): List<Segment2D> {
            val result = mutableListOf<Segment2D>()
            var from = i
            while (from < j) {
                var best: Segment2D = screen[from]
                var to = from + 1
                var misses = 0
                var next = from + 2
                // a few longer ranges are tried after one that misses, a fit can get better further on
                while (next <= j && misses < 3) {
                    val (fitted, worst) = fitRange(from, next)
                    if (worst <= tolerance) { best = fitted; to = next; misses = 0 } else misses++
                    next++
                }
                result.add(best)
                from = to
            }
            return result
        }

        // kinks stay keys
        val kinks = (1 until part.size).filter { k ->
            val a = -inTangent(k - 1)
            val b = outTangent(k)
            Math.toDegrees(kotlin.math.acos(a.dot(b).coerceIn(-1.0, 1.0))) > cornerAngle
        }
        val bounds = listOf(0) + kinks + part.size
        val fitted = bounds.zipWithNext().flatMap { (i, j) -> recurse(i, j) }
        if (fitted.size >= part.size) return part
        val result = fitted.map { f -> fix(Segment2D(fromScreen(f.start), f.cubic.control.map(::fromScreen), fromScreen(f.end))) }.toMutableList()
        // the keys that stay keep their exact places
        result[0] = result[0].copy(start = part.first().start)
        result[result.lastIndex] = result.last().copy(end = part.last().end)
        return result
    }

    //<editor-fold desc="sculpting">
    /** the weight of the brush at [x] for a brush at [center], 1 in the middle to 0 at its edge */
    private fun falloff(x: Double, center: Double): Double {
        val r = brushRadius / zoomX
        val d = abs(x - center) / r
        return if (d >= 1.0) 0.0 else 0.5 + 0.5 * cos(d * PI)
    }

    /** the continuous runs of the segments: index ranges without jumps between their segments */
    private fun runs(from: List<Segment2D>): List<IntRange> {
        val result = mutableListOf<IntRange>()
        var start = 0
        for (i in 1..from.size) {
            if (i == from.size || abs(from[i].start.y - from[i - 1].end.y) > 1E-9) {
                result.add(start until i)
                start = i
            }
        }
        return result
    }

    /** the value of the run [range] of [from] at [x], held at its ends */
    private fun valueIn(from: List<Segment2D>, range: IntRange, x: Double): Double {
        if (x <= from[range.first].start.x) return from[range.first].start.y
        if (x >= from[range.last].end.x) return from[range.last].end.y
        var lo = range.first
        var hi = range.last
        while (lo < hi) {
            val mid = (lo + hi) / 2
            if (from[mid].end.x < x) lo = mid + 1 else hi = mid
        }
        val s = from[lo]
        return s.position(tAt(s, x)).y
    }

    private fun noise(x: Double): Double {
        fun hash(i: Long): Double {
            var h = i * 0x9E3779B97F4A7C15uL.toLong()
            h = (h xor (h ushr 31)) * 0xBF58476D1CE4E5B9uL.toLong()
            return ((h ushr 11).toDouble() / (1L shl 53).toDouble()) * 2.0 - 1.0
        }
        val i = floor(x).toLong()
        val f = x - floor(x)
        val u = f * f * (3.0 - 2.0 * f)
        return hash(i) * (1.0 - u) + hash(i + 1) * u
    }

    private var noiseSeed = 0.0

    /** one step of the sculpting tool for the pointer at [p], having started at [start] */
    private fun sculptStep(start: Vector2, p: Vector2, first: Boolean, last: Vector2? = null) {
        val center = fromScreen(p).x
        val r = brushRadius / zoomX
        when (tool) {
            FCurveTool.GRAB -> {
                // from the curve as it was, so that grabbing does not add up errors
                val center0 = fromScreen(start).x
                val dy = (fromScreen(p) - fromScreen(start)).y
                segments = before.toMutableList()
                if (dy != 0.0) sculpt(center0 - r, center0 + r) { x, y, _ -> y + dy * falloff(x, center0) }
            }
            FCurveTool.SMOOTH -> {
                // the values move toward their average over half the brush, within their run
                val from = segments.toList()
                val window = r * 0.5
                val k = brushStrength * 0.5
                sculpt(center - r, center + r) { x, y, run ->
                    var sum = 0.0
                    var weights = 0.0
                    for (j in -6..6) {
                        val o = j / 6.0
                        val w = exp(-o * o * 3.0)
                        sum += valueIn(from, run, x + o * window) * w
                        weights += w
                    }
                    y + (sum / weights - y) * k * falloff(x, center)
                }
            }
            FCurveTool.FLATTEN -> {
                val target = fromScreen(p).y
                sculpt(center - r, center + r) { x, y, _ -> y + (target - y) * brushStrength * 0.3 * falloff(x, center) }
            }
            FCurveTool.NOISE -> {
                if (first) noiseSeed += 1000.0
                // the noise has features of a tenth of the brush, as tall as a fifth of the brush on the screen
                val amplitude = brushRadius * 0.2 / zoomY * brushStrength * 0.25
                val moved = last?.let { abs(it.x - p.x) + abs(it.y - p.y) } ?: 1.0
                if (moved > 0.0 || first) sculpt(center - r, center + r) { x, y, _ ->
                    y + noise(noiseSeed + x / (r * 0.1)) * amplitude * falloff(x, center)
                }
            }
            else -> {}
        }
        changed(true)
    }

    /**
     * Moves the values of the segments that overlap [x0] to [x1] to those that [displace] gives for their times and
     * values and the runs they are in: the segments are distorted on the screen, so that they are fit to [tolerance]
     * pixels, and their tangents are scaled to keep them function shaped
     */
    private fun sculpt(x0: Double, x1: Double, displace: (x: Double, y: Double, run: IntRange) -> Double) {
        val range = overlapping(x0, x1)
        if (range.isEmpty()) return
        val from = segments.toList()
        val runOf = IntArray(from.size)
        val runList = runs(from)
        for ((ri, run) in runList.withIndex()) for (i in run) runOf[i] = ri
        val result = mutableListOf<Segment2D>()
        for (i in range) {
            val s = from[i]
            val run = runList[runOf[i]]
            val screen = Segment2D(toScreen(s.start), s.cubic.control.map(::toScreen), toScreen(s.end))
            val pieces = screen.distort(tolerance, 6, 16) { q, _ ->
                val c = fromScreen(q)
                Vector2(q.x, sy(displace(c.x, c.y, run)))
            }
            result.addAll(pieces.map { piece ->
                fix(Segment2D(fromScreen(piece.start), piece.control.map(::fromScreen), fromScreen(piece.end)))
            })
        }
        // the keys at the ends of the sculpted range keep their times exactly
        val firstStart = from[range.first].start.x
        val lastEnd = from[range.last].end.x
        if (result.isNotEmpty()) {
            result[0] = result[0].copy(start = result[0].start.copy(x = firstStart))
            result[result.lastIndex] = result.last().copy(end = result.last().end.copy(x = lastEnd))
        }
        val next = from.subList(0, range.first) + result + from.subList(range.last + 1, from.size)
        segments = next.toMutableList()
        // the keys inside the sculpted range are new
        selection = selection.filter { it <= range.first || it > range.last }.map { if (it > range.last) it + result.size - range.count() else it }.toSet()
    }
    //</editor-fold>

    override fun draw(drawer: Drawer) {
        val color = computedStyle.effectiveColor ?: ColorRGBa.WHITE
        val text = panelText ?: return
        val area = plot
        val w = layout.screenWidth
        val h = layout.screenHeight
        val x0 = fromScreen(Vector2(area.x, 0.0)).x
        val x1 = fromScreen(Vector2(area.x + area.width, 0.0)).x
        val yTop = fromScreen(Vector2(0.0, area.y)).y
        val yBottom = fromScreen(Vector2(0.0, area.y + area.height)).y

        // the grid and the rulers
        drawer.strokeWeight = 1.0
        val stepX = niceStep((x1 - x0), (area.width / 90.0).toInt().coerceAtLeast(1))
        val stepY = niceStep((yTop - yBottom), (area.height / 50.0).toInt().coerceAtLeast(1))
        var gx = ceil(x0 / stepX) * stepX
        while (gx <= x1) {
            drawer.stroke = color.opacify(if (abs(gx) < stepX * 1E-6) 0.25 else 0.08)
            drawer.lineSegment(sx(gx), area.y, sx(gx), area.y + area.height)
            text.drawLine(drawer, this, "x $gx", computedStyle, label(gx, stepX), sx(gx) + 3.0, rulerHeight - 7.0, color.opacify(0.55))
            gx += stepX
        }
        var gy = ceil(yBottom / stepY) * stepY
        while (gy <= yTop) {
            drawer.stroke = color.opacify(if (abs(gy) < stepY * 1E-6) 0.25 else 0.08)
            drawer.lineSegment(area.x, sy(gy), area.x + area.width, sy(gy))
            text.drawInBox(drawer, this, "y $gy", computedStyle, label(gy, stepY), Rectangle(0.0, sy(gy) - 10.0, rulerWidth - 6.0, 20.0), 1.0, 0.5, color.opacify(0.55))
            gy += stepY
        }

        drawer.isolated {
            drawer.drawStyle.clip = Rectangle(layout.screenX + area.x, layout.screenY + area.y, area.width, area.height).let { c ->
                drawer.drawStyle.clip?.let { o -> intersect(o, c) } ?: c
            }
            val visible = overlapping(x0, x1)
            val curve = curveColor ?: color
            // the brush, under the curve
            val p = pointer
            if (p != null && tool.ordinal >= FCurveTool.GRAB.ordinal && p.x >= area.x) {
                val center = fromScreen(p).x
                drawer.stroke = null
                drawer.fill = accent.opacify(0.07)
                drawer.rectangle(p.x - brushRadius, area.y, brushRadius * 2.0, area.height)
                drawer.stroke = accent.opacify(0.6)
                drawer.fill = null
                drawer.lineStrip(List(41) { i ->
                    val x = p.x - brushRadius + i / 40.0 * brushRadius * 2.0
                    Vector2(x, area.y + area.height - 4.0 - falloff(fromScreen(Vector2(x, 0.0)).x, center) * 40.0)
                })
                drawer.lineSegment(p.x, area.y, p.x, area.y + area.height)
            }
            if (!visible.isEmpty()) {
                drawer.fill = null
                drawer.stroke = curve
                drawer.strokeWeight = 1.5
                if (visible.count() > area.width * 2.0) {
                    // more segments than pixels: the range of the values in every column
                    val columns = area.width.toInt()
                    val lo = DoubleArray(columns) { Double.POSITIVE_INFINITY }
                    val hi = DoubleArray(columns) { Double.NEGATIVE_INFINITY }
                    for (i in visible) {
                        val s = segments[i]
                        val c0 = ((sx(s.start.x) - area.x).toInt()).coerceIn(0, columns - 1)
                        val c1 = ((sx(s.end.x) - area.x).toInt()).coerceIn(0, columns - 1)
                        val b = s.bounds
                        for (c in c0..c1) {
                            lo[c] = minOf(lo[c], sy(b.y + b.height)); hi[c] = maxOf(hi[c], sy(b.y))
                        }
                    }
                    drawer.lineSegments((0 until columns).filter { lo[it] <= hi[it] }.flatMap { c ->
                        listOf(Vector2(area.x + c + 0.5, lo[c]), Vector2(area.x + c + 0.5, hi[c] + 0.5))
                    })
                } else {
                    drawer.segments(visible.map { i ->
                        val s = segments[i]
                        Segment2D(toScreen(s.start), s.control.map(::toScreen), toScreen(s.end))
                    })
                    // the jumps, dotted
                    drawer.strokeWeight = 1.0
                    drawer.stroke = curve.opacify(0.35)
                    for (i in visible) if (i > 0 && isJump(i)) {
                        val a = toScreen(segments[i - 1].end)
                        val b = toScreen(segments[i].start)
                        val n = (abs(b.y - a.y) / 6.0).toInt()
                        drawer.lineSegments((0 until n step 2).flatMap { j ->
                            listOf(Vector2(a.x, a.y + (b.y - a.y) * j / n), Vector2(a.x, a.y + (b.y - a.y) * (j + 1) / n))
                        })
                    }
                    // the keys, when they are not too close together, and the selected keys; in one draw for each color
                    val keys = (visible.first..(visible.last + 1)).filter { it in 0 until keyCount }
                    val showKeys = keys.size < area.width / 6.0
                    val plain = mutableListOf<Rectangle>()
                    val chosen = mutableListOf<Rectangle>()
                    val selectedInView = mutableListOf<Int>()
                    for (k in keys) {
                        val selected = k in selection
                        if (selected) selectedInView.add(k)
                        if (!showKeys && !selected) continue
                        val l = leftPoint(k)
                        val r = rightPoint(k)
                        for (pt in if (l != null && r != null && l == r) listOf(l) else listOfNotNull(l, r)) {
                            val q = toScreen(pt)
                            (if (selected) chosen else plain).add(Rectangle(q.x - 3.5, q.y - 3.5, 7.0, 7.0))
                        }
                    }
                    drawer.stroke = null
                    if (plain.isNotEmpty()) { drawer.fill = curve; drawer.rectangles(plain) }
                    if (chosen.isNotEmpty()) { drawer.fill = accent; drawer.rectangles(chosen) }
                    // the handles of the selected keys in view, when they are not too close together
                    if (selectedInView.size < area.width / 12.0) {
                        val lines = mutableListOf<Vector2>()
                        val ends = mutableListOf<Vector2>()
                        for (k in selectedInView) {
                            val l = leftPoint(k)
                            val r = rightPoint(k)
                            if (l != null) handleIn(k)?.let { h -> lines.add(toScreen(l)); lines.add(toScreen(h)); ends.add(toScreen(h)) }
                            if (r != null) handleOut(k)?.let { h -> lines.add(toScreen(r)); lines.add(toScreen(h)); ends.add(toScreen(h)) }
                        }
                        if (lines.isNotEmpty()) {
                            drawer.strokeWeight = 1.0
                            drawer.stroke = accent.opacify(0.7)
                            drawer.lineSegments(lines)
                            drawer.stroke = null
                            drawer.fill = accent
                            drawer.circles(ends, 4.0)
                        }
                    }
                }
            }
            // the only key of a curve without segments
            lone?.let { k ->
                val q = toScreen(k)
                drawer.stroke = null
                drawer.fill = accent
                drawer.rectangle(q.x - 3.5, q.y - 3.5, 7.0, 7.0)
            }
            // where the pen adds the next key: after the end, before the start, or of a curve without segments
            if (tool == FCurveTool.PEN && p != null && p.x >= area.x && p.y >= area.y) {
                val t = fromScreen(p).x
                val from = lone ?: segments.lastOrNull()?.end?.takeIf { t > it.x } ?: segments.firstOrNull()?.start?.takeIf { t < it.x }
                drawer.stroke = accent.opacify(0.6)
                drawer.strokeWeight = 1.0
                from?.let { f ->
                    val a = toScreen(f)
                    val n = (a.distanceTo(p) / 6.0).toInt()
                    drawer.lineSegments((0 until n step 2).flatMap { j -> listOf(a + (p - a) * (j.toDouble() / n), a + (p - a) * ((j + 1.0) / n)) })
                }
                drawer.fill = null
                drawer.rectangle(p.x - 3.5, p.y - 3.5, 7.0, 7.0)
            }
            // the stroke that is drawn
            (gesture as? Gesture.Draw)?.let { g ->
                drawer.stroke = accent
                drawer.strokeWeight = 1.5
                drawer.fill = null
                if (g.points.size > 1) drawer.lineStrip(g.points)
            }
            // the playhead
            playhead?.let { t ->
                drawer.stroke = ColorRGBa.fromHex("#f88379")
                drawer.strokeWeight = 1.5
                drawer.lineSegment(sx(t), area.y, sx(t), area.y + area.height)
            }
            // the box that selects
            (gesture as? Gesture.Box)?.let { g ->
                drawer.fill = accent.opacify(0.12)
                drawer.stroke = accent
                drawer.strokeWeight = 1.0
                drawer.rectangle(Rectangle(minOf(g.start.x, g.end.x), minOf(g.start.y, g.end.y), abs(g.end.x - g.start.x), abs(g.end.y - g.start.y)))
            }
        }
        // how to start a curve
        if (segments.isEmpty()) {
            val hint = if (lone == null) "An empty curve: add keys with the pen (P), or draw the curve (D)" else "Add the next key with the pen (P)"
            text.drawInBox(drawer, this, "hint", computedStyle, hint, Rectangle(area.x, area.y + area.height / 2.0 + 16.0, area.width, 24.0), 0.5, 0.5, color.opacify(0.6))
        }
        // the tool and the number of segments
        val status = "${tool.title}  ·  ${segments.size} segments" + if (tool.ordinal >= FCurveTool.GRAB.ordinal) "  ·  radius ${brushRadius.toInt()}" else ""
        text.drawInBox(drawer, this, "status", computedStyle, status, Rectangle(area.x, area.y + area.height - 22.0, area.width - 8.0, 22.0), 1.0, 0.5, color.opacify(0.5))
        drawer.stroke = color.opacify(0.15)
        drawer.lineSegment(rulerWidth, rulerHeight, w, rulerHeight)
        drawer.lineSegment(rulerWidth, rulerHeight, rulerWidth, h)
    }

    private fun label(v: Double, step: Double): String {
        val decimals = maxOf(0, -floor(kotlin.math.log10(step)).toInt())
        return String.format("%.${decimals}f", v).let { if (it == "-0") "0" else it }
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
        toolChanged.close()
    }
}

/** binds the curve of the editor to [property], both ways, see [bindValue] */
fun FCurveEditor.bind(property: KMutableProperty0<FCurve>): PropertyBinding<FCurve> =
    bindValue(valueChanged, property, { value }, { value = it })

fun Element.fcurveEditor(vararg classes: String, init: FCurveEditor.() -> Unit): FCurveEditor = initElement(classes, FCurveEditor(), init)

