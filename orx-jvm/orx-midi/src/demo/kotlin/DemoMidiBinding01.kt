import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.midi.bindMidiControl
import org.openrndr.extra.midi.openMidiDevice
import org.openrndr.extra.parameters.ColorParameter
import org.openrndr.extra.parameters.DoubleParameter
import org.openrndr.math.Vector2

/**
 * Demonstration of two-way binding using [bindMidiControl]
 */
fun main() {
    application {
        program {
            val midi = openMidiDevice("MIDI2x2 [hw:3,0,0]")
            val settings = object {
                @DoubleParameter("radius", 0.0, 100.0)
                var radius = 0.0

                @DoubleParameter("x", -100.0, 100.0)
                var x = 0.0

                @DoubleParameter("y", -100.0, 100.0)
                var y = 0.0

                @ColorParameter("fill")
                var color = ColorRGBa.WHITE
            }

            bindMidiControl(settings::radius, midi, 0, 1)
            bindMidiControl(settings::x, midi, 0, 2)
            bindMidiControl(settings::y, midi, 0, 3)
            bindMidiControl(settings::color, midi, 0, 4)

            extend {
                drawer.fill = settings.color
                drawer.circle(drawer.bounds.center + Vector2(settings.x, settings.y), settings.radius)
            }
        }
    }
}