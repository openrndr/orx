@file:Suppress("RUNTIME_ANNOTATION_NOT_SUPPORTED")

package org.openrndr.extra.fx.distort

import org.openrndr.draw.*
import org.openrndr.extra.parameters.*
import org.openrndr.extra.shadestyles.presets.NoiseWarp
import org.openrndr.extra.shadestyles.presets.WarpPreset
import org.openrndr.extra.shadestyles.presets.presetSlot
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle

/**
 * Distorts its input by displacing texture coordinates with a [warp] preset.
 *
 * The warp maps a texture coordinate, with [offset] and segmentation applied, to the coordinate to read from; the
 * difference is the displacement. By default it is a [NoiseWarp], fractal simplex noise. Assigning a warp of
 * another class compiles a shader once per class; changing the properties of a warp does not recompile.
 *
 * ```
 * val noise = NoiseWarp()
 * perturb.warp = noise
 * noise.phase = seconds * 0.1
 * perturb.warp = ChainWarp(Ripple(), Swirl())
 * ```
 */
@Description("Perturb")
class Perturb : ShadeStyleFilter1to1(shadeStyle {
    fragmentPreamble = """
        vec2 perturb_segment(vec2 t, int x, int y) {
            float sx = x == 0 ? t.x : floor(t.x * float(x)) / float(x);
            float sy = y == 0 ? t.y : floor(t.y * float(y)) / float(y);
            return vec2(sx, sy);
        }
    """.trimIndent()
    fragmentTransform = """
        vec2 q = perturb_segment((c_textureCoordinate + p_offset) * 2.0 - 1.0, p_xSegments, p_ySegments);
        vec2 suv = (q + 1.0) * 0.5;
        vec2 distCoord = c_textureCoordinate + (p_warp(suv) - suv);
        if (p_outputUV) {
            x_fill = vec4(distCoord, 0.0, 1.0);
        } else if (p_clampEdges && (distCoord.x < 0.0 || distCoord.y < 0.0 || distCoord.x >= 1.0 || distCoord.y >= 1.0)) {
            x_fill = vec4(0.0);
        } else if (p_xSegments == 0 && p_ySegments == 0) {
            x_fill = texture(p_input0, distCoord);
        } else {
            x_fill = textureLod(p_input0, distCoord, 0.0);
        }
    """.trimIndent()
}) {
    /**
     * the warp, a function `vec2 warp(vec2 uv)` that maps a texture coordinate to the coordinate to read from.
     * The default is a [NoiseWarp].
     */
    @NestedParameter("warp", order = 5)
    var warp: WarpPreset by presetSlot("warp", WarpPreset.CONTRACT, NoiseWarp())

    @IntParameter("x segments", 0, 256, order = 0)
    var xSegments: Int by Parameter("xSegments", 0)

    @IntParameter("y segments", 0, 256, order = 1)
    var ySegments: Int by Parameter("ySegments", 0)

    @BooleanParameter("output UV", order = 2)
    var outputUV: Boolean by Parameter("outputUV", false)

    @BooleanParameter("clamp edges", order = 3)
    var clampEdges: Boolean by Parameter("clampEdges", true)

    @Vector2Parameter("offset", -1.0, 1.0, order = 4)
    var offset: Vector2 by Parameter("offset", Vector2.ZERO)

    var bicubicFiltering = true
    override fun apply(source: Array<ColorBuffer>, target: Array<ColorBuffer>, clip: Rectangle?) {
        if (bicubicFiltering && source.isNotEmpty()) {
            source[0].generateMipmaps()
            source[0].filter(MinifyingFilter.LINEAR_MIPMAP_LINEAR, MagnifyingFilter.LINEAR)
        }
        super.apply(source, target, clip)
    }
}
