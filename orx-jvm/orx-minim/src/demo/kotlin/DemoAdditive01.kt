import ddf.minim.ugens.Oscil
import ddf.minim.ugens.Pan
import org.openrndr.application
import org.openrndr.color.rgb
import org.openrndr.extra.minim.minim
import org.openrndr.math.Polar
import kotlin.math.pow
import kotlin.random.Random

/**
 * Random drone generator and visualizer with 20 stereo voices.
 * Hold the mouse button to randomize the frequencies.
 * Press keys 'a' or 'b' for less random frequencies.
 */
fun main() {
    application {
        program {
            val minim = minim()
            val out = minim.lineOut

            // generates a random frequency value biased down
            fun randomFreq() = 20f + Random.nextFloat().pow(3) * 1000

            // If one didn't want to visualize or control the synths we
            // wouldn't need a data structure to store them. Here we store
            // Pairs, so we have access both to the frequency of the wave
            // and the current amplitude defined by the lfo (low frequency
            // oscillator).
            val synths = List(20) {
                // By default Oscil creates sine waves, but it can be changed.
                val lfo = Oscil(
                    Random.nextFloat() * 0.1f + 0.005f,
                    0.05f
                ).apply {
                    // Here we set the center of the lfo to 0.05f.
                    // Since the amplitude is also 0.05f, it moves between
                    // 0.00f and 0.10f.
                    offset.lastValue = 0.05f

                    // Have the sine waves to not start in sync.
                    //phase.lastValue = Random.nextFloat() * 6.28f
                }
                val wave = Oscil(randomFreq(), 0f)
                // The `lfo` Oscil controls the `wave` Oscil's amplitude.
                lfo.patch(wave.amplitude)
                // Random pan to avoid a mono sound.
                val pan = Pan(Random.nextFloat() * 2 - 1)
                wave.patch(pan)
                pan.patch(out)
                // Store a [Pair] in `synths`.
                Pair(wave, lfo)
            }
            val bgColor = rgb(0.094, 0.188, 0.349)
            val lineColor = rgb(0.992, 0.918, 0.671)

            extend {
                drawer.clear(bgColor)
                drawer.translate(drawer.bounds.center)
                drawer.rotate(seconds)
                // A CircleBatchBuilder for faster drawing of circles.
                drawer.circles {
                    // For each synth draw a circle.
                    synths.forEachIndexed { i, (wave, lfo) ->
                        stroke = lineColor.opacify(Random.nextDouble(0.4) + 0.6)
                        fill = lineColor.opacify(Random.nextDouble() * 0.04)
                        // A Polar arrangement centered on the screen.
                        // Higher pitch circles are farther away from the center.
                        val pos = Polar(
                            360.0 * i / synths.size,
                            50.0 + wave.frequency.lastValue * 0.2
                        ).cartesian
                        // The size of the circle depends on the current volume
                        // set by the lfo.
                        circle(pos, 500 * lfo.lastValues.last().toDouble())
                    }
                }
                if (mouse.pressedButtons.isNotEmpty()) {
                    synths.random().first.setFrequency(randomFreq())
                }
            }
            keyboard.keyDown.listen { key ->
                when (key.name) {
                    "a" -> {
                        // make all frequencies close to a base frequency
                        // (circular arrangement)
                        val baseFreq = 20 + Random.nextFloat() * 200
                        synths.forEach {
                            it.first.setFrequency(baseFreq + Random.nextFloat() * 20)
                        }
                    }

                    "b" -> {
                        // make all frequencies follow an exponential series
                        // (spiral arrangement)
                        val inc = Random.nextFloat() * 0.1f
                        synths.forEachIndexed { i, (wave, _) ->
                            wave.setFrequency(25f.pow(1f + i * inc))
                        }
                    }
                }
            }
        }
    }
}