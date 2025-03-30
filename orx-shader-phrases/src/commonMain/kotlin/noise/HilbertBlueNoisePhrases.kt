package org.openrndr.extra.shaderphrases.noise

import org.openrndr.extra.shaderphrases.spacefilling.hilbertPhrase
import org.openrndr.extra.shaderphrases.spacefilling.hilbertV3Phrase
import org.openrndr.extra.shaderphrases.spacefilling.inverseGray32Phrase

// https://www.shadertoy.com/view/3tB3z3
val hilbertR1BlueNoisePhrase = """#ifndef SP_HILBERT_R1_BLUE_NOISE
#define SP_HILBERT_R1_BLUE_NOISE
$hilbertPhrase
$kmhfPhrase
uint hilbertR1BlueNoise(uvec2 p, uint bits, uint seed) {
    uint x = uint(hilbert(ivec2(p), int(bits))) % (1u << bits) + seed;
    x = kmhf(x);
    return x;
}
#endif
"""

val hilbertR1BlueNoiseFloatPhrase = """#ifndef SP_HILBERT_R1_BLUE_NOISE_FLOAT
#define SP_HILBERT_R1_BLUE_NOISE_FLOAT
$hilbertR1BlueNoisePhrase
float hilbertR1BlueNoiseFloat(uvec2 p, uint bits, uint seed) {
    uint x = hilbertR1BlueNoise(p, bits, seed);
    return float(x) / 4294967296.0;
}
#endif
"""

// https://www.shadertoy.com/view/3tB3z3
val inverseR1BlueNoisePhrase = """#ifndef SP_INVERSE_R1_BLUE_NOISE
#define SP_INVERSE_R1_BLUE_NOISE
$inverseGray32Phrase
$inverseKmhfPhrase
ivec2 inverseR1BlueNoise(uint x, uint bits) {
    x = inverseKmhf(x);
    return uvec2(inverseHilbert(int(x), int(bits)));}
#endif"""

// https://www.shadertoy.com/view/3tB3z3
val hilbertR1BlueNoiseV3Phrase = """#ifndef SP_HILBERT_R1_BLUE_NOISE_V3
#define SP_HILBERT_R1_BLUE_NOISE_V3
$hilbertV3Phrase
$kmhfPhrase
uint hilbertR1BlueNoise(uvec3 p, uint bits, uint seed) {
    uint x = uint(hilbert(ivec3(p), int(bits))) % (1u << bits) + seed;
    x = kmhf(x);
    return x;
}
#endif
"""

val hilbertR1BlueNoiseFloatV3Phrase = """#ifndef SP_HILBERT_R1_BLUE_NOISE_FLOAT_V3
#define SP_HILBERT_R1_BLUE_NOISE_FLOAT_V3
$hilbertR1BlueNoiseV3Phrase
float hilbertR1BlueNoiseFloat(uvec3 p, uint bits, uint seed) {
    uint x = hilbertR1BlueNoise(p, bits, seed);
    return float(x) / 4294967296.0;
}
#endif
"""