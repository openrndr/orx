package org.openrndr.panel.elements

import org.openrndr.KEY_ARROW_DOWN
import org.openrndr.KEY_ARROW_LEFT
import org.openrndr.KEY_ARROW_RIGHT
import org.openrndr.KEY_ARROW_UP
import org.openrndr.KeyModifier
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.LineCap
import org.openrndr.events.Event
import org.openrndr.math.Polar
import org.openrndr.math.Vector2
import org.openrndr.panel.style.*
import org.openrndr.shape.Rectangle
import org.openrndr.shape.contour
import kotlin.math.round

/** a change of the value of a control, by the user when [interactive] is set */
class ValueChange<T>(val oldValue: T, val newValue: T, val interactive: Boolean)

/** [v] rounded to [precision] decimals */
private fun rounded(v: Double, precision: Int): Double = Math.pow(10.0, precision.toDouble()).let { round(v * it) / it }

private fun format(v: Double, precision: Int) = String.format("%.${precision}f", v)

/**
 * A rotary knob for a value in [range]: dragging up and down turns it, finer with shift, so does the wheel and, with
 * the keyboard focus, the arrow keys; a double click sets it to [default]. Its label and value are below it.
 */
class Knob : Element(ElementType("knob")) {
    override val handlesDoubleClick = true
    override val handlesKeyboardFocus = true

    var label: String by attribute("label", "")
    var range: ClosedRange<Double> = 0.0..1.0
    var precision = 2

    /** the value a double click sets */
    var default = 0.0

    val valueChanged = Event<ValueChange<Double>>("knob-value-changed")

    private var current = 0.0

    var value: Double
        get() = current
        set(v) = set(v, false)

    private fun set(v: Double, interactive: Boolean) {
        val clean = rounded(v.coerceIn(range.start, range.endInclusive), precision)
        if (clean != current) {
            val old = current
            current = clean
            draw.dirty = true
            valueChanged.trigger(ValueChange(old, clean, interactive))
        }
    }

    private val span get() = range.endInclusive - range.start

    init {
        mouse.pressed.listen { it.cancelPropagation() }
        mouse.dragged.listen {
            val fine = if (KeyModifier.SHIFT in it.modifiers) 0.1 else 1.0
            set(value - it.dragDisplacement.y / 200.0 * span * fine, true)
            it.cancelPropagation()
        }
        mouse.scrolled.listen {
            set(value + it.rotation.y * span * 0.01, true)
            it.cancelPropagation()
        }
        mouse.doubleClicked.listen {
            set(default, true)
            it.cancelPropagation()
        }
        keyboard.pressed.listen {
            val step = span * if (KeyModifier.SHIFT in it.modifiers) 0.001 else 0.01
            when (it.key) {
                KEY_ARROW_UP, KEY_ARROW_RIGHT -> set(value + step, true)
                KEY_ARROW_DOWN, KEY_ARROW_LEFT -> set(value - step, true)
                else -> return@listen
            }
            it.cancelPropagation()
        }
    }

    override fun draw(drawer: Drawer) {
        val color = computedStyle.effectiveColor ?: ColorRGBa.WHITE
        val w = layout.screenWidth
        val text = panelText
        val face = text?.face(computedStyle)
        val labelHeight = face?.let { (it.ascent - it.descent) * 2.2 } ?: 0.0
        val size = minOf(w, layout.screenHeight - labelHeight)
        val center = Vector2(w / 2.0, size / 2.0)
        val radius = size / 2.0 - 6.0
        // the track from -135 to 135 degrees, 0 up, and the value along it
        fun arc(from: Double, to: Double) = contour {
            val steps = 48
            for (i in 0..steps) {
                val a = from + (to - from) * i / steps
                val p = center + Polar(a - 90.0, radius).cartesian
                if (i == 0) moveTo(p) else lineTo(p)
            }
        }
        val t = ((value - range.start) / span).coerceIn(0.0, 1.0)
        drawer.fill = null
        drawer.lineCap = LineCap.ROUND
        drawer.strokeWeight = 5.0
        drawer.stroke = color.opacify(0.25)
        drawer.contour(arc(-135.0, 135.0))
        drawer.stroke = color
        if (t > 0.0) drawer.contour(arc(-135.0, -135.0 + 270.0 * t))
        // the pointer
        drawer.strokeWeight = 2.0
        drawer.lineSegment(center, center + Polar(-135.0 + 270.0 * t - 90.0, radius - 10.0).cartesian)
        if (ElementPseudoClass("focus-visible") in pseudoClasses) {
            drawer.stroke = color.opacify(0.5)
            drawer.strokeWeight = 1.0
            drawer.circle(center, radius + 5.0)
        }
        if (text != null && face != null) {
            text.drawInBox(drawer, this, "label", computedStyle, label, Rectangle(0.0, size, w, labelHeight / 2.0), 0.5, 0.5, color)
            text.drawInBox(drawer, this, "value", computedStyle, format(value, precision), Rectangle(0.0, size + labelHeight / 2.0, w, labelHeight / 2.0), 0.5, 0.5, color.opacify(0.7))
        }
    }
}

/**
 * A slider with two thumbs for a range of values in [range], [low] to [high]: the thumb nearest to the pointer follows
 * it; with the keyboard focus, left and right move the thumb that moved last, finer with shift.
 */
class RangeSlider : Element(ElementType("range-slider")) {
    override val handlesKeyboardFocus = true

    var label: String by attribute("label", "")
    var range: ClosedRange<Double> = 0.0..1.0
    var precision = 2

    val valueChanged = Event<ValueChange<ClosedRange<Double>>>("range-slider-value-changed")

    /** the range of values that is selected */
    var value: ClosedRange<Double> = 0.25..0.75
        set(v) {
            val start = rounded(v.start.coerceIn(range.start, range.endInclusive), precision)
            val end = rounded(v.endInclusive.coerceIn(start, range.endInclusive), precision)
            val clean = start..end
            if (clean != field) {
                val old = field
                field = clean
                draw.dirty = true
                valueChanged.trigger(ValueChange(old, clean, interactive))
            }
        }

    val low: Double get() = value.start
    val high: Double get() = value.endInclusive

    private var interactive = false
    private var activeHigh = true
    private val margin = 7.0

    private fun valueAt(x: Double) = range.start + ((x - layout.screenX - margin) / (layout.screenWidth - 2.0 * margin)).coerceIn(0.0, 1.0) * (range.endInclusive - range.start)
    private fun xOf(v: Double) = margin + (v - range.start) / (range.endInclusive - range.start) * (layout.screenWidth - 2.0 * margin)

    private fun move(v: Double, high: Boolean) {
        interactive = true
        value = if (high) low..maxOf(v, low) else minOf(v, this.high)..this.high
        interactive = false
    }

    init {
        mouse.pressed.listen {
            val v = valueAt(it.position.x)
            activeHigh = Math.abs(v - high) < Math.abs(v - low) || (v > high)
            move(v, activeHigh)
            it.cancelPropagation()
        }
        mouse.dragged.listen {
            move(valueAt(it.position.x), activeHigh)
            it.cancelPropagation()
        }
        keyboard.pressed.listen {
            val step = (range.endInclusive - range.start) * if (KeyModifier.SHIFT in it.modifiers) 0.001 else 0.01
            val current = if (activeHigh) high else low
            when (it.key) {
                KEY_ARROW_RIGHT -> move(current + step, activeHigh)
                KEY_ARROW_LEFT -> move(current - step, activeHigh)
                else -> return@listen
            }
            it.cancelPropagation()
        }
    }

    override fun draw(drawer: Drawer) {
        val color = computedStyle.effectiveColor ?: ColorRGBa.WHITE
        val text = panelText ?: return
        val face = text.face(computedStyle)
        drawer.translate(0.0, (layout.screenHeight - (10.0 + face.ascent + face.descent)) / 2)
        drawer.lineCap = LineCap.ROUND
        drawer.strokeWeight = 8.0
        drawer.stroke = color.opacify(0.25)
        drawer.lineSegment(margin, 2.0, layout.screenWidth - margin, 2.0)
        drawer.stroke = color
        drawer.lineSegment(xOf(low), 2.0, xOf(high), 2.0)
        drawer.stroke = null
        drawer.fill = color
        drawer.circle(xOf(low), 2.0, 5.0)
        drawer.circle(xOf(high), 2.0, 5.0)
        if (ElementPseudoClass("focus-visible") in pseudoClasses) {
            drawer.fill = null
            drawer.stroke = color.opacify(0.6)
            drawer.strokeWeight = 1.0
            drawer.circle(xOf(if (activeHigh) high else low), 2.0, 9.0)
        }
        val y = 2.0 + 10.0 + face.ascent
        text.drawLine(drawer, this, "label", computedStyle, label, 0.0, y, color)
        val values = "${format(low, precision)} – ${format(high, precision)}"
        text.drawLine(drawer, this, "value", computedStyle, values, layout.screenWidth - text.width(computedStyle, values), y, color)
    }
}

fun Element.knob(vararg classes: String, init: Knob.() -> Unit): Knob = initElement(classes, Knob(), init)

fun Element.rangeSlider(vararg classes: String, init: RangeSlider.() -> Unit): RangeSlider = initElement(classes, RangeSlider(), init)
