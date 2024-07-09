package org.openrndr.extra.midi

import javax.sound.midi.MidiMessage
import javax.sound.midi.ShortMessage

enum class MidiEventType(val status: Int) {

    MIDI_TIME_CODE(ShortMessage.MIDI_TIME_CODE),
    SONG_POSITION_POINTER(ShortMessage.SONG_POSITION_POINTER),
    SONG_SELECT(ShortMessage.SONG_SELECT),
    TUNE_REQUEST(ShortMessage.TUNE_REQUEST),
    END_OF_EXCLUSIVE(ShortMessage.END_OF_EXCLUSIVE),
    TIMING_CLOCK(ShortMessage.TIMING_CLOCK),
    START(ShortMessage.START),
    CONTINUE(ShortMessage.CONTINUE),
    STOP(ShortMessage.STOP),
    ACTIVE_SENSING(ShortMessage.ACTIVE_SENSING),
    SYSTEM_RESET(ShortMessage.SYSTEM_RESET),
    NOTE_ON(ShortMessage.NOTE_ON),
    NOTE_OFF(ShortMessage.NOTE_OFF),
    CONTROL_CHANGE(ShortMessage.CONTROL_CHANGE),
    PROGRAM_CHANGE(ShortMessage.PROGRAM_CHANGE),
    CHANNEL_PRESSURE(ShortMessage.CHANNEL_PRESSURE),
    PITCH_BEND(ShortMessage.PITCH_BEND);

    companion object {

        private val statusMap: Map<Int, MidiEventType> =
            entries.associateBy { it.status }

        fun fromStatus(
            status: Int
        ): MidiEventType = requireNotNull(
            statusMap[if (status >= 0xf0) status else status and 0xf0]
        ) {
            "Invalid MIDI status: $status"
        }

    }

}

val MidiMessage.eventType: MidiEventType get() = MidiEventType.fromStatus(status)

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

        fun noteOff(channel: Int, note: Int, velocity: Int): MidiEvent {
            val midiEvent = MidiEvent(MidiEventType.NOTE_OFF)
            midiEvent.note = note
            midiEvent.channel = channel
            midiEvent.velocity = velocity
            return midiEvent
        }

        fun controlChange(channel: Int, control: Int, value: Int): MidiEvent {
            val midiEvent = MidiEvent(MidiEventType.CONTROL_CHANGE)
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