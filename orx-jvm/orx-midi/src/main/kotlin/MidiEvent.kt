package org.openrndr.extra.midi

enum class MidiEventType {
    NOTE_ON,
    NOTE_OFF,
    CONTROL_CHANGED,
    PROGRAM_CHANGE,
    CHANNEL_PRESSURE,
    PITCH_BEND
}

class MidiEvent(val eventType: MidiEventType) {
    var origin = Origin.DEVICE
    var control: Int = 0
    var program: Int = 0
    var note: Int = 0
    var channel: Int = 0
    var pitchBend: Int = 0
    var pressure: Int = 0
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

        fun controlChange(channel: Int, control: Int, value: Int): MidiEvent {
            val midiEvent = MidiEvent(MidiEventType.CONTROL_CHANGED)
            midiEvent.channel = channel
            midiEvent.control = control
            midiEvent.value = value
            return midiEvent
        }

        fun programChange(channel: Int, program: Int): MidiEvent {
            val midiEvent = MidiEvent(MidiEventType.PROGRAM_CHANGE)
            midiEvent.channel = channel
            midiEvent.program = program
            return midiEvent
        }

        fun channelPressure(channel: Int, pressure: Int): MidiEvent {
            val midiEvent = MidiEvent(MidiEventType.CHANNEL_PRESSURE)
            midiEvent.channel = channel
            midiEvent.pressure = pressure
            return midiEvent
        }

        fun pitchBend(channel: Int, pitchBend: Int): MidiEvent {
            val midiEvent = MidiEvent(MidiEventType.PITCH_BEND)
            midiEvent.channel = channel
            midiEvent.pitchBend = pitchBend
            return midiEvent
        }
    }

    override fun toString(): String {
        return "MidiEvent(eventType=$eventType, " +
                "origin=$origin, " +
                "program=$program, " +
                "control=$control, " +
                "note=$note, " +
                "channel=$channel, " +
                "pitchBend=$pitchBend, " +
                "pressure=$pressure, " +
                "value=$value, " +
                "velocity=$velocity)"
    }
}