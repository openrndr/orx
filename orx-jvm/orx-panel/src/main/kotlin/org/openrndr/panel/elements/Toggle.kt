package org.openrndr.panel.elements

import org.openrndr.KEY_SPACEBAR
import org.openrndr.Program
import org.openrndr.draw.Drawer
import org.openrndr.draw.LineCap
import org.openrndr.draw.loadFont
import org.openrndr.panel.style.*
import org.openrndr.shape.Rectangle


import org.openrndr.events.Event
import org.openrndr.extra.textwriter.TextWriter
import org.openrndr.panel.binding.Binding0
import org.openrndr.panel.binding.Binding1
import kotlin.reflect.KMutableProperty0
import kotlin.reflect.KMutableProperty1

class Toggle : Element(ElementType("toggle")) {

    override val handlesKeyboardFocus = true

    var label = ""
    var value = false

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
        get() {
            computedStyle.let { style ->
                val fontUrl = (root() as? Body)?.controlManager?.fontManager?.resolve(style.fontFamily) ?: "broken"
                val fontSize = (style.fontSize as? LinearDimension.PX)?.value ?: 14.0
                val program = (root() as? Body)?.controlManager?.program ?: error("no program")
                val fontMap = program.loadFont(fontUrl, fontSize)

                val writer = TextWriter(null)

                writer.box = Rectangle(
                    0.0,
                    0.0,
                    Double.POSITIVE_INFINITY,
                    Double.POSITIVE_INFINITY
                )

                writer.drawStyle.fontMap = fontMap
                writer.newLine()
                writer.text(label, visible = false)

                return writer.cursor.x + (computedStyle.height as LinearDimension.PX).value - 8.0 + 5.0
            }
        }

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
        drawer.fontMap = (root() as? Body)?.controlManager?.fontManager?.font(computedStyle)!!
        drawer.translate(5.0 + checkBoxSize, (layout.screenHeight / 2.0) + drawer.fontMap!!.height / 2.0)
        drawer.stroke = null
        drawer.fill = computedStyle.effectiveColor
        drawer.text(label, 0.0, 0.0)
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