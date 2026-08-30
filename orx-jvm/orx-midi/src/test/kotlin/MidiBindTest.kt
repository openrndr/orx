package org.openrndr.extra.midi

import io.kotest.matchers.shouldBe
import io.mockk.*
import org.openrndr.Dispatcher
import org.openrndr.Program
import org.openrndr.extra.parameters.DoubleParameter
import javax.sound.midi.MidiDevice
import javax.sound.midi.MidiMessage
import javax.sound.midi.Receiver
import javax.sound.midi.ShortMessage
import kotlin.test.Test

@Suppress("MemberVisibilityCanBePrivate")
class MidiBindTest {

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
        every { program.dispatcher } returns Dispatcher()
    }

    val transceiver = MidiTransceiver(
        program,
        receiverDevice,
        transmitterDevice
    )

    @Test
    fun testReceive() {
        val settings = object {
            @DoubleParameter("radius", 0.0, 100.0)
            var radius = 0.0
        }

        program.bindMidiControl(settings::radius, transceiver, 5, 3)

        // when: simulate incoming MIDI from the device
        transmitter.receiver!!.send(
            ShortMessage(ShortMessage.CONTROL_CHANGE, 5, 3, 127),
            1042
        )

        // then
        settings.radius shouldBe 100.0
    }

    @Test
    fun `receiving a pitch bend message should update the bound variable`() {
        val settings = object {
            @DoubleParameter("radius", -1.0, 1.0)
            var radius = 0.0
        }

        program.bindMidiPitchBend(settings::radius, transceiver, 5)

        val value = 8191 // pitch bend range is -8192 .. 8191
        val lsb = value and 0x7F
        val msb = (value shr 7) and 0x7F

        // when: simulate incoming MIDI from the device
        transmitter.receiver!!.send(
            ShortMessage(ShortMessage.PITCH_BEND, 5, lsb, msb),
            1042
        )

        // then
        settings.radius shouldBe 1.0
    }
}
