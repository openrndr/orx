package org.openrndr.panel.elements

import org.openrndr.KEY_ARROW_DOWN
import org.openrndr.KEY_ARROW_LEFT
import org.openrndr.KEY_ARROW_RIGHT
import org.openrndr.KEY_ARROW_UP
import org.openrndr.KEY_BACKSPACE
import org.openrndr.KEY_DELETE
import org.openrndr.KeyModifier
import org.openrndr.color.ColorRGBa
import org.openrndr.color.Linearity
import org.openrndr.draw.Drawer
import org.openrndr.draw.LineCap
import org.openrndr.events.Event
import org.openrndr.extra.color.spaces.ColorOKLABa
import org.openrndr.extra.style.TimingFunction
import org.openrndr.math.Vector2
import org.openrndr.panel.style.*
import org.openrndr.shape.Rectangle
import org.openrndr.shape.contour

/** a cubic Bézier easing curve from (0, 0) to (1, 1), with the control points ([x1], [y1]) and ([x2], [y2]) */
data class CubicBezier(val x1: Double, val y1: Double, val x2: Double, val y2: Double) {
    /** the curve as a timing function, see [TimingFunction.cubicBezier] */
    val timingFunction: TimingFunction get() = TimingFunction.cubicBezier(x1, y1, x2, y2)

    /** the eased value at [t] */
    fun ease(t: Double) = timingFunction.ease(t)

    /** the curve as CSS, like `cubic-bezier(0.25, 0.1, 0.25, 1)` */
    fun toCss() = "cubic-bezier(${f(x1)}, ${f(y1)}, ${f(x2)}, ${f(y2)})"

    private fun f(v: Double) = String.format("%.3f", v).trimEnd('0').trimEnd('.').ifEmpty { "0" }

    companion object {
        val EASE = CubicBezier(0.25, 0.1, 0.25, 1.0)
        val LINEAR = CubicBezier(0.0, 0.0, 1.0, 1.0)
    }
}

/**
 * An editor of a [CubicBezier] easing curve: the pointer drags its two handles, the x of a handle stays from 0 to 1,
 * its y can overshoot; with the keyboard focus the arrow keys move the handle that moved last, finer with shift.
 */
class CurveEditor : Element(ElementType("curve-editor")) {
    override val handlesKeyboardFocus = true

    var label: String by attribute("label", "")

    val valueChanged = Event<ValueChange<CubicBezier>>("curve-editor-value-changed")

    private var interactive = false

    var value: CubicBezier = CubicBezier.EASE
        set(v) {
            val clean = CubicBezier(v.x1.coerceIn(0.0, 1.0), v.y1.coerceIn(-0.5, 1.5), v.x2.coerceIn(0.0, 1.0), v.y2.coerceIn(-0.5, 1.5))
            if (clean != field) {
                val old = field
                field = clean
                draw.dirty = true
                valueChanged.trigger(ValueChange(old, clean, interactive))
            }
        }

    /** the handle that moves, 1 or 2 */
    private var handle = 1

    /** the square the curve is drawn in, with room for overshoot above and below */
    private val area: Rectangle
        get() {
            val labelHeight = 20.0
            val size = minOf(layout.screenWidth, layout.screenHeight - labelHeight) - 8.0
            return Rectangle((layout.screenWidth - size) / 2.0, 4.0 + size * 0.15, size, size * 0.7)
        }

    private fun toScreen(p: Vector2) = Vector2(area.x + p.x * area.width, area.y + (1.0 - p.y) * area.height)
    private fun fromScreen(p: Vector2) = Vector2((p.x - area.x) / area.width, 1.0 - (p.y - area.y) / area.height)

    private fun move(handle: Int, p: Vector2) {
        interactive = true
        value = if (handle == 1) value.copy(x1 = p.x, y1 = p.y) else value.copy(x2 = p.x, y2 = p.y)
        interactive = false
    }

    init {
        mouse.pressed.listen {
            val local = it.position - Vector2(layout.screenX, layout.screenY)
            val d1 = local.distanceTo(toScreen(Vector2(value.x1, value.y1)))
            val d2 = local.distanceTo(toScreen(Vector2(value.x2, value.y2)))
            handle = if (d1 <= d2) 1 else 2
            move(handle, fromScreen(local))
            it.cancelPropagation()
        }
        mouse.dragged.listen {
            move(handle, fromScreen(it.position - Vector2(layout.screenX, layout.screenY)))
            it.cancelPropagation()
        }
        keyboard.pressed.listen {
            val step = if (KeyModifier.SHIFT in it.modifiers) 0.001 else 0.01
            val p = if (handle == 1) Vector2(value.x1, value.y1) else Vector2(value.x2, value.y2)
            val d = when (it.key) {
                KEY_ARROW_LEFT -> Vector2(-step, 0.0)
                KEY_ARROW_RIGHT -> Vector2(step, 0.0)
                KEY_ARROW_UP -> Vector2(0.0, step)
                KEY_ARROW_DOWN -> Vector2(0.0, -step)
                else -> return@listen
            }
            move(handle, p + d)
            it.cancelPropagation()
        }
    }

    override fun draw(drawer: Drawer) {
        val color = computedStyle.effectiveColor ?: ColorRGBa.WHITE
        val a = area
        drawer.fill = color.opacify(0.05)
        drawer.stroke = color.opacify(0.15)
        drawer.strokeWeight = 1.0
        drawer.rectangle(a)
        drawer.lineSegment(toScreen(Vector2.ZERO), toScreen(Vector2.ONE))
        val p0 = toScreen(Vector2.ZERO)
        val p1 = toScreen(Vector2(value.x1, value.y1))
        val p2 = toScreen(Vector2(value.x2, value.y2))
        val p3 = toScreen(Vector2.ONE)
        // the handles
        drawer.stroke = color.opacify(0.5)
        drawer.lineSegment(p0, p1)
        drawer.lineSegment(p3, p2)
        // the curve
        drawer.fill = null
        drawer.stroke = color
        drawer.strokeWeight = 2.5
        drawer.lineCap = LineCap.ROUND
        drawer.contour(contour { moveTo(p0); curveTo(p1, p2, p3) })
        drawer.stroke = null
        for ((i, p) in listOf(p1, p2).withIndex()) {
            drawer.fill = if (i + 1 == handle && ElementPseudoClass("focus-visible") in pseudoClasses) ColorRGBa.fromHex("#5ab0ff") else color
            drawer.circle(p, 5.0)
        }
        panelText?.drawInBox(drawer, this, "label", computedStyle, if (label.isEmpty()) value.toCss() else "$label  ${value.toCss()}",
            Rectangle(0.0, layout.screenHeight - 20.0, layout.screenWidth, 20.0), 0.5, 0.5, color.opacify(0.8))
    }
}

/** the color space the stops of a gradient are interpolated in */
enum class GradientSpace { SRGB, LINEAR_RGB, OKLAB }

/** a stop of a gradient: its [position] from 0 to 1 and its [color] */
data class ColorStop(val position: Double, val color: ColorRGBa)

/**
 * An editor of a gradient of color stops, interpolated in [space]: a bar of the gradient with the stops under it. The
 * pointer drags a stop along the bar, dragging it far below the bar removes it; a double click on the bar adds a stop
 * of the color there. The color of the stop that is selected is edited in the color picker under the bar; with the
 * keyboard focus, left and right move it, delete removes it.
 */
class GradientEditor : Div(ElementType("gradient-editor")) {
    override val handlesKeyboardFocus = true

    val valueChanged = Event<ValueChange<List<ColorStop>>>("gradient-editor-value-changed")

    private var interactive = false

    /** the stops, in the order of their positions */
    var value: List<ColorStop> = listOf(ColorStop(0.0, ColorRGBa.BLACK), ColorStop(1.0, ColorRGBa.WHITE))
        set(v) {
            val clean = v.map { it.copy(position = it.position.coerceIn(0.0, 1.0)) }.sortedBy { it.position }
            if (clean != field) {
                val old = field
                field = clean
                bar.draw.dirty = true
                valueChanged.trigger(ValueChange(old, clean, interactive))
            }
        }

    /** the color space the stops are interpolated in */
    var space = GradientSpace.OKLAB
        set(v) {
            field = v
            bar.draw.dirty = true
        }

    /** the index of the stop that is selected */
    var selected = 0
        private set(v) {
            field = v
            picker.color = value.getOrNull(v)?.color ?: picker.color
            bar.draw.dirty = true
        }

    private val bar = GradientBar(this)
    private val picker = Colorpicker()

    /** the color of the gradient at [t], from 0 to 1 */
    fun colorAt(t: Double): ColorRGBa {
        val stops = value
        if (stops.isEmpty()) return ColorRGBa.TRANSPARENT
        if (t <= stops.first().position) return stops.first().color
        if (t >= stops.last().position) return stops.last().color
        val i = stops.indexOfFirst { it.position >= t }
        val a = stops[i - 1]
        val b = stops[i]
        val f = if (b.position > a.position) (t - a.position) / (b.position - a.position) else 0.0
        return when (space) {
            GradientSpace.SRGB -> a.color.toSRGB().mix(b.color.toSRGB(), f)
            GradientSpace.LINEAR_RGB -> a.color.toLinear().mix(b.color.toLinear(), f)
            GradientSpace.OKLAB -> {
                val la = ColorOKLABa.fromRGBa(a.color)
                val lb = ColorOKLABa.fromRGBa(b.color)
                ColorOKLABa(la.l + (lb.l - la.l) * f, la.a + (lb.a - la.a) * f, la.b + (lb.b - la.b) * f, la.alpha + (lb.alpha - la.alpha) * f).toRGBa()
            }
        }
    }

    internal fun change(stops: List<ColorStop>, select: ColorStop? = null) {
        interactive = true
        value = stops
        interactive = false
        select?.let { s -> selected = value.indexOf(s).coerceAtLeast(0) }
    }

    internal fun select(index: Int) {
        selected = index
    }

    init {
        append(bar)
        append(picker)
        picker.color = value[0].color
        picker.events.colorChanged.listen { e ->
            val stop = value.getOrNull(selected) ?: return@listen
            if (stop.color != e.newColor) change(value.map { if (it === stop) it.copy(color = e.newColor) else it }, stop.copy(color = e.newColor))
        }
        keyboard.pressed.listen {
            val stop = value.getOrNull(selected) ?: return@listen
            val step = if (KeyModifier.SHIFT in it.modifiers) 0.001 else 0.01
            when (it.key) {
                KEY_ARROW_LEFT, KEY_ARROW_RIGHT -> {
                    val moved = stop.copy(position = (stop.position + if (it.key == KEY_ARROW_LEFT) -step else step).coerceIn(0.0, 1.0))
                    change(value.map { s -> if (s === stop) moved else s }, moved)
                }
                KEY_DELETE, KEY_BACKSPACE -> if (value.size > 2) {
                    change(value - stop)
                    selected = selected.coerceAtMost(value.size - 1)
                }
                else -> return@listen
            }
            it.cancelPropagation()
        }
    }
}

/** the bar of a [GradientEditor] with its stops */
class GradientBar internal constructor(private val editor: GradientEditor) : Element(ElementType("gradient-bar")) {
    override val handlesDoubleClick = true

    private val margin = 8.0
    private var dragging: ColorStop? = null

    private fun positionAt(x: Double) = ((x - layout.screenX - margin) / (layout.screenWidth - 2.0 * margin)).coerceIn(0.0, 1.0)
    private fun xOf(t: Double) = margin + t * (layout.screenWidth - 2.0 * margin)
    private val barHeight get() = layout.screenHeight - 18.0

    init {
        mouse.pressed.listen {
            val x = it.position.x - layout.screenX
            val stops = editor.value
            val nearest = stops.indices.minByOrNull { i -> Math.abs(xOf(stops[i].position) - x) }
            dragging = nearest?.takeIf { i -> Math.abs(xOf(stops[i].position) - x) < 8.0 }?.let { i -> editor.select(i); stops[i] }
            it.cancelPropagation()
        }
        mouse.dragged.listen {
            val stop = dragging ?: return@listen
            val below = it.position.y - layout.screenY > layout.screenHeight + 30.0
            // a stop dragged far below the bar is removed, it comes back when it is dragged back
            val moved = stop.copy(position = positionAt(it.position.x))
            val others = editor.value.filter { s -> s !== stop && s != stop }
            if (below && others.size >= 2) editor.change(others) else editor.change(others + moved, moved)
            dragging = if (below && others.size >= 2) stop else moved
            it.cancelPropagation()
        }
        mouse.clicked.listen { dragging = null }
        mouse.doubleClicked.listen {
            val t = positionAt(it.position.x)
            val stop = ColorStop(t, editor.colorAt(t))
            editor.change(editor.value + stop, stop)
            it.cancelPropagation()
        }
    }

    override fun draw(drawer: Drawer) {
        val w = layout.screenWidth - 2.0 * margin
        val h = barHeight
        // the gradient, in vertical strips
        val steps = maxOf(2, (w / 2.0).toInt())
        drawer.stroke = null
        for (i in 0 until steps) {
            drawer.fill = editor.colorAt((i + 0.5) / steps)
            drawer.rectangle(margin + w * i / steps, 0.0, w / steps + 0.5, h)
        }
        drawer.fill = null
        drawer.stroke = ColorRGBa.WHITE.opacify(0.25)
        drawer.strokeWeight = 1.0
        drawer.rectangle(margin, 0.0, w, h)
        // the stops, the selected one outlined
        for ((i, stop) in editor.value.withIndex()) {
            val x = xOf(stop.position)
            drawer.fill = stop.color
            drawer.stroke = if (i == editor.selected) ColorRGBa.fromHex("#5ab0ff") else ColorRGBa.WHITE.opacify(0.8)
            drawer.strokeWeight = if (i == editor.selected) 2.0 else 1.0
            drawer.contour(contour { moveTo(x, h + 2.0); lineTo(x + 6.0, h + 9.0); lineTo(x + 6.0, h + 16.0); lineTo(x - 6.0, h + 16.0); lineTo(x - 6.0, h + 9.0); close() })
        }
    }
}

fun Element.curveEditor(vararg classes: String, init: CurveEditor.() -> Unit): CurveEditor = initElement(classes, CurveEditor(), init)

fun Element.gradientEditor(vararg classes: String, init: GradientEditor.() -> Unit): GradientEditor = initElement(classes, GradientEditor(), init)
