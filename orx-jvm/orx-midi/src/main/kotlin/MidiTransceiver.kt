package org.openrndr.extra.midi

import org.openrndr.events.Event
import javax.sound.midi.*

data class MidiDeviceName(val name: String, val vendor: String)
class MidiDeviceCapabilities {
    var receive: Boolean = false
    var transmit: Boolean = false

    override fun toString(): String {
        return "MidiDeviceCapabilities(receive=$receive, transmit=$transmit)"
    }
}

data class MidiDeviceDescription(
    val name: String,
    val vendor: String,
    val receive: Boolean,
    val transmit: Boolean
) {
    companion object {
        fun list(): List<MidiDeviceDescription> {
            val caps = mutableMapOf<MidiDeviceName, MidiDeviceCapabilities>()

            val infos = MidiSystem.getMidiDeviceInfo()
            for (info in infos) {
                val device = MidiSystem.getMidiDevice(info)
                val name = MidiDeviceName(info.name, info.vendor)
                val deviceCaps =
                    caps.getOrPut(name) { MidiDeviceCapabilities() }

                if (device !is Sequencer && device !is Synthesizer) {
                    if (device.maxReceivers != 0 && device.maxTransmitters == 0) {
                        deviceCaps.receive = true
                    }
                    if (device.maxTransmitters != 0 && device.maxReceivers == 0) {
                        deviceCaps.transmit = true
                    }
                }
            }
            return caps.map {
                MidiDeviceDescription(
                    it.key.name,
                    it.key.vendor,
                    it.value.receive,
                    it.value.transmit
                )
            }
        }
    }

    fun open(): MidiTransceiver {
        require(receive && transmit) {
            "device should be a receiver and transmitter"
        }

        return MidiTransceiver.fromDeviceVendor(name, vendor)
    }
}

class MidiTransceiver(val receiverDevice: MidiDevice, val transmitterDevicer: MidiDevice) {
    companion object {
        fun fromDeviceVendor(name: String, vendor: String): MidiTransceiver {
            val infos = MidiSystem.getMidiDeviceInfo()

            var receiverDevice: MidiDevice? = null
            var transmitterDevice: MidiDevice? = null

            for (info in infos) {
                try {
                    val device = MidiSystem.getMidiDevice(info)
                    if (device !is Sequencer && device !is Synthesizer) {
                        if (info.vendor == vendor && info.name == name) {
                            if (device.maxTransmitters != 0 && device.maxReceivers == 0) {
                                transmitterDevice = device
                            }
                            if (device.maxReceivers != 0 && device.maxTransmitters == 0) {
                                receiverDevice = device
                            }
                        }
                    }
                } catch (e: MidiUnavailableException) {
                    throw IllegalStateException("no midi available")
                }
            }

            if (receiverDevice != null && transmitterDevice != null) {
                receiverDevice.open()
                transmitterDevice.open()
                return MidiTransceiver(receiverDevice, transmitterDevice)
            } else {
                throw IllegalArgumentException("midi device not found ${name}:${vendor} ${receiverDevice} ${transmitterDevice}")
            }
        }
    }

    private val receiver = receiverDevice.receiver
    private val transmitter = transmitterDevicer.transmitter

    private inner class Destroyer : Thread() {
        override fun run() {
            destroy()
        }
    }

    init {
        transmitter.receiver = object : MidiDeviceReceiver {
            override fun getMidiDevice(): MidiDevice? {
                return null
            }

            override fun send(message: MidiMessage, timeStamp: Long) {
                val cmd = message.message
                val channel = (cmd[0].toInt() and 0xff) and 0x0f
                val status = (cmd[0].toInt() and 0xff) and 0xf0
                when (status) {
                    ShortMessage.NOTE_ON -> noteOn.trigger(
                        MidiEvent.noteOn(
                            channel,
                            cmd[1].toInt() and 0xff,
                            cmd[2].toInt() and 0xff
                        )
                    )

                    ShortMessage.NOTE_OFF -> noteOff.trigger(
                        MidiEvent.noteOff(
                            channel,
                            cmd[1].toInt() and 0xff
                        )
                    )

                    ShortMessage.CONTROL_CHANGE -> controlChanged.trigger(
                        MidiEvent.controlChange(
                            channel,
                            cmd[1].toInt() and 0xff,
                            cmd[2].toInt() and 0xff
                        )
                    )

                    ShortMessage.PROGRAM_CHANGE -> programChanged.trigger(
                        MidiEvent.programChange(
                            channel,
                            cmd[1].toInt() and 0xff
                        )
                    )

                    ShortMessage.CHANNEL_PRESSURE -> channelPressure.trigger(
                        MidiEvent.channelPressure(
                            channel,
                            cmd[1].toInt() and 0xff
                        )
                    )
                    // https://sites.uci.edu/camp2014/2014/04/30/managing-midi-pitchbend-messages/
                    // The next operation to combine two 7bit values
                    // was verified to give the same results as the Linux
                    // `midisnoop` program while using an `Alesis Vortex
                    // Wireless 2` device. This MIDI device does not provide a
                    // full range 14 bit pitch-bend resolution though, so
                    // a different device is needed to confirm the pitch bend
                    // values slide as expected from -8192 to +8191.
                    ShortMessage.PITCH_BEND -> pitchBend.trigger(
                        MidiEvent.pitchBend(
                            channel,
                            (cmd[2].toInt() shl 25 shr 18) + cmd[1].toInt()
                        )
                    )
                }
            }
            override fun close() {
            }
        }

        // shut down midi if user calls `exitProcess(0)`
        Runtime.getRuntime().addShutdownHook(Destroyer())
    }

    val controlChanged = Event<MidiEvent>("midi-transceiver::controller-changed")
    val programChanged = Event<MidiEvent>("midi-transceiver::program-changed")
    val noteOn = Event<MidiEvent>("midi-transceiver::note-on")
    val noteOff = Event<MidiEvent>("midi-transceiver::note-off")
    val channelPressure = Event<MidiEvent>("midi-transceiver::channel-pressure")
    val pitchBend = Event<MidiEvent>("midi-transceiver::pitch-bend")

    fun controlChange(channel: Int, control: Int, value: Int) {
        try {
            val msg = ShortMessage(ShortMessage.CONTROL_CHANGE, channel, control, value)
            receiver.send(msg, receiverDevice.microsecondPosition)
        } catch (e: InvalidMidiDataException) {
            //
        }
    }

    fun programChange(channel: Int, program: Int) {
        try {
            val msg = ShortMessage(ShortMessage.PROGRAM_CHANGE, channel, program)
            receiver.send(msg, receiverDevice.microsecondPosition)
        } catch (e: InvalidMidiDataException) {
            //
        }
    }

    fun noteOn(channel: Int, key: Int, velocity: Int) {
        try {
            val msg = ShortMessage(ShortMessage.NOTE_ON, channel, key, velocity)
            receiver.send(msg, receiverDevice.microsecondPosition)
        } catch (e: InvalidMidiDataException) {
            //
        }
    }

    fun channelPressure(channel: Int, value: Int) {
        try {
            val msg = ShortMessage(ShortMessage.CHANNEL_PRESSURE, channel, value)
            receiver.send(msg, receiverDevice.microsecondPosition)
        } catch (e: InvalidMidiDataException) {
            //
        }
    }

    fun pitchBend(channel: Int, value: Int) {
        try {
            val msg = ShortMessage(ShortMessage.PITCH_BEND, channel, value)
            receiver.send(msg, receiverDevice.microsecondPosition)
        } catch (e: InvalidMidiDataException) {
            //
        }
    }

    fun destroy() {
        receiverDevice.close()
        transmitterDevicer.close()
    }
}

fun main() {
    val deviceName = "BCR2000"
    MidiDeviceDescription.list().forEach(::println)
    MidiDeviceDescription.list().firstOrNull { it.name.contains(deviceName) }
        ?.run {
            val controller = MidiTransceiver.fromDeviceVendor(name, vendor)
            controller.controlChanged.listen { println(it) }
            controller.programChanged.listen { println(it) }
            controller.noteOn.listen { println(it) }
            controller.noteOff.listen { println(it) }
            controller.channelPressure.listen { println(it) }
            controller.pitchBend.listen { println(it) }
        }
}