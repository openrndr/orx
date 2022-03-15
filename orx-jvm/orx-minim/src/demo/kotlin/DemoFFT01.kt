import ddf.minim.Minim
import ddf.minim.analysis.FFT
import ddf.minim.analysis.LanczosWindow

import org.openrndr.application
import org.openrndr.extra.minim.minim
import org.openrndr.math.map
import kotlin.math.ln

fun main() {
    application {
        configure {
            width = 1280
            height = 720
        }

        program {
            val minim = minim()
            val lineIn = minim.getLineIn(Minim.MONO, 2048, 48000f)
            val fft = FFT(lineIn.bufferSize(), lineIn.sampleRate())
            fft.window(LanczosWindow())
            extend {
                fft.forward(lineIn.mix)
                for (i in 0 until 200) {
                    val bandDB = 20.0 * ln(2.0 * fft.getBand(i) / fft.timeSize())
                    drawer.rectangle(i * 5.0, height / 2.0, 5.0, bandDB.map(0.0, -150.0, 0.0, -height / 8.0))
                }
            }
        }
    }
}