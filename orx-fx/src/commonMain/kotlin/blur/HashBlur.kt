@file:Suppress("RUNTIME_ANNOTATION_NOT_SUPPORTED")

package org.openrndr.extra.fx.blur

import org.openrndr.draw.ShadeStyleFilter1to1
import org.openrndr.draw.shadeStyle
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.DoubleParameter
import org.openrndr.extra.parameters.IntParameter
import org.openrndr.extra.parameters.NestedParameter
import org.openrndr.extra.shadestyles.presets.ConstantScalar
import org.openrndr.extra.shadestyles.presets.ConstantVector2
import org.openrndr.extra.shadestyles.presets.ScalarPreset
import org.openrndr.extra.shadestyles.presets.Vector2Preset
import org.openrndr.extra.shadestyles.presets.presetSlot
import org.openrndr.math.Vector2

/**
 * Hash blur, based on Hashed blur by David Hoskins.
 *
 * Every pixel averages [samples] randomly placed samples from a disc with radius [radius]², optionally spread out
 * along a line in [direction] over [spread] pixels. The radius scale and the direction are presets that can vary per
 * pixel, for example by reading them from textures:
 *
 * ```
 * blur.radiusScale = TextureScalar(radiusMap)       // multiplies by the red channel
 * blur.direction = TextureVector2(directionMap)     // reads the red and green channels
 * ```
 *
 * Replacing a preset with one of the same class does not recompile the shader.
 *
 * License Creative Commons Attribution-NonCommercial-ShareAlike 3.0 Unported License.
 */
@Description("Hash blur")
class HashBlur : ShadeStyleFilter1to1(shadeStyle {
    fragmentPreamble = """
        #define HASHBLUR_TAU 6.28318530718
        vec2 hashblur_hash22(vec2 p) {
            vec3 p3 = fract(vec3(p.xyx) * 443.8975);
            p3 += dot(p3, p3.yzx + 19.19);
            return fract(vec2((p3.x + p3.y) * p3.z, (p3.x + p3.z) * p3.y));
        }
        vec2 hashblur_circle(inout vec2 r) {
            r = fract(r * vec2(33.3983, 43.4427));
            return sqrt(r.x + .001) * vec2(sin(r.y * HASHBLUR_TAU), cos(r.y * HASHBLUR_TAU)) * .5;
        }
        vec2 hashblur_line(inout vec2 r, vec2 direction) {
            r = fract(r * vec2(33.3983, 43.4427));
            return (r.x + .001) * direction;
        }
    """.trimIndent()
    fragmentTransform = """
        vec2 uv = c_textureCoordinate;
        vec2 texel = vec2(1.0) / vec2(textureSize(p_input0, 0));
        vec2 circle = vec2(p_radius * p_radius * p_radiusScale) * texel;
        vec2 line = vec2(p_spread) * texel;
        vec2 direction = p_direction;
        vec2 randomC = hashblur_hash22(uv + vec2(p_time));
        vec2 randomL = randomC;
        vec4 acc = vec4(0.0);
        for (int i = 0; i < p_samples; i++) {
            vec2 offset = circle * hashblur_circle(randomC) + line * hashblur_line(randomL, direction);
            acc += textureLod(p_input0, uv + offset, 0.0);
        }
        x_fill = acc / float(p_samples);
        x_fill.rgb *= p_gain;
    """.trimIndent()
}) {
    /**
     * Blur radius in pixels, default is 5.0
     */
    @DoubleParameter("blur radius", 1.0, 25.0)
    var radius: Double by Parameter("radius", 5.0)

    /**
     * Time/seed, this should be fed with seconds, default is 0.0
     */
    var time: Double by Parameter("time", 0.0)

    /**
     * Number of samples, default is 30
     */
    @IntParameter("number of samples", 1, 100)
    var samples: Int by Parameter("samples", 30)

    /**
     * Post-blur gain, default is 1.0
     */
    @DoubleParameter("image gain", 0.0, 2.0)
    var gain: Double by Parameter("gain", 1.0)

    /**
     * Length in pixels of the line along [direction] that samples are spread over, default is 25.0
     */
    @DoubleParameter("blur spread", 0.0, 25.0)
    var spread: Double by Parameter("spread", 25.0)

    /**
     * Scale of the squared radius, can vary per pixel, for example with `TextureScalar(map)`. The default is a
     * [ConstantScalar] of 1.0.
     */
    @NestedParameter("radius scale")
    var radiusScale: ScalarPreset by presetSlot("radiusScale", ScalarPreset.CONTRACT, ConstantScalar(1.0))

    /**
     * Direction of the line blur, scaled by [spread], can vary per pixel, for example with `TextureVector2(map)`.
     * The default, a [ConstantVector2] of [Vector2.ZERO], blurs a disc only.
     */
    @NestedParameter("direction")
    var direction: Vector2Preset by presetSlot("direction", Vector2Preset.CONTRACT, ConstantVector2(Vector2.ZERO))
}
