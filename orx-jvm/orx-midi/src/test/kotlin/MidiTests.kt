package org.openrndr.extra.midi

import javax.sound.midi.Receiver
import javax.sound.midi.Transmitter

class TestTransmitter : Transmitter {

    private var receiver: Receiver? = null

    override fun setReceiver(receiver: Receiver?) {
        this.receiver = receiver
    }

    override fun getReceiver(): Receiver? = receiver

    override fun close() {
        receiver?.close()
    }

}