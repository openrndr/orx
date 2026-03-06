package org.openrndr.panel.elements

import org.openrndr.Program
import org.openrndr.events.Event
import org.openrndr.panel.binding.Binding0
import org.openrndr.panel.binding.Binding1
import kotlin.reflect.KMutableProperty0
import kotlin.reflect.KMutableProperty1

class Textfield : Element(ElementType("textfield")) {
    val textInput: TextInput
    val labelElement: Label

    var value: String = ""
        get() {
            return field
        }
        set(value) {
            if (field != value) {
                field = value
                requestRedraw()
            }
        }

    var label: String = "label"
        set(value) {
            if (field != value) {
                field = value
                requestRedraw()
            }
        }

    class ValueChangedEvent(val source: Textfield, val oldValue: String, val newValue: String)
    class Events : AutoCloseable {
        val valueChanged = Event<ValueChangedEvent>("textfield-value-changed")
        override fun close() {
            valueChanged.close()
        }
    }

    val events = Events()

    init {
        append {
            val program = Program.active ?: error("no program")
            labelElement = label {
                bind(this@Textfield::label, program)
                this@Textfield.label
            }
            textInput = textInput {
                bind(this@Textfield::value, program)
                events.valueChanged.listen {
                    this@Textfield.events.valueChanged.trigger(ValueChangedEvent(this@Textfield, it.oldValue, it.newValue))
                }
            }
        }
    }

    override fun close() {
        super.close()
        events.close()
    }
}

fun Textfield.bind(property: KMutableProperty0<String>, program: Program? = null) {
    val program = program ?: (root() as? Body)?.controlManager?.program
    Binding0(program ?: error("no program"), this, this.events.valueChanged, property, { it.newValue }, { value = it })
}

fun Textfield.bind(container: Any, property: KMutableProperty1<Any, String>, program: Program? = null) {
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