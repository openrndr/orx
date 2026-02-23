package org.openrndr.panel.elements

import org.openrndr.Program
import org.openrndr.events.Event
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.math.Vector4
import org.openrndr.panel.binding.Binding0
import org.openrndr.panel.binding.Binding1
import kotlin.reflect.KMutableProperty0
import kotlin.reflect.KMutableProperty1

class SlidersVector2 : SequenceEditorBase("sliders-vector2") {
    var value: Vector2
        get() {
            return Vector2(baseValue[0], baseValue[1])
        }
        set(value) {
            baseValue[0] = value.x
            baseValue[1] = value.y
            requestRedraw()
        }

    class ValueChangedEvent(
        val source: SequenceEditorBase,
        val oldValue: Vector2,
        val newValue: Vector2
    )

    class Events {
        val valueChanged = Event<ValueChangedEvent>("sequence-editor-value-changed")
    }

    val events = Events()

    init {
        baseValue = mutableListOf(0.0, 0.0)
        minimumSequenceLength = 2
        maximumSequenceLength = 2
        baseEvents.valueChanged.listen {
            events.valueChanged.trigger(
                ValueChangedEvent(
                    this,
                    Vector2(it.oldValue[0], it.oldValue[1]),
                    Vector2(it.newValue[0], it.newValue[1])
                )
            )
        }
    }
}

fun SlidersVector2.bind(property: KMutableProperty0<Vector2>, program: Program? = null): Binding0<SlidersVector2.ValueChangedEvent, Vector2> {
    val program = program ?: (root() as? Body)?.controlManager?.program
    return Binding0(program ?: error("no program"), this, this.events.valueChanged, property, { it.newValue }, { value = it })
}

fun SlidersVector2.bind(container: Any, property: KMutableProperty1<Any, Vector2>,  program: Program? = null): Binding1<SlidersVector2.ValueChangedEvent, Vector2> {
    val program = program ?: (root() as? Body)?.controlManager?.program
    return Binding1(
        program ?: error("no program"),
        this,
        this.events.valueChanged,
        container,
        property,
        { it.newValue },
        { value = it })
}

class SlidersVector3 : SequenceEditorBase("sliders-vector3") {
    var value: Vector3
        get() {
            return Vector3(baseValue[0], baseValue[1], baseValue[2])
        }
        set(value) {
            baseValue[0] = value.x
            baseValue[1] = value.y
            baseValue[2] = value.z
            requestRedraw()
        }

    class ValueChangedEvent(
        val source: SequenceEditorBase,
        val oldValue: Vector3,
        val newValue: Vector3
    )

    class Events {
        val valueChanged = Event<ValueChangedEvent>("sliders-vector3-value-changed")
    }

    val events = Events()

    init {
        baseValue = mutableListOf(0.0, 0.0, 0.0)
        minimumSequenceLength = 3
        maximumSequenceLength = 3
        baseEvents.valueChanged.listen {
            events.valueChanged.trigger(
                ValueChangedEvent(
                    this,
                    Vector3(it.oldValue[0], it.oldValue[1], it.oldValue[2]),
                    Vector3(it.newValue[0], it.newValue[1], it.newValue[2])
                )
            )
        }
    }
}

fun SlidersVector3.bind(property: KMutableProperty0<Vector3>, program: Program? = null): Binding0<SlidersVector3.ValueChangedEvent, Vector3> {
    val program = program ?: (root() as? Body)?.controlManager?.program
    return Binding0(program ?: error("no program"), this, this.events.valueChanged, property, { it.newValue }, { value = it })
}

fun SlidersVector3.bind(container: Any, property: KMutableProperty1<Any, Vector3>, program: Program? = null): Binding1<SlidersVector3.ValueChangedEvent, Vector3> {
    val program = program ?: (root() as? Body)?.controlManager?.program
    return Binding1(
        program ?: error("no program"),
        this,
        this.events.valueChanged,
        container,
        property,
        { it.newValue },
        { value = it })
}

class SlidersVector4 : SequenceEditorBase("sliders-vector4") {
    var value: Vector4
        get() {
            return Vector4(baseValue[0], baseValue[1], baseValue[2], baseValue[3])
        }
        set(value) {
            baseValue[0] = value.x
            baseValue[1] = value.y
            baseValue[2] = value.z
            baseValue[3] = value.w
            requestRedraw()
        }

    class ValueChangedEvent(
        val source: SequenceEditorBase,
        val oldValue: Vector4,
        val newValue: Vector4
    )

    class Events {
        val valueChanged = Event<ValueChangedEvent>("sliders-vector4-value-changed")
    }

    val events = Events()

    init {
        baseValue = mutableListOf(0.0, 0.0, 0.0, 0.0)
        minimumSequenceLength = 4
        maximumSequenceLength = 4
        baseEvents.valueChanged.listen {
            events.valueChanged.trigger(
                ValueChangedEvent(
                    this,
                    Vector4(it.oldValue[0], it.oldValue[1], it.oldValue[2], it.oldValue[3]),
                    Vector4(it.newValue[0], it.newValue[1], it.newValue[2], it.newValue[3])
                )
            )
        }
    }
}

fun SlidersVector4.bind(property: KMutableProperty0<Vector4>, program: Program? = null): Binding0<SlidersVector4.ValueChangedEvent, Vector4> {
    val program = program ?: (root() as? Body)?.controlManager?.program
    return Binding0(program ?: error("no program"), this, this.events.valueChanged, property, { it.newValue }, { value = it })
}

fun SlidersVector4.bind(container: Any, property: KMutableProperty1<Any, Vector4>, program: Program? = null): Binding1<SlidersVector4.ValueChangedEvent, Vector4> {
    val program = program ?: (root() as? Body)?.controlManager?.program
    return Binding1(
        program ?: error("no program"),
        this,
        this.events.valueChanged,
        container,
        property,
        { it.newValue },
        { value = it })
}