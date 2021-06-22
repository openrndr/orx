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

data class MidiDeviceDescription(val name: String, val vendor: String, val receive: Boolean, val transmit: Boolean) {
    companion object {
        fun list(): List<MidiDeviceDescription> {
            val caps = mutableMapOf<MidiDeviceName, MidiDeviceCapabilities>()

            val infos = MidiSystem.getMidiDeviceInfo()
            for (info in infos) {
                val device = MidiSystem.getMidiDevice(info)
                val name = MidiDeviceName(info.name, info.vendor)
                val deviceCaps = caps.getOrPut(name) { MidiDeviceCapabilities() }

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
                MidiDeviceDescription(it.key.name, it.key.vendor, it.value.receive, it.value.transmit)
            }
        }
    }

    fun open() : MidiTransceiver {
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
    private inner class Destroyer: Thread() {
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
                    ShortMessage.NOTE_ON -> noteOn.trigger(MidiEvent.noteOn(channel, cmd[1].toInt() and 0xff, cmd[2].toInt() and 0xff))
                    ShortMessage.NOTE_OFF -> noteOff.trigger(MidiEvent.noteOff(channel, cmd[1].toInt() and 0xff))
                    ShortMessage.CONTROL_CHANGE -> controlChanged.trigger(MidiEvent.controlChange(channel,cmd[1].toInt() and 0xff, cmd[2].toInt() and 0xff))
                    ShortMessage.PROGRAM_CHANGE -> programChanged.trigger(MidiEvent.programChange(channel,cmd[1].toInt() and 0xff))
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

    fun controlChange(channel: Int, control: Int, value: Int) {
        try {
            val msg = ShortMessage(ShortMessage.CONTROL_CHANGE, channel, control, value)
            if (receiverDevice != null) {
                val tc = receiverDevice.microsecondPosition
                receiver.send(msg, tc)
            }
        } catch (e: InvalidMidiDataException) {
            //
        }
    }

    fun programChange(channel: Int, program: Int) {
        try {
            val msg = ShortMessage(ShortMessage.PROGRAM_CHANGE, channel, program)
            if (receiverDevice != null) {
                val tc = receiverDevice.microsecondPosition
                receiver.send(msg, tc)
            }
        } catch (e: InvalidMidiDataException) {
            //
        }
    }

    fun noteOn(channel: Int, key: Int, velocity: Int) {
        try {
            val msg = ShortMessage(ShortMessage.NOTE_ON, channel, key, velocity)
            if (receiverDevice != null) {
                val tc = receiverDevice.microsecondPosition
                receiver.send(msg, tc)
            }
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
    MidiDeviceDescription.list().forEach {
        println("> ${it.name}, ${it.vendor} r:${it.receive} t:${it.transmit}")
    }
    val dev = MidiTransceiver.fromDeviceVendor("BCR2000 [hw:2,0,0]", "ALSA (http://www.alsa-project.org)")
    dev.controlChanged.listen {
        println("${it.channel} ${it.control} ${it.value}")
    }
}