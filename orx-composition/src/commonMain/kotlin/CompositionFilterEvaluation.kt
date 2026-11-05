package org.openrndr.extra.composition

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.ColorBuffer
import org.openrndr.draw.ColorFormat
import org.openrndr.draw.ColorType
import org.openrndr.draw.MagnifyingFilter
import org.openrndr.draw.MinifyingFilter
import org.openrndr.draw.ShadeStyleFilter
import org.openrndr.draw.colorBuffer
import org.openrndr.draw.shadeStyle
import org.openrndr.extra.shadestyles.presets.PaintUnits
import org.openrndr.math.Matrix44
import org.openrndr.math.Vector2
import org.openrndr.math.Vector4
import org.openrndr.shape.Rectangle
import org.openrndr.utils.buffer.MPPBuffer
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

/*
 * The evaluation of filter effects on the layer of a node. Every primitive is one or more passes that read and write
 * textures of the size of the layer. The textures hold premultiplied colors in linear light; primitives that compute
 * in sRGB convert them when they read and write.
 */

/** the textures that the passes of filters write, by the depth of the layer and their index */
private val filterBuffers = mutableMapOf<Pair<Int, Int>, ColorBuffer>()

private fun filterBuffer(depth: Int, index: Int, like: ColorBuffer): ColorBuffer {
    val key = Pair(depth, index)
    filterBuffers[key]?.let {
        if (it.width == like.width && it.height == like.height && it.contentScale == like.contentScale) return it
        it.destroy()
    }
    return colorBuffer(like.width, like.height, like.contentScale, ColorFormat.RGBa, ColorType.FLOAT16).also {
        it.filter(MinifyingFilter.NEAREST, MagnifyingFilter.NEAREST)
        filterBuffers[key] = it
    }
}

/** the functions that every pass uses: reading an input in the color space of the primitive and writing a result */
private val passPreamble = """
    vec3 f_toSrgb(vec3 c) {
        c = clamp(c, 0.0, 1.0);
        return mix(1.055 * pow(c, vec3(1.0 / 2.4)) - 0.055, c * 12.92, step(c, vec3(0.0031308)));
    }
    vec3 f_toLinear(vec3 c) {
        return mix(pow((c + 0.055) / 1.055, vec3(2.4)), c / 12.92, step(c, vec3(0.04045)));
    }
    // a premultiplied color of a texture in the color space of the primitive, transparent outside the texture
    vec4 f_get(sampler2D t, vec2 uv) {
        if (uv.x < 0.0 || uv.y < 0.0 || uv.x > 1.0 || uv.y > 1.0) return vec4(0.0);
        vec4 c = texture(t, uv);
        if (p_srgb == 1 && c.a > 0.0) c.rgb = f_toSrgb(c.rgb / c.a) * c.a;
        return c;
    }
    // the value that a pass writes for a premultiplied result, the filter premultiplies it
    vec4 f_out(vec4 r) {
        r = clamp(r, 0.0, 1.0);
        vec3 c = r.a > 0.0 ? min(r.rgb / r.a, vec3(1.0)) : vec3(0.0);
        if (p_srgb == 1) c = f_toLinear(c);
        return vec4(c, r.a);
    }
    vec3 f_straight(vec4 c) { return c.a > 0.0 ? c.rgb / c.a : vec3(0.0); }
""".trimIndent()

private fun pass(code: String, preamble: String = "") = ShadeStyleFilter(shadeStyle {
    fragmentPreamble = passPreamble + "\n" + preamble
    fragmentTransform = "vec2 uv = c_textureCoordinate;\n$code"
})

private val copyPass by lazy {
    pass("""
        vec4 c = f_get(p_input0, uv);
        if (p_alphaOnly == 1) c.rgb = vec3(0.0);
        x_fill = f_out(c);
    """.trimIndent())
}

private val blurPass by lazy {
    pass("""
        vec4 acc = vec4(0.0);
        float total = 0.0;
        for (int i = -p_radius; i <= p_radius; i++) {
            float w = exp(-0.5 * float(i * i) / (p_sigma * p_sigma));
            acc += w * f_get(p_input0, uv + float(i) * p_step);
            total += w;
        }
        x_fill = f_out(acc / total);
    """.trimIndent())
}

private val offsetPass by lazy { pass("x_fill = f_out(f_get(p_input0, uv - p_shift));") }

private val floodPass by lazy { pass("x_fill = f_out(vec4(p_color.rgb * p_color.a, p_color.a));") }

private val compositePass by lazy {
    pass("""
        vec4 a = f_get(p_input0, uv);
        vec4 b = f_get(p_input1, uv);
        vec4 r;
        if (p_operator == 0) r = a + b * (1.0 - a.a);
        else if (p_operator == 1) r = a * b.a;
        else if (p_operator == 2) r = a * (1.0 - b.a);
        else if (p_operator == 3) r = a * b.a + b * (1.0 - a.a);
        else if (p_operator == 4) r = a * (1.0 - b.a) + b * (1.0 - a.a);
        else if (p_operator == 5) r = a + b;
        else r = p_k.x * a * b + p_k.y * a + p_k.z * b + p_k.w;
        r = clamp(r, 0.0, 1.0);
        x_fill = f_out(vec4(min(r.rgb, vec3(r.a)), r.a));
    """.trimIndent())
}

private val blendPass by lazy {
    pass("""
        vec4 s = f_get(p_input0, uv);
        vec4 b = f_get(p_input1, uv);
        vec3 cs = f_straight(s);
        vec3 cb = f_straight(b);
        vec3 mixed = (1.0 - b.a) * cs + b.a * layer_blend(p_mode, cb, cs);
        x_fill = f_out(vec4(s.a * mixed + (1.0 - s.a) * b.rgb, s.a + b.a * (1.0 - s.a)));
    """.trimIndent(), blendFunctions)
}

private val matrixPass by lazy {
    pass("""
        vec4 s = f_get(p_input0, uv);
        vec4 r = clamp(p_matrix * vec4(f_straight(s), s.a) + p_offset, 0.0, 1.0);
        x_fill = f_out(vec4(r.rgb * r.a, r.a));
    """.trimIndent())
}

private val transferPass by lazy {
    pass("""
        vec4 s = f_get(p_input0, uv);
        vec4 c = vec4(f_straight(s), s.a);
        // the table holds 256 values of every function, it is read between them
        vec4 r;
        for (int k = 0; k < 4; k++) {
            float x = clamp(c[k], 0.0, 1.0) * 255.0;
            float i = min(floor(x), 254.0);
            float lo = texelFetch(p_table, ivec2(int(i), k), 0).r;
            float hi = texelFetch(p_table, ivec2(int(i) + 1, k), 0).r;
            r[k] = mix(lo, hi, x - i);
        }
        r = clamp(r, 0.0, 1.0);
        x_fill = f_out(vec4(r.rgb * r.a, r.a));
    """.trimIndent())
}

private val morphologyPass by lazy {
    pass("""
        vec4 m = f_get(p_input0, uv);
        for (int i = -p_radius; i <= p_radius; i++) {
            vec4 c = f_get(p_input0, uv + float(i) * p_step);
            m = p_dilate == 1 ? max(m, c) : min(m, c);
        }
        x_fill = f_out(m);
    """.trimIndent())
}

private val shadowPass by lazy {
    pass("""
        vec4 shadow = vec4(p_color.rgb * p_color.a, p_color.a) * f_get(p_input1, uv).a;
        vec4 source = f_get(p_input0, uv);
        x_fill = f_out(source + shadow * (1.0 - source.a));
    """.trimIndent())
}

private val turbulencePass by lazy {
    pass("""
        vec2 point = (p_toUser * vec4(uv * 2.0 - 1.0, 0.0, 1.0)).xy;
        vec4 r;
        for (int k = 0; k < 4; k++) {
            float sum = 0.0;
            vec2 v = point * p_frequency;
            float ratio = 1.0;
            for (int o = 0; o < p_octaves; o++) {
                float n = t_noise2(k, v);
                sum += p_fractal == 1 ? n / ratio : abs(n) / ratio;
                v *= 2.0;
                ratio *= 2.0;
            }
            r[k] = p_fractal == 1 ? (sum + 1.0) / 2.0 : sum;
        }
        r = clamp(r, 0.0, 1.0);
        x_fill = f_out(vec4(r.rgb * r.a, r.a));
    """.trimIndent(), """
        int t_lattice(int i) { return int(texelFetch(p_lattice, ivec2(i, 0), 0).r + 0.5); }
        vec2 t_gradient(int k, int i) { return texelFetch(p_lattice, ivec2(i, k + 1), 0).rg; }
        float t_curve(float t) { return t * t * (3.0 - 2.0 * t); }
        float t_noise2(int k, vec2 v) {
            float t = v.x + 4096.0;
            int bx0 = int(t) & 255;
            int bx1 = (bx0 + 1) & 255;
            float rx0 = t - floor(t);
            float rx1 = rx0 - 1.0;
            t = v.y + 4096.0;
            int by0 = int(t) & 255;
            int by1 = (by0 + 1) & 255;
            float ry0 = t - floor(t);
            float ry1 = ry0 - 1.0;
            int i = t_lattice(bx0);
            int j = t_lattice(bx1);
            int b00 = t_lattice(i + by0);
            int b10 = t_lattice(j + by0);
            int b01 = t_lattice(i + by1);
            int b11 = t_lattice(j + by1);
            float sx = t_curve(rx0);
            float sy = t_curve(ry0);
            vec2 q = t_gradient(k, b00);
            float u = rx0 * q.x + ry0 * q.y;
            q = t_gradient(k, b10);
            float w = rx1 * q.x + ry0 * q.y;
            float a = mix(u, w, sx);
            q = t_gradient(k, b01);
            u = rx0 * q.x + ry1 * q.y;
            q = t_gradient(k, b11);
            w = rx1 * q.x + ry1 * q.y;
            float b = mix(u, w, sx);
            return mix(a, b, sy);
        }
    """.trimIndent())
}

private val displacementPass by lazy {
    pass("""
        vec4 d = f_get(p_input1, uv);
        vec4 c = vec4(f_straight(d), d.a);
        vec2 shift = p_scale * (vec2(c[p_xChannel], c[p_yChannel]) - 0.5);
        x_fill = f_out(f_get(p_input0, uv + (p_toUv * vec3(shift, 0.0)).xy));
    """.trimIndent())
}

private val regionPass by lazy {
    pass("""
        vec4 c = texture(p_input0, uv);
        vec2 point = (p_toUser * vec4(uv * 2.0 - 1.0, 0.0, 1.0)).xy;
        // the coverage of the pixel by the region, its edges are anti-aliased
        vec2 size = max(fwidth(point), vec2(1e-6));
        vec2 inside = min(point - p_region.xy, p_region.xy + p_region.zw - point) / size;
        float coverage = clamp(inside.x + 0.5, 0.0, 1.0) * clamp(inside.y + 0.5, 0.0, 1.0);
        x_fill = vec4(f_straight(c), c.a * coverage);
    """.trimIndent())
}

/** the lattice and gradients of the Perlin noise of feTurbulence for a seed, as in the specification */
private val turbulenceLattices = mutableMapOf<Int, ColorBuffer>()

private fun turbulenceLattice(seed: Double): ColorBuffer {
    // the seed is rounded, as in browsers
    val s = kotlin.math.round(seed).toInt()
    return turbulenceLattices.getOrPut(s) {
        val bSize = 256
        val randM = 2147483647L
        var lSeed = s.toLong()
        if (lSeed <= 0) lSeed = -(lSeed % (randM - 1)) + 1
        if (lSeed > randM - 1) lSeed = randM - 1
        fun random(): Long {
            var result = 16807L * (lSeed % 127773L) - 2836L * (lSeed / 127773L)
            if (result <= 0) result += randM
            lSeed = result
            return result
        }
        val lattice = IntArray(bSize + bSize + 2)
        val gradient = Array(4) { Array(bSize + bSize + 2) { DoubleArray(2) } }
        for (k in 0 until 4) {
            for (i in 0 until bSize) {
                lattice[i] = i
                for (j in 0 until 2) gradient[k][i][j] = ((random() % (bSize + bSize)) - bSize).toDouble() / bSize
                val l = sqrt(gradient[k][i][0] * gradient[k][i][0] + gradient[k][i][1] * gradient[k][i][1])
                gradient[k][i][0] /= l
                gradient[k][i][1] /= l
            }
        }
        var i = bSize - 1
        while (i > 0) {
            val k = lattice[i]
            val j = (random() % bSize).toInt()
            lattice[i] = lattice[j]
            lattice[j] = k
            i--
        }
        for (n in 0 until bSize + 2) {
            lattice[bSize + n] = lattice[n]
            for (k in 0 until 4) for (j in 0 until 2) gradient[k][bSize + n][j] = gradient[k][n][j]
        }
        val width = bSize + bSize + 2
        val buffer = MPPBuffer.allocate(width * 5 * 4 * 4)
        for (row in 0 until 5) for (x in 0 until width) {
            if (row == 0) {
                buffer.putFloat(lattice[x].toFloat()); buffer.putFloat(0f)
            } else {
                buffer.putFloat(gradient[row - 1][x][0].toFloat()); buffer.putFloat(gradient[row - 1][x][1].toFloat())
            }
            buffer.putFloat(0f); buffer.putFloat(0f)
        }
        buffer.rewind()
        colorBuffer(width, 5, 1.0, ColorFormat.RGBa, ColorType.FLOAT32).also {
            it.filter(MinifyingFilter.NEAREST, MagnifyingFilter.NEAREST)
            it.write(buffer)
        }
    }
}

/** the table of the transfer functions of a component transfer: 256 values of every function */
private var transferTable: ColorBuffer? = null

private fun transferTable(functions: List<FilterPrimitive.TransferFunction>): ColorBuffer {
    val table = transferTable ?: colorBuffer(256, 4, 1.0, ColorFormat.R, ColorType.FLOAT32).also {
        it.filter(MinifyingFilter.NEAREST, MagnifyingFilter.NEAREST)
        transferTable = it
    }
    val buffer = MPPBuffer.allocate(256 * 4 * 4)
    for (f in functions) for (i in 0 until 256) buffer.putFloat(f.apply(i / 255.0).toFloat())
    buffer.rewind()
    table.write(buffer)
    return table
}

/**
 * The transforms of a node that a filter is applied to: [toClip] maps its user space to clip space, [bounds] are the
 * bounds of what it draws in user space
 */
internal class FilterSpace(private val toClip: Matrix44, val bounds: Rectangle?, private val size: Vector2) {
    /** maps texture coordinates to user space */
    val toUser: Matrix44 = toClip.inversed

    /** the change of texture coordinates for a change of [v] in user space */
    fun uv(v: Vector2): Vector2 = (toClip * Vector4(v.x, v.y, 0.0, 0.0)).xy * 0.5

    /** the number of pixels along [v] in user space */
    fun pixels(v: Vector2): Double = (uv(v) * size).length

    /** the matrix that maps a change in user space to a change of texture coordinates */
    val toUv: org.openrndr.math.Matrix33
        get() {
            val x = uv(Vector2.UNIT_X)
            val y = uv(Vector2.UNIT_Y)
            return org.openrndr.math.Matrix33(x.x, y.x, 0.0, x.y, y.y, 0.0, 0.0, 0.0, 1.0)
        }
}

/**
 * Applies [effects] to [source], the layer of a node, and returns the result, a texture that this function owns
 */
internal fun applyFilters(effects: List<FilterEffect>, source: ColorBuffer, space: FilterSpace, depth: Int): ColorBuffer {
    var index = 0
    var input = source
    for (effect in effects) {
        input = applyEffect(effect, input, space) { filterBuffer(depth, index++, source) }
    }
    return input
}

private fun ShadeStyleFilter.run(inputs: List<ColorBuffer>, target: ColorBuffer, srgb: Boolean) {
    parameter("srgb", if (srgb) 1 else 0)
    apply(inputs.toTypedArray(), target)
}

private fun applyEffect(effect: FilterEffect, source: ColorBuffer, space: FilterSpace, next: () -> ColorBuffer): ColorBuffer {
    val bounds = space.bounds
    if ((effect.regionUnits == PaintUnits.BOUNDS && effect.region != null) || effect.primitiveUnits == PaintUnits.BOUNDS) {
        if (bounds == null || bounds.width <= 0.0 || bounds.height <= 0.0) return next().also { clear(it) }
    }
    // lengths of primitives along x and y in user space
    fun lengths(v: Vector2) = if (effect.primitiveUnits == PaintUnits.BOUNDS) Vector2(v.x * bounds!!.width, v.y * bounds.height) else v

    val results = mutableMapOf<String, ColorBuffer>()
    var previous = source
    val sourceAlpha by lazy {
        next().also { copyPass.parameter("alphaOnly", 1); copyPass.run(listOf(source), it, false) }
    }
    val transparent by lazy { next().also { clear(it) } }

    for (primitive in effect.primitives) {
        val srgb = primitive.colorSpace == FilterColorSpace.SRGB
        fun input(i: FilterInput): ColorBuffer = when (i) {
            FilterInput.Previous -> previous
            FilterInput.SourceGraphic -> source
            FilterInput.SourceAlpha -> sourceAlpha
            is FilterInput.Result -> results[i.name] ?: previous
            is FilterInput.Unsupported -> transparent
        }

        fun blur(input: ColorBuffer, stdDeviation: Vector2): ColorBuffer {
            var current = input
            for ((axis, sigma) in listOf(Vector2.UNIT_X to stdDeviation.x, Vector2.UNIT_Y to stdDeviation.y)) {
                val pixels = space.pixels(axis)
                val sigmaPixels = sigma * pixels
                if (sigmaPixels <= 0.0 || pixels <= 0.0) continue
                val out = next()
                blurPass.parameter("step", space.uv(axis) / pixels)
                blurPass.parameter("sigma", sigmaPixels)
                blurPass.parameter("radius", min(ceil(sigmaPixels * 3.0).toInt(), 512))
                blurPass.run(listOf(current), out, srgb)
                current = out
            }
            return current
        }

        val output: ColorBuffer = when (primitive) {
            is FilterPrimitive.GaussianBlur -> {
                val s = lengths(primitive.stdDeviation)
                if (s.x < 0.0 || s.y < 0.0) transparent else blur(input(primitive.input), s)
            }

            is FilterPrimitive.Offset -> next().also {
                val d = lengths(Vector2(primitive.dx, primitive.dy))
                offsetPass.parameter("shift", space.uv(d))
                offsetPass.run(listOf(input(primitive.input)), it, srgb)
            }

            is FilterPrimitive.Flood -> next().also {
                floodPass.parameter("color", workingColor(primitive.color, srgb))
                floodPass.run(listOf(noInput), it, srgb)
            }

            is FilterPrimitive.Merge -> {
                var merged = transparent
                for (i in primitive.inputs) {
                    val out = next()
                    compositePass.parameter("operator", 0)
                    compositePass.parameter("k", Vector4.ZERO)
                    compositePass.run(listOf(input(i), merged), out, srgb)
                    merged = out
                }
                merged
            }

            is FilterPrimitive.Composite -> next().also {
                compositePass.parameter("operator", primitive.operator.ordinal)
                compositePass.parameter("k", Vector4(primitive.k1, primitive.k2, primitive.k3, primitive.k4))
                compositePass.run(listOf(input(primitive.input), input(primitive.input2)), it, srgb)
            }

            is FilterPrimitive.Blend -> next().also {
                blendPass.parameter("mode", primitive.mode.ordinal)
                blendPass.run(listOf(input(primitive.input), input(primitive.input2)), it, srgb)
            }

            is FilterPrimitive.ColorMatrix -> next().also {
                val v = primitive.values.takeIf { it.size == 20 } ?: FilterPrimitive.ColorMatrix.saturate(1.0)
                matrixPass.parameter("matrix", Matrix44(
                    v[0], v[1], v[2], v[3],
                    v[5], v[6], v[7], v[8],
                    v[10], v[11], v[12], v[13],
                    v[15], v[16], v[17], v[18]
                ))
                matrixPass.parameter("offset", Vector4(v[4], v[9], v[14], v[19]))
                matrixPass.run(listOf(input(primitive.input)), it, srgb)
            }

            is FilterPrimitive.ComponentTransfer -> next().also {
                transferPass.parameter("table", transferTable(listOf(primitive.red, primitive.green, primitive.blue, primitive.alpha)))
                transferPass.run(listOf(input(primitive.input)), it, srgb)
            }

            is FilterPrimitive.Morphology -> {
                val r = lengths(primitive.radius)
                if (r.x < 0.0 || r.y < 0.0 || (r.x == 0.0 && r.y == 0.0)) {
                    if (r.x == 0.0 && r.y == 0.0) input(primitive.input) else transparent
                } else {
                    var current = input(primitive.input)
                    for ((axis, radius) in listOf(Vector2.UNIT_X to r.x, Vector2.UNIT_Y to r.y)) {
                        val pixels = space.pixels(axis)
                        val radiusPixels = (radius * pixels).roundToInt()
                        if (radiusPixels <= 0) continue
                        val out = next()
                        morphologyPass.parameter("step", space.uv(axis) / pixels)
                        morphologyPass.parameter("radius", min(radiusPixels, 512))
                        morphologyPass.parameter("dilate", if (primitive.dilate) 1 else 0)
                        morphologyPass.run(listOf(current), out, srgb)
                        current = out
                    }
                    current
                }
            }

            is FilterPrimitive.DropShadow -> {
                val inputBuffer = input(primitive.input)
                val blurred = blur(inputBuffer, lengths(primitive.stdDeviation))
                val moved = next().also {
                    offsetPass.parameter("shift", space.uv(lengths(Vector2(primitive.dx, primitive.dy))))
                    offsetPass.run(listOf(blurred), it, srgb)
                }
                next().also {
                    shadowPass.parameter("color", workingColor(primitive.color, srgb))
                    shadowPass.run(listOf(inputBuffer, moved), it, srgb)
                }
            }

            is FilterPrimitive.Turbulence -> next().also {
                turbulencePass.parameter("lattice", turbulenceLattice(primitive.seed))
                turbulencePass.parameter("toUser", space.toUser)
                turbulencePass.parameter("frequency", primitive.baseFrequency)
                turbulencePass.parameter("octaves", max(primitive.octaves, 0))
                turbulencePass.parameter("fractal", if (primitive.fractalNoise) 1 else 0)
                turbulencePass.run(listOf(noInput), it, srgb)
            }

            is FilterPrimitive.DisplacementMap -> next().also {
                val scale = lengths(Vector2(primitive.scale, primitive.scale))
                displacementPass.parameter("scale", scale)
                displacementPass.parameter("toUv", space.toUv)
                displacementPass.parameter("xChannel", primitive.xChannel.ordinal)
                displacementPass.parameter("yChannel", primitive.yChannel.ordinal)
                displacementPass.run(listOf(input(primitive.input), input(primitive.input2)), it, srgb)
            }
        }
        previous = output
        primitive.result?.let { results[it] = output }
    }

    // the result is clipped to the region of the filter
    val region = effect.region ?: return previous
    val userRegion = if (effect.regionUnits == PaintUnits.BOUNDS) Rectangle(
        bounds!!.x + region.x * bounds.width, bounds.y + region.y * bounds.height,
        region.width * bounds.width, region.height * bounds.height
    ) else region
    return next().also {
        regionPass.parameter("toUser", space.toUser)
        regionPass.parameter("region", Vector4(userRegion.x, userRegion.y, userRegion.width, userRegion.height))
        regionPass.run(listOf(previous), it, false)
    }
}

/** [color] as a straight color in the color space of a primitive, colors are given in sRGB */
private fun workingColor(color: ColorRGBa, srgb: Boolean): Vector4 {
    val c = if (srgb) color.toSRGB() else color.toLinear()
    return Vector4(c.r, c.g, c.b, c.alpha)
}

/** a texture that passes without inputs are given, they do not read it */
private val noInput by lazy { colorBuffer(1, 1) }

private fun clear(buffer: ColorBuffer) {
    floodPass.parameter("color", Vector4.ZERO)
    floodPass.run(listOf(noInput), buffer, false)
}
