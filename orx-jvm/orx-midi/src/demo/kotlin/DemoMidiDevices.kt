import org.openrndr.application
import org.openrndr.extra.midi.MidiConsole
import org.openrndr.extra.midi.MidiDeviceDescription
import org.openrndr.extra.midi.MidiTransceiver

fun main() {
    application {
        program {
            MidiDeviceDescription.list().forEach { println(it.toString()) }
            val midi = MidiTransceiver.fromDeviceVendor(this,"Launchpad [hw:4,0,0]", "ALSA (http://www.alsa-project.org)")
            extend(MidiConsole()) {
                register(midi)
            }
        }
    }
}