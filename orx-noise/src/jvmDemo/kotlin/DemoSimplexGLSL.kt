import org.openrndr.application
import org.openrndr.color.rgb
import org.openrndr.draw.colorBuffer
import org.openrndr.extra.noise.Random
import org.openrndr.extra.noise.filters.*
import org.openrndr.math.Vector3
import org.openrndr.math.Vector4
import kotlin.math.sin

/**
 * A sine oscillator with randomized parameters
 */
class SinOsc {
    private val freq = Random.double(0.1, 2.0)
    private val phase = Random.double(0.0, 6.28)
    private val add = Random.double(0.0, 1.0)
    private val mul = Random.double(0.0, 1.0 - add)
    operator fun invoke() = sin(System.currentTimeMillis() * 0.0001 * freq + phase) * mul + add
}

/**
 * Render an animated Simplex3D texture using shaders.
 *
 * The uniforms in the shader are controlled by
 * randomized sine oscillators.
 */
fun main() = application {
    program {
        val noise = SimplexNoise3D()
        val img = colorBuffer(width, height)
        val wav = List(21) { SinOsc() }

        extend {
            noise.seed = Vector3(wav[0](), wav[1](), wav[2]()) // = position
            noise.scale = Vector3(wav[3](), wav[4](), wav[5]())
            noise.lacunarity = Vector3(wav[6](), wav[7](), wav[8]())
            noise.gain = Vector4(wav[9](), wav[10](), wav[11](), wav[12]())
            noise.decay = Vector4(wav[13](), wav[14](), wav[15](), wav[16]())
            noise.octaves = 4
            noise.bias = Vector4(wav[17](), wav[18](), wav[19](), wav[20]())

            noise.apply(emptyArray(), img)
            drawer.clear(rgb(0.20, 0.18, 0.16))
            drawer.image(img)
        }
    }
}
