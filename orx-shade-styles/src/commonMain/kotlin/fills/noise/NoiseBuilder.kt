package org.openrndr.extra.shadestyles.fills.noise

import org.openrndr.draw.ObservableHashmap
import org.openrndr.draw.ShadeStyle
import org.openrndr.draw.StyleParameters
import org.openrndr.extra.shaderphrases.noise.*
import org.openrndr.math.Matrix44
import org.openrndr.math.transforms.transform
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

class NoiseBuilder : StyleParameters {
    override var parameterTypes: ObservableHashmap<String, String> = ObservableHashmap(mutableMapOf()) {}
    override var parameterValues: MutableMap<String, Any> = mutableMapOf()
    override var textureBaseIndex: Int = 2

    var scaleToSize: Boolean by Parameter("noiseScaleToSize", false)

    var filterWindow: Int by Parameter("noiseFilterWindow", 1)

    var transform: Matrix44 by Parameter("noiseTransform", Matrix44.IDENTITY)
    var domainWarpFunction = """vec3 domainWarp(vec3 p) { return p; }"""
    var levelWarpFunction = """float levelWarp(vec3 p, float l) { return l; }"""
    var noiseFunction = """float noise(vec3 p) { return fract(sin(dot(p.xy, vec2(12.9898,78.233))) * 43758.5453);}"""
    var fbmFunction = """float fbm(vec3 p) { return noise(p); }"""
    var blendFunction = """vec4 blend(vec4 o, float n) { return vec4(o.rgb * n, o.a); }"""

    var phase: Double by Parameter("noisePhase", 0.0)

    inner class WhiteNoiseBuilder {
        init {
            noiseFunction = """
            $fhash12Phrase
            float noise(vec3 p) { return fhash12(vec2(p.x,p.y)); }
        """.trimIndent()
        }

        fun bilinear() {
            scaleToSize = true
            noiseFunction = """
            $fhash12Phrase
            float noise(vec3 p) {
                uvec2 up00 = uvec2(p.xy * 1.0);
                uvec2 up10 = uvec2(p.xy * 1.0) + uvec2(1u, 0u);
                uvec2 up01 = uvec2(p.xy * 1.0) + uvec2(0u, 1u);
                uvec2 up11 = uvec2(p.xy * 1.0) + uvec2(1u, 1u);
                
                uint seed = uint(0);
                float n00 = fhash12(vec2(up00));
                float n10 = fhash12(vec2(up10));
                float n01 = fhash12(vec2(up01));
                float n11 = fhash12(vec2(up11));
                
                vec2 f = fract(p.xy);                
                return (n00 * (1.0 -f.x) * (1.0 -f.y) +
                       n10 * f.x * (1.0 -f.y) + 
                       n01 * (1.0 -f.x) * f.y + 
                       n11 * f.x * f.y);
            }
        """.trimIndent()
        }

    }
    @OptIn(ExperimentalContracts::class)
    fun whiteNoise(builder: WhiteNoiseBuilder.() -> Unit) {
        contract {
            callsInPlace(builder, kotlin.contracts.InvocationKind.EXACTLY_ONCE)
        }
        WhiteNoiseBuilder().builder()
    }

    inner class SimplexBuilder {}

    @OptIn(ExperimentalContracts::class)
    fun simplex(builder: SimplexBuilder.() -> Unit) {
        contract {
            callsInPlace(builder, kotlin.contracts.InvocationKind.EXACTLY_ONCE)
        }
        noiseFunction = """
            ${simplex13}
            float noise(vec3 p) { return simplex13(vec3(p)); }
        """.trimIndent()
    }

    inner class FBMBuilder {
        var octaves: Int by Parameter("noiseOctaves", 1)
        var frequency: Double by Parameter("noiseFrequency", 1.0)
        var lacunarity: Double by Parameter("noiseLacunarity", 2.0)
        var gain: Double by Parameter("noiseGain", 0.5)
    }

    inner class BlueNoiseBuilder {
        var scale: Double by Parameter("noiseScale", 1.0)
        var bits: Int by Parameter("noiseBits", 17)

        fun bilinear() {
            noiseFunction = """
            $hilbertR1BlueNoiseFloatPhrase
            float noise(vec3 p) {
                uvec2 up00 = uvec2(p.xy * 1.0);
                uvec2 up10 = uvec2(p.xy * 1.0) + uvec2(1u, 0u);
                uvec2 up01 = uvec2(p.xy * 1.0) + uvec2(0u, 1u);
                uvec2 up11 = uvec2(p.xy * 1.0) + uvec2(1u, 1u);
                
                uint seed = uint(0);
                uint bits = uint(p_noiseBits);
                float n00 = hilbertR1BlueNoiseFloat(up00, bits, seed);
                float n10 = hilbertR1BlueNoiseFloat(up10, bits, seed);
                float n01 = hilbertR1BlueNoiseFloat(up01, bits, seed);
                float n11 = hilbertR1BlueNoiseFloat(up11, bits, seed);
                
                vec2 f = fract(p.xy);
                
                return (n00 * (1.0 -f.x) * (1.0 -f.y) +
                       n10 * f.x * (1.0 -f.y) + 
                       n01 * (1.0 -f.x) * f.y + 
                       n11 * f.x * f.y);
            }
        """.trimIndent()
        }

        fun trilinear() {
            noiseFunction = """
                            
            $hilbertR1BlueNoiseFloatV3Phrase
            float noise(vec3 p) {
                uvec3 up000 = uvec3(p * 1.0);
                uvec3 up100 = uvec3(p * 1.0) + uvec3(1u, 0u, 0u);
                uvec3 up010 = uvec3(p * 1.0) + uvec3(0u, 1u, 0u);
                uvec3 up110 = uvec3(p * 1.0) + uvec3(1u, 1u, 0u);
                
                uvec3 up001 = uvec3(p * 1.0) + uvec3(0u, 0u, 1u);
                uvec3 up101 = uvec3(p * 1.0) + uvec3(1u, 0u, 1u);
                uvec3 up011 = uvec3(p * 1.0) + uvec3(0u, 1u, 1u);
                uvec3 up111 = uvec3(p * 1.0) + uvec3(1u, 1u, 1u);

                uint seed = 0u;
                uint bits = uint(p_noiseBits);
                float n000 = hilbertR1BlueNoiseFloat(up000, bits, seed);
                float n100 = hilbertR1BlueNoiseFloat(up100, bits, seed);
                float n010 = hilbertR1BlueNoiseFloat(up010, bits, seed);
                float n110 = hilbertR1BlueNoiseFloat(up110, bits, seed);

                float n001 = hilbertR1BlueNoiseFloat(up001, bits, seed);
                float n101 = hilbertR1BlueNoiseFloat(up101, bits, seed);
                float n011 = hilbertR1BlueNoiseFloat(up011, bits, seed);
                float n111 = hilbertR1BlueNoiseFloat(up111, bits, seed);

                vec3 f = fract(p);
                f.z = 0.0;
                
                return (n000 * (1.0 -f.x) * (1.0 -f.y) * (1.0 - f.z) +
                        n100 * f.x * (1.0 -f.y) * (1.0 - f.z)+ 
                        n010 * (1.0 -f.x) * f.y * (1.0 - f.z)+ 
                        n110 * f.x * f.y * (1.0 - f.z) + 
                        n001 * (1.0 -f.x) * (1.0 -f.y) * f.z +
                        n101 * f.x * (1.0 -f.y) * f.z + 
                        n011 * (1.0 -f.x) * f.y * f.z + 
                        n111 * f.x * f.y * f.z);
            }
        """.trimIndent()
        }


        init {
            noiseFunction = """
            $hilbertR1BlueNoiseFloatPhrase
            float noise(vec3 p) {
                uvec2 up00 = uvec2(p.xy * 1.0);
                uint seed = uint(p.z);
                uint bits = uint(p_noiseBits);
                float n00 = hilbertR1BlueNoiseFloat(up00, bits, seed);
                return n00;                
            }
            """.trimIndent()
        }
    }

    @OptIn(ExperimentalContracts::class)
    fun blueNoise(builder: BlueNoiseBuilder.() -> Unit) {
        contract {
            callsInPlace(builder, kotlin.contracts.InvocationKind.EXACTLY_ONCE)
        }
        scaleToSize = true

        BlueNoiseBuilder().apply { builder() }
    }

    @OptIn(ExperimentalContracts::class)
    fun fbm(builder: FBMBuilder.() -> Unit) {
        contract {
            callsInPlace(builder, kotlin.contracts.InvocationKind.EXACTLY_ONCE)
        }
        FBMBuilder().apply { builder() }

        fbmFunction = """
            float fbm(vec3 p) {
                float f = 0.0;
                float amp = 1.0;
                for (int i = 0; i < p_noiseOctaves; i++) {
                    f += noise(p) * amp;
                    p *= p_noiseLacunarity;
                    amp *= p_noiseGain;
                }
                return f;
            }""".trimIndent()
    }

    inner class AnisotropicFBMBuilder {
        var octaves: Int by Parameter("noiseOctaves", 1)
        var lacunarity: Matrix44 by Parameter("noiseLacunarity", transform { scale(2.0, 2.0, 1.0) })
        var decay: Double by Parameter("noiseDecay", 0.5)
        var warpFactor: Double by Parameter("warpFactor", 1.0)
    }

    @OptIn(ExperimentalContracts::class)
    fun anisotropicFbm(builder: AnisotropicFBMBuilder.() -> Unit) {
        contract {
            callsInPlace(builder, kotlin.contracts.InvocationKind.EXACTLY_ONCE)
        }
        AnisotropicFBMBuilder().apply { builder() }
        fbmFunction = """float fbm(vec3 p) {
            float f = 0.0;
            float amp = 1.0;
            for (int i = 0; i < p_noiseOctaves; i++) {
                f += abs(noise(p) * amp);
                p = (p_noiseLacunarity * vec4(p, 1.0)).xyz;
                p = mix(p, noiseDomainWarp(p), p_warpFactor);
                amp *= p_noiseDecay;
            }
            return f;
        }"""
    }

    fun build(): ShadeStyle {
        return NoiseBase(
            parameterValues,
            domainWarpFunction,
            noiseFunction,
            fbmFunction,
            levelWarpFunction,
            blendFunction
        ).apply {
            this.parameterTypes.putAll(this@NoiseBuilder.parameterTypes)
        }
    }
}

@OptIn(ExperimentalContracts::class)
fun noise(builder: NoiseBuilder.() -> Unit): ShadeStyle {
    contract {
        callsInPlace(builder, kotlin.contracts.InvocationKind.EXACTLY_ONCE)
    }
    val b = NoiseBuilder()
    b.builder()
    return b.build()
}