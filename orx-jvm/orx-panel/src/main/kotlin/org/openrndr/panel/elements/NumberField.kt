package org.openrndr.panel.elements

import org.openrndr.panel.recordEdit
import kotlin.reflect.KMutableProperty0
import org.openrndr.panel.binding.bindValue
import org.openrndr.panel.binding.PropertyBinding
import org.openrndr.*
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.events.Event
import org.openrndr.panel.style.*
import org.openrndr.shape.Rectangle
import kotlin.math.round

/**
 * A number that is changed by dragging it left and right, by [step] per pixel, ten times finer with shift, or by
 * typing: double-click it or press enter to type a value, enter accepts it and escape cancels. The arrow keys change it
 * by ten steps. The value stays in [range] when it is set.
 */
class NumberField : Element(ElementType("number-field")) {
    override val handlesKeyboardFocus = true
    override val handlesDoubleClick = true

    class ValueChangedEvent(val source: NumberField, val oldValue: Double, val newValue: Double)
    class Events : AutoCloseable {
        val valueChanged = Event<ValueChangedEvent>("number-field-value-changed")
        override fun close() = valueChanged.close()
    }

    val events = Events()

    /** the label, the attribute `label` */
    var label: String by attribute("label", "value")

    /** the change of the value for every pixel the pointer is dragged */
    var step = 0.01

    /** the number of decimals shown */
    var precision = 3

    /** the range of the value, null when it is not limited */
    var range: ClosedRange<Double>? = null

    var value: Double = 0.0
        set(value) {
            val limited = range?.let { value.coerceIn(it.start, it.endInclusive) } ?: value
            if (limited != field) {
                val old = field
                field = limited
                events.valueChanged.trigger(ValueChangedEvent(this, old, limited))
                requestRedraw()
            }
        }

    // a change by the user can be undone
    private fun setByUser(v: Double) {
        val old = value
        value = v
        recordEdit("Change $label", old, value) { value = it }
    }

    /** the text that is typed, null when the value is not edited */
    var editing: String? = null
        private set

    private var dragStartValue = 0.0
    private var dragStartX = 0.0

    /** starts typing a value, with [text] typed */
    fun edit(text: String = format(value)) {
        editing = text
        requestRedraw()
    }

    /** accepts the typed value when it is a value, a number or arithmetic, see [evaluateInput] */
    fun accept() {
        editing?.let { evaluateInput(it, value, range) }?.let { setByUser(it) }
        editing = null
        requestRedraw()
    }

    /** stops typing, keeping the value */
    fun cancel() {
        editing = null
        requestRedraw()
    }

    private fun format(v: Double): String {
        val factor = Math.pow(10.0, precision.toDouble())
        return (round(v * factor) / factor).toString().let { if (it.endsWith(".0") && precision == 0) it.dropLast(2) else it }
    }

    init {
        mouse.pressed.listen {
            dragStartX = it.position.x
            dragStartValue = value
            it.cancelPropagation()
        }
        mouse.dragged.listen {
            if (editing == null) {
                val fine = if (KeyModifier.SHIFT in it.modifiers) 0.1 else 1.0
                setByUser(dragStartValue + (it.position.x - dragStartX) * step * fine)
            }
            it.cancelPropagation()
        }
        mouse.clicked.listen { it.cancelPropagation() }
        mouse.doubleClicked.listen {
            edit()
            it.cancelPropagation()
        }
        keyboard.pressed.listen {
            val typed = editing
            if (typed != null) {
                when (it.key) {
                    KEY_ENTER -> accept()
                    KEY_ESCAPE -> cancel()
                    KEY_BACKSPACE -> edit(typed.dropLast(1))
                    else -> return@listen
                }
            } else {
                val fine = if (KeyModifier.SHIFT in it.modifiers) 0.1 else 1.0
                when (it.key) {
                    KEY_ENTER -> edit()
                    KEY_ARROW_UP, KEY_ARROW_RIGHT -> value += step * 10.0 * fine
                    KEY_ARROW_DOWN, KEY_ARROW_LEFT -> value -= step * 10.0 * fine
                    else -> return@listen
                }
            }
            it.cancelPropagation()
        }
        keyboard.character.listen {
            if (it.character.isDigit() || it.character in ".-+eE") {
                // typing a number starts editing with it
                edit((editing ?: "") + it.character)
                it.cancelPropagation()
            }
        }
        keyboard.focusLost.listen { accept() }
    }

    override fun draw(drawer: Drawer) {
        val cs = computedStyle
        drawer.fill = cs.effectiveBackground ?: ColorRGBa.TRANSPARENT
        drawer.stroke = if (editing != null) (cs.effectiveColor ?: ColorRGBa.WHITE) else cs.effectiveBorderColor
        drawer.strokeWeight = 1.0
        drawer.box(layout.boundsAtOrigin, cornerRadius)
        // the part of the range the value is at
        range?.let { r ->
            if (r.endInclusive > r.start && editing == null) {
                val t = ((value - r.start) / (r.endInclusive - r.start)).coerceIn(0.0, 1.0)
                drawer.stroke = null
                drawer.fill = (cs.effectiveColor ?: ColorRGBa.WHITE).opacify(0.15)
                drawer.box(Rectangle(0.0, 0.0, layout.screenWidth * t, layout.screenHeight), cornerRadius)
            }
        }
        val panel = panelText ?: return
        val face = panel.face(cs)
        val color = cs.effectiveColor ?: ColorRGBa.WHITE
        val content = layout.contentBoundsAtOrigin
        val baseline = round(content.y + content.height / 2.0 + face.ascent / 2.0 - 1.0)
        panel.drawLine(drawer, this, "label", cs, label, content.x, baseline, color)
        val text = editing?.let { "$it|" } ?: format(value)
        panel.drawLine(drawer, this, "value", cs, text, content.x + content.width - panel.width(cs, text), baseline, color)
    }
}

/** adds a [NumberField] */
fun Element.numberField(vararg classes: String, init: NumberField.() -> Unit): NumberField =
    initElement(classes, NumberField(), init)

/** binds the value of the field to [property], both ways, see [bindValue] */
fun NumberField.bind(property: KMutableProperty0<Double>): PropertyBinding<Double> =
    bindValue(events.valueChanged, property, { value }, { value = it })
