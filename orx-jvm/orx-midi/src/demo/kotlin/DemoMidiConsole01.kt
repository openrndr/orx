import org.openrndr.application
import org.openrndr.extra.midi.MidiConsole
import org.openrndr.extra.midi.listMidiDevices
import org.openrndr.extra.midi.openMidiDevice

/**
 * Demonstration of [MidiConsole]
 */
fun main() {
    application {
        program {
            listMidiDevices().forEach { println(it.toString()) }
            val midi = openMidiDevice("Launchpad [hw:4,0,0]")
            extend(MidiConsole()) {
                register(midi)
            }
        }
    }
}