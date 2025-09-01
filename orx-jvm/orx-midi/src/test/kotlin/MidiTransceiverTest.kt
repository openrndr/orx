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

@Suppress("MemberVisibilityCanBePrivate")
class MidiTransceiverTest {

    // given
    val program = mockk<Program>(relaxed = true)
    val receiver = mockk<Receiver>()
    val receiverDevice = mockk<MidiDevice>(relaxed = true)
    val messageSlot = slot<MidiMessage>()

    val transmitter = TestTransmitter()
    val transmitterDevice = mockk<MidiDevice>()

    init {
        every { receiverDevice.receiver } returns receiver
        every { receiver.send(capture(messageSlot), any()) } just runs
        every { transmitterDevice.transmitter } returns transmitter
    }

    val transceiver = MidiTransceiver(
        program,
        receiverDevice,
        transmitterDevice
    )

    @Test
    fun `should send out NOTE_ON message`() {
        // when
        transceiver.noteOn(5, 10, 100)

        // then
        messageSlot.captured should beInstanceOf<ShortMessage>()
        (messageSlot.captured as ShortMessage).apply {
            command shouldBe ShortMessage.NOTE_ON
            channel shouldBe 5
            data1 shouldBe 10
            data2 shouldBe 100
        }

    }

    @Test
    fun `should send out NOTE_OFF message`() {
        // when
        transceiver.noteOff(1, 10, 62)

        // then
        messageSlot.captured should beInstanceOf<ShortMessage>()
        (messageSlot.captured as ShortMessage).apply {
            command shouldBe ShortMessage.NOTE_OFF
            channel shouldBe 1
            data1 shouldBe 10
            data2 shouldBe 62
        }
    }

    @Test
    fun `should receive NOTE_ON event on receiving NOTE_ON message`() {
        // given
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
    fun `should receive NOTE_OFF event on receiving NOTE_ON message with velocity 0`() {
        // given
        val eventSlot = AtomicReference<MidiEvent>()
        transceiver.noteOff.listen {
            eventSlot.set(it)
        }

        // when
        transmitter.receiver!!.send(
            ShortMessage(ShortMessage.NOTE_ON, 2, 3, 0), 1042
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
