package org.openrndr.extra.midi

enum class MidiEventType {
    NOTE_ON,
    NOTE_OFF,
    CONTROL_CHANGED
}

class MidiEvent(val eventType: MidiEventType) {
    var origin = Origin.DEVICE
    var control: Int = 0
    var note: Int = 0
    var channel: Int = 0
    var value: Int = 0
    var velocity: Int = 0

    enum class Origin {
        DEVICE,
        USER
    }

    companion object {
        fun noteOn(channel: Int, note: Int, velocity: Int): MidiEvent {
            val midiEvent = MidiEvent(MidiEventType.NOTE_ON)
            midiEvent.velocity = velocity
            midiEvent.note = note
            midiEvent.channel = channel
            return midiEvent
        }

        fun noteOff(channel: Int, note: Int): MidiEvent {
            val midiEvent = MidiEvent(MidiEventType.NOTE_OFF)
            midiEvent.note = note
            midiEvent.channel = channel
            return midiEvent
        }

        fun controlChange(channel:Int, control: Int, value: Int): MidiEvent {
            val midiEvent = MidiEvent(MidiEventType.CONTROL_CHANGED)
            midiEvent.channel = channel
            midiEvent.control = control
            midiEvent.value = value
            return midiEvent
        }
    }

    override fun toString(): String {
        return "MidiEvent(eventType=$eventType, origin=$origin, control=$control, note=$note, channel=$channel, value=$value, velocity=$velocity)"
    }
}