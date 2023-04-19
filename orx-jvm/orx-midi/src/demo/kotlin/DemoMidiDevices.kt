import org.openrndr.application
import org.openrndr.extra.midi.MidiDeviceDescription
import org.openrndr.extra.midi.MidiTransceiver
import org.openrndr.extra.midi.bindMidiControl
import org.openrndr.extra.parameters.DoubleParameter

fun main() {
    application {
        program {
            //MidiDeviceDescription.list().forEach { println(it.toString()) }
            val midi = MidiTransceiver.fromDeviceVendor(this,"MIDI2x2 [hw:3,0,0]", "ALSA (http://www.alsa-project.org)")
            midi.controlChanged.listen {
                println(it)
            }
        }
    }
}