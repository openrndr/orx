package org.openrndr.panel.elements

import org.openrndr.color.ColorRGBa

import org.openrndr.KEY_SPACEBAR
import org.openrndr.Program
import org.openrndr.draw.Drawer
import org.openrndr.draw.StrokeAlignment
import org.openrndr.draw.LineCap
import org.openrndr.panel.style.*
import org.openrndr.shape.Rectangle


import org.openrndr.events.Event
import org.openrndr.panel.binding.Binding0
import org.openrndr.panel.binding.Binding1
import kotlin.reflect.KMutableProperty0
import kotlin.reflect.KMutableProperty1

class Toggle : Element(ElementType("toggle")) {

    override val handlesKeyboardFocus = true

    /** the label, the attribute `label` */
    var label: String by attribute("label", "")
    /** the value, a toggle that is on is `:checked` */
    var value = false
        set(value) {
            field = value
            if (value) pseudoClasses.add(ElementPseudoClass("checked")) else pseudoClasses.remove(ElementPseudoClass("checked"))
        }

    class ValueChangedEvent(
        val source: Toggle,
        val oldValue: Boolean,
        val newValue: Boolean
    )

    class Events : AutoCloseable {

        val valueChanged = Event<ValueChangedEvent>("toggle-value-changed")

        override fun close() {
            valueChanged.close()
        }
    }

    val events = Events()

    override val widthHint: Double?
        get() = (panelText?.width(computedStyle, label) ?: error("no control manager")) +
                (computedStyle.height as LinearDimension.PX).value - 8.0 + 5.0

    init {
        mouse.pressed.listen {
            it.cancelPropagation()
        }
        mouse.clicked.listen {
            value = !value
            draw.dirty = true
            events.valueChanged.trigger(Toggle.ValueChangedEvent(this, !value, value))
            it.cancelPropagation()
        }

        keyboard.pressed.listen {
            if (it.key == KEY_SPACEBAR) {
                value = !value
                it.cancelPropagation()
                draw.dirty = true
                events.valueChanged.trigger(Toggle.ValueChangedEvent(this, !value, value))
            }
        }
    }

    /**
     * Emits the current value through the valueChanged event
     */
    fun emit() {
        events.valueChanged.trigger(Toggle.ValueChangedEvent(this, value, value))
    }

    override fun draw(drawer: Drawer) {
        drawer.pushModel()
        val checkBoxSize = layout.screenHeight - 8.0
        drawer.translate(0.0, (layout.screenHeight - checkBoxSize) / 2.0)
        drawer.strokeWeight = 1.0
        drawer.stroke = computedStyle.effectiveColor
        drawer.fill = null
        // the frame of the check box is drawn inside it
        drawer.strokeAlignment = StrokeAlignment.INSIDE
        drawer.rectangle(0.0, 0.0, checkBoxSize, checkBoxSize)

        if (value) {
            drawer.strokeWeight = 2.0
            drawer.stroke = computedStyle.effectiveColor
            drawer.fill = null
            drawer.lineCap = LineCap.ROUND
            drawer.lineSegment(5.0, 5.0, checkBoxSize / 2.0 - 2.0, checkBoxSize / 2.0 - 2.0)
            drawer.lineSegment(
                checkBoxSize / 2.0 + 2.0,
                checkBoxSize / 2.0 + 2.0,
                checkBoxSize - 5.0,
                checkBoxSize - 5.0
            )
            drawer.lineSegment(checkBoxSize - 5.0, 5.0, checkBoxSize / 2.0 + 2.0, checkBoxSize / 2.0 - 2.0)
            drawer.lineSegment(checkBoxSize / 2.0 - 2.0, checkBoxSize / 2.0 + 2.0, 5.0, checkBoxSize - 5.0)
        }

        drawer.popModel()
        val text = panelText ?: return
        val face = text.face(computedStyle)
        // the label is centered vertically, from its ascent to its descent, as the labels of buttons are
        text.drawLine(
            drawer, this, "label", computedStyle, label, 5.0 + checkBoxSize,
            layout.screenHeight / 2.0 + (face.ascent + face.descent) / 2.0, computedStyle.effectiveColor ?: ColorRGBa.WHITE
        )
    }

    override fun close() {
        super.close()
        events.close()
    }
}

fun Toggle.bind(property: KMutableProperty0<Boolean>, program: Program? = null) {
    val program = program ?: (root() as? Body)?.controlManager?.program
    Binding0(program ?: error("no program"), this, this.events.valueChanged, property, { it.newValue }, { value = it })
}

fun Toggle.bind(container: Any, property: KMutableProperty1<Any, Boolean>, program: Program? = null) {
    val program = program ?: (root() as? Body)?.controlManager?.program
    Binding1(
        program ?: error("no program"),
        this,
        this.events.valueChanged,
        container,
        property,
        { it.newValue },
        { value = it })
}