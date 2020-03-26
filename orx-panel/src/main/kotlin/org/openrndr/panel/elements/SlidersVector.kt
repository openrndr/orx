package org.openrndr.panel.elements

import org.openrndr.events.Event
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.math.Vector4

class SlidersVector2 : SequenceEditorBase("sliders-vector2") {
    var value : Vector2
        get() {
            return Vector2(baseValue[0], baseValue[1])
        }
        set(value) {
            baseValue[0] = value.x
            baseValue[1] = value.y
        }

    class ValueChangedEvent(val source: SequenceEditorBase,
                            val oldValue: Vector2,
                            val newValue: Vector2)

    class Events {
        val valueChanged = Event<ValueChangedEvent>("sequence-editor-value-changed")
    }

    val events = Events()

    init {
        baseValue = mutableListOf(0.0, 0.0)
        minimumSequenceLength = 2
        maximumSequenceLength = 2
        baseEvents.valueChanged.listen {
            events.valueChanged.trigger(ValueChangedEvent(this,
                    Vector2(it.oldValue[0], it.oldValue[1]),
                    Vector2(it.newValue[0], it.newValue[1]))
            )
        }
    }
}


class SlidersVector3 : SequenceEditorBase("sliders-vector3") {
    var value : Vector3
    get() {
        return Vector3(baseValue[0], baseValue[1], baseValue[2])
    }
    set(value) {
        baseValue[0] = value.x
        baseValue[1] = value.y
        baseValue[2] = value.z
    }

    class ValueChangedEvent(val source: SequenceEditorBase,
                            val oldValue: Vector3,
                            val newValue: Vector3)

    class Events {
        val valueChanged = Event<ValueChangedEvent>("sliders-vector3-value-changed")
    }

    val events = Events()

    init {
        baseValue = mutableListOf(0.0, 0.0, 0.0)
        minimumSequenceLength = 3
        maximumSequenceLength = 3
        baseEvents.valueChanged.listen {
            events.valueChanged.trigger(ValueChangedEvent(this,
                    Vector3(it.oldValue[0], it.oldValue[1], it.oldValue[2]),
                    Vector3(it.newValue[0], it.newValue[1], it.newValue[2]))
            )
        }
    }
}

class SlidersVector4 : SequenceEditorBase("sliders-vector4") {
    var value : Vector4
        get() {
            return Vector4(baseValue[0], baseValue[1], baseValue[2], baseValue[3])
        }
        set(value) {
            baseValue[0] = value.x
            baseValue[1] = value.y
            baseValue[2] = value.z
            baseValue[3] = value.w
        }

    class ValueChangedEvent(val source: SequenceEditorBase,
                            val oldValue: Vector4,
                            val newValue: Vector4)

    class Events {
        val valueChanged = Event<ValueChangedEvent>("sliders-vector4-value-changed")
    }

    val events = Events()

    init {
        baseValue = mutableListOf(0.0, 0.0, 0.0, 0.0)
        minimumSequenceLength = 4
        maximumSequenceLength = 4
        baseEvents.valueChanged.listen {
            events.valueChanged.trigger(ValueChangedEvent(this,
                    Vector4(it.oldValue[0], it.oldValue[1], it.oldValue[2], it.oldValue[3]),
                    Vector4(it.newValue[0], it.newValue[1], it.newValue[2], it.newValue[3]))
            )
        }
    }
}

