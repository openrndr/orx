import org.openrndr.extra.noise.fbm
import org.openrndr.extra.noise.simplex1D
import kotlin.math.absoluteValue
import kotlin.test.Test

class TestFbm {
    @Test
    fun testFbmOctaveImpact() {
        val octavesLow = 1
        val octavesHigh = 8
        val samples = 500
        val step = 0.02

        val noiseLow = simplex1D.fbm(octavesLow)
        val noiseHigh = simplex1D.fbm(octavesHigh)

        val valuesLow = List(samples) { noiseLow(123, it * step) }
        val valuesHigh = List(samples) { noiseHigh(123, it * step) }

        val roughnessLow = valuesLow.zipWithNext().map {
            (a, b) -> (a - b).absoluteValue
        }.average()

        val roughnessHigh = valuesHigh.zipWithNext().map {
            (a, b) -> (a - b).absoluteValue
        }.average()

        assert(roughnessHigh > roughnessLow * 1.5) {
            "fbm with $octavesHigh octaves (roughness=$roughnessHigh) " +
                    "should be significantly rougher than with $octavesLow octave (roughness=$roughnessLow"
        }
    }
}