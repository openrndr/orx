package org.openrndr.extra.noise.filters

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Filter
import org.openrndr.draw.filterShaderFromUrl
import org.openrndr.extra.parameters.*
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.math.Vector4
import org.openrndr.resourceUrl

/**
 * Hash noise filter that produces white-noise-like noise.
 */
class HashNoise : Filter(filterShaderFromUrl(resourceUrl("/org/openrndr/extra/noise/shaders/gl3/hash-noise.frag"))) {
    /**
     * noise gain per channel, default is Vector4(1.0, 1.0, 1.0, 0.0)
     */
    var gain: Vector4 by parameters

    /**
     * noise bias per channel, default is Vector4(0.0, 0.0, 0.0, 1.0)
     */
    var bias: Vector4 by parameters

    /**
     * is the noise monochrome, default is true
     */
    @BooleanParameter("Monochrome")
    var monochrome: Boolean by parameters

    /**
     * noise seed, feed it with time to animate
     */
    @DoubleParameter("Seed", 0.0, 10000.0)
    var seed: Double by parameters

    init {
        monochrome = true
        gain = Vector4(1.0, 1.0, 1.0, 0.0)
        bias = Vector4(0.0, 0.0, 0.0, 1.0)
        seed = 0.0
    }
}

/**
 * Speckle noise filter
 */
class SpeckleNoise : Filter(filterShaderFromUrl(resourceUrl("/org/openrndr/extra/noise/shaders/gl3/speckle-noise.frag"))) {

    /**
     * The color of the generated speckles
     */
    @ColorParameter("Color")
    var color: ColorRGBa by parameters

    /**
     * Density of the speckles, default is 0.1, min, 0.0, max is 1.0
     */
    @DoubleParameter("Density", 0.0, 1.0)
    var density: Double by parameters


    /**
     * Noisiness of the generated speckles, default is 0.0, min is 0.0, max is 1.0
     */
    @DoubleParameter("Noise", 0.0, 1.0)
    var noise: Double by parameters

    /**
     * should the output colors be multiplied by the alpha channel, default is true
     */
    var premultipliedAlpha: Boolean by parameters

    /**
     * noise seed, feed it with time to animate
     */
    @DoubleParameter("Seed", 0.0, 10000.0)
    var seed: Double by parameters

    init {
        density = 0.1
        color = ColorRGBa.WHITE
        seed = 0.0
        noise = 0.0
        premultipliedAlpha = true
    }
}

/**
 * Filter that produces cell or Voronoi noise
 */
class CellNoise : Filter(filterShaderFromUrl(resourceUrl("/org/openrndr/extra/noise/shaders/gl3/cell-noise.frag"))) {
    var seed: Vector2 by parameters

    /**
     * base noise scale, default is Vector2(1.0, 1.0)
     */
    var scale: Vector2 by parameters

    /**
     * lacunarity is the amount by which scale is modulated per octave, default is Vector2(2.0, 2.0)
     */
    var lacunarity: Vector2 by parameters

    /**
     * gain is the base intensity per channel, default is Vector2(1.0, 1.0, 1.0, 1.0)
     */
    var gain: Vector4 by parameters

    /**
     * decay is the amount by which gain is modulated per octave, default is Vector4(0.5, 0.5, 0.5, 0.5)
     */
    var decay: Vector4 by parameters

    /**
     * the number of octaves of noise to generate, default is 4
     */
    @IntParameter("Octaves", 1, 8)
    var octaves: Int by parameters

    /**
     * the value to add to the resulting noise
     */
    var bias: Vector4 by parameters

    /**
     * should the output colors be multiplied by the alpha channel, default is true
     */
    var premultipliedAlpha: Boolean by parameters

    init {
        seed = Vector2.ZERO
        scale = Vector2.ONE
        lacunarity = Vector2(2.0, 2.0)
        gain = Vector4.ONE
        decay = Vector4.ONE / 2.0
        octaves = 4
        bias = Vector4.ZERO
        premultipliedAlpha = true
    }
}

/**
 * Filter that produces value noise
 */
class ValueNoise : Filter(filterShaderFromUrl(resourceUrl("/org/openrndr/extra/noise/shaders/gl3/value-noise.frag"))) {
    @DoubleParameter("Seed", 0.0, 10000.0)
    var seed: Vector2 by parameters

    /**
     * base noise scale, default is Vector2(1.0, 1.0)
     */
    var scale: Vector2 by parameters

    /**
     * lacunarity is the amount by which scale is modulated per octave, default is Vector2(2.0, 2.0)
     */
    var lacunarity: Vector2 by parameters

    /**
     * gain is the base intensity per channel, default is Vector2(1.0, 1.0, 1.0, 1.0)
     */
    var gain: Vector4 by parameters

    /**
     * decay is the amount by which gain is modulated per octave, default is Vector4(0.5, 0.5, 0.5, 0.5)
     */
    var decay: Vector4 by parameters

    /**
     * the number of octaves of noise to generate, default is 4
     */
    @IntParameter("Octaves", 1, 8)
    var octaves: Int by parameters

    /**
     * the value to add to the resulting noise
     */
    var bias: Vector4 by parameters

    /**
     * should the output colors be multiplied by the alpha channel, default is true
     */
    var premultipliedAlpha: Boolean by parameters

    init {
        seed = Vector2.ZERO
        scale = Vector2.ONE
        lacunarity = Vector2(2.0, 2.0)
        gain = Vector4.ONE
        decay = Vector4.ONE / 2.0
        octaves = 4
        bias = Vector4.ZERO
        premultipliedAlpha = true
    }
}

/**
 * Filter that produces 3D Simplex Noise
 */
@Description("Simplex Noise")
class SimplexNoise3D : Filter(filterShaderFromUrl(resourceUrl("/org/openrndr/extra/noise/shaders/gl3/simplex-noise-3d.frag"))) {
    var seed: Vector3 by parameters

    /**
     * base noise scale, default is Vector3(1.0, 1.0, 1.0)
     */
    var scale: Vector3 by parameters

    /**
     * lacunarity is the amount by which scale is modulated per octave, default is Vector3(2.0, 2.0, 2.0)
     */
    var lacunarity: Vector3 by parameters

    /**
     * gain is the base intensity per channel, default is Vector2(1.0, 1.0, 1.0, 1.0)
     */
    var gain: Vector4 by parameters

    /**
     * decay is the amount by which gain is modulated per octave, default is Vector4(0.5, 0.5, 0.5, 0.5)
     */
    var decay: Vector4 by parameters

    /**
     * the number of octaves of noise to generate, default is 4
     */
    @IntParameter("Octaves", 1, 8)
    var octaves: Int by parameters

    /**
     * the value to add to the resulting noise
     */
    var bias: Vector4 by parameters

    /**
     * should the output colors be multiplied by the alpha channel, default is true
     */
    @BooleanParameter("Premultiplied alpha")
    var premultipliedAlpha: Boolean by parameters

    init {
        seed = Vector3.ZERO
        scale = Vector3.ONE
        lacunarity = Vector3(2.0, 2.0, 2.0)
        gain = Vector4.ONE / 2.0
        decay = Vector4.ONE / 2.0
        octaves = 4
        bias = Vector4.ONE / 2.0
        premultipliedAlpha = true
    }
}


/**
 * Filter for Worley Noise
 */
@Description("Worley Noise")
class WorleyNoise : Filter(filterShaderFromUrl(resourceUrl("/org/openrndr/extra/noise/shaders/gl3/worley-noise.frag"))) {
    @DoubleParameter("Scale", 0.1, 200.0)
    var scale: Double by parameters

    @BooleanParameter("Premultiplied alpha")
    var premultipliedAlpha: Boolean by parameters

    init {
        premultipliedAlpha = true
        scale = 5.0
    }
}
