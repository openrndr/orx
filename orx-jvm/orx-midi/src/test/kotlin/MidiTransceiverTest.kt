package org.openrndr.extra.midi

import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.beInstanceOf
import io.mockk.*
import org.openrndr.Program
import java.util.concurrent.atomic.AtomicReference
import javax.sound.midi.MidiDevice
import javax.sound.midi.MidiMessage
import javax.sound.midi.Receiver
import javax.sound.midi.ShortMessage
import kotlin.test.Test

class MidiTransceiverTest {

    @Test
    fun `should send noteOn message`() {
        // given
        val program = mockk<Program>(relaxed = true)

        val receiver = mockk<Receiver>()
        val receiverDevice = mockk<MidiDevice>(relaxed = true)
        every { receiverDevice.receiver } returns receiver
        val messageSlot = slot<MidiMessage>()
        every { receiver.send(capture(messageSlot), any()) } just runs

        val transmitter = TestTransmitter()
        val transmitterDevice = mockk<MidiDevice>()
        every { transmitterDevice.transmitter } returns transmitter

        val transceiver = MidiTransceiver(
            program,
            receiverDevice,
            transmitterDevice
        )

        // when
        transceiver.noteOn(5, 10, 100)

        // then
        messageSlot.captured should beInstanceOf<ShortMessage>()
        val message = messageSlot.captured as ShortMessage
        message.command shouldBe ShortMessage.NOTE_ON
        message.channel shouldBe 5
        message.data1 shouldBe 10
        message.data2 shouldBe 100
    }

    @Test
    fun `should send noteOff message`() {
        // given
        val program = mockk<Program>(relaxed = true)

        val receiver = mockk<Receiver>()
        val receiverDevice = mockk<MidiDevice>(relaxed = true)
        every { receiverDevice.receiver } returns receiver
        val messageSlot = slot<MidiMessage>()
        every { receiver.send(capture(messageSlot), any()) } just runs

        val transmitter = TestTransmitter()
        val transmitterDevice = mockk<MidiDevice>()
        every { transmitterDevice.transmitter } returns transmitter

        val transceiver = MidiTransceiver(
            program,
            receiverDevice,
            transmitterDevice
        )

        // when
        transceiver.noteOff(1, 10, 62)

        // then
        messageSlot.captured should beInstanceOf<ShortMessage>()
        val message = messageSlot.captured as ShortMessage
        message.command shouldBe ShortMessage.NOTE_OFF
        message.channel shouldBe 1
        message.data1 shouldBe 10
        message.data2 shouldBe 62
    }

    @Test
    fun `should receive noteOn event`() {
        // given
        val program = mockk<Program>(relaxed = true)

        val receiver = mockk<Receiver>()
        val receiverDevice = mockk<MidiDevice>(relaxed = true)
        every { receiverDevice.receiver } returns receiver
        val messageSlot = slot<MidiMessage>()
        every { receiver.send(capture(messageSlot), any()) } just runs

        val transmitter = TestTransmitter()
        val transmitterDevice = mockk<MidiDevice>()
        every { transmitterDevice.transmitter } returns transmitter

        val transceiver = MidiTransceiver(
            program,
            receiverDevice,
            transmitterDevice
        )
        val eventSlot = AtomicReference<MidiEvent>()
        transceiver.noteOn.listen {
            eventSlot.set(it)
        }

        // when
        transmitter.receiver!!.send(
            ShortMessage(ShortMessage.NOTE_ON, 1, 2, 3), 1042
        )
        val noteOnEvent = eventSlot.get()

        // then
        noteOnEvent.apply {
            eventType shouldBe MidiEventType.NOTE_ON
            origin shouldBe MidiEvent.Origin.DEVICE
            channel shouldBe 1
            note shouldBe 2
            velocity shouldBe 3
        }
    }

    @Test
    fun `should receive noteOff event for NOTE_ON message with velocity 0`() {
        // given
        val program = mockk<Program>(relaxed = true)

        val receiver = mockk<Receiver>()
        val receiverDevice = mockk<MidiDevice>(relaxed = true)
        every { receiverDevice.receiver } returns receiver
        val messageSlot = slot<MidiMessage>()
        every { receiver.send(capture(messageSlot), any()) } just runs

        val transmitter = TestTransmitter()
        val transmitterDevice = mockk<MidiDevice>()
        every { transmitterDevice.transmitter } returns transmitter

        val transceiver = MidiTransceiver(
            program,
            receiverDevice,
            transmitterDevice
        )
        val eventSlot = AtomicReference<MidiEvent>()
        transceiver.noteOff.listen {
            eventSlot.set(it)
        }

        // when
        transmitter.receiver!!.send(
            ShortMessage(ShortMessage.NOTE_OFF, 2, 3, 0), 1042
        )
        val noteOnEvent = eventSlot.get()

        // then
        noteOnEvent.apply {
            eventType shouldBe MidiEventType.NOTE_OFF
            origin shouldBe MidiEvent.Origin.DEVICE
            channel shouldBe 2
            note shouldBe 3
            velocity shouldBe 0
        }
    }

}
