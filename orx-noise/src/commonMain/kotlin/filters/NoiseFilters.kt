package org.openrndr.extra.noise.filters

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.extra.parameters.*
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.math.Vector4

/**
 * Hash noise filter that produces white-noise-like noise.
 */
@Description("Hash Noise")
class HashNoise : Filter(filterShaderFromCode(
"""
// uniforms
uniform vec4 gain;
uniform vec4 bias;
uniform bool monochrome;
uniform float seed;

// varyings
in vec2 v_texCoord0;

// outputs
out vec4 o_output;

vec2 hash22(vec2 p) {
    float n = sin(dot(p, vec2(41, 289)));
    return fract(vec2(262144, 32768)*n);
}

void main() {

    vec2 vseed = vec2(seed);
    if (!monochrome) {
        o_output.rg = hash22(vseed + v_texCoord0) * gain.rg + bias.rg;
        o_output.ba = hash22(vseed + v_texCoord0+vec2(1.0, 1.0)) * gain.ba + bias.ba;
    } else {
        float c = hash22(vseed + v_texCoord0).r;
        o_output = c * gain + bias;
    }

}    
""", "hash-noise"
)) {
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
class SpeckleNoise : Filter(filterShaderFromCode(
"""
// uniforms
uniform float seed;
uniform float density;
uniform vec4 color;
uniform bool premultipliedAlpha;
uniform float noise;

// varyings
in vec2 v_texCoord0;

// outputs
out vec4 o_output;

vec2 hash22(vec2 p) {
    float n = sin(dot(p, vec2(41, 289)));
    return fract(vec2(262144, 32768)*n);
}

void main() {
    vec2 vseed = vec2(seed);
    vec2 hash = hash22(v_texCoord0 + seed);
    float t = hash.x;

    vec4 result = vec4(0.0);
    if (t < density) {
        vec4 noisyColor = vec4(color.rgb * mix(1.0, hash.y, noise), color.a);
        result = noisyColor;
    }
    o_output = result;
    if (premultipliedAlpha) {
        o_output.rgb *= o_output.a;
    }
}        
""", "speckel-noise"
)) {

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
@Description("Cell Noise")
class CellNoise : Filter(filterShaderFromCode(
"""
// uniforms
uniform vec4 gain;
uniform vec4 bias;
uniform vec2 seed;

uniform vec2 scale;
uniform vec2 lacunarity;
uniform vec4 decay;
uniform int octaves;
uniform bool premultipliedAlpha;

// varyings
in vec2 v_texCoord0;

// outputs
out vec4 o_output;

vec2 hash22(vec2 p) {
    float n = sin(dot(p, vec2(41, 289)));
    return fract(vec2(262144, 32768)*n);
}

float cell(vec2 p) {
    vec2 ip = floor(p);
    p = fract(p);

    float d = 1.0;
    for (int i = -1; i <= 1; i++) {
        for (int j = -1; j <= 1; j++) {
            vec2 cellRef = vec2(i, j);
            vec2 offset = hash22(ip + cellRef);
            vec2 r = cellRef + offset - p;
            float d2 = dot(r, r);
            d = min(d, d2);
        }
    }
    return d;
}

void main() {
    vec4 result = vec4(0.0);
    vec4 _gain = gain;
    vec2 _scale = scale;
    for (int o = 0; o < octaves; ++o) {
        result += cell((v_texCoord0+seed) * _scale) * _gain;
        _scale *= lacunarity;
        _gain *= decay;
    }
    o_output = result + bias;

    if (premultipliedAlpha) {
        o_output.rgb *= o_output.a;
    }
}        
""", "cell-noise"
)) {
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
@Description("Value Noise")
class ValueNoise : Filter(filterShaderFromCode(
"""
// based on https://www.shadertoy.com/view/4dS3Wd

// uniforms
uniform vec4 gain;
uniform vec4 bias;
uniform vec2 seed;

uniform vec2 scale;
uniform vec2 lacunarity;
uniform vec4 decay;
uniform int octaves;
uniform bool premultipliedAlpha;

// varyings
in vec2 v_texCoord0;

// outputs
out vec4 o_output;

float hash(vec2 p) { return fract(1e4 * sin(17.0 * p.x + p.y * 0.1) * (0.1 + abs(sin(p.y * 13.0 + p.x)))); }

float noise(vec2 x) {
    vec2 i = floor(x);
    vec2 f = fract(x);

    float a = hash(i);
    float b = hash(i + vec2(1.0, 0.0));
    float c = hash(i + vec2(0.0, 1.0));
    float d = hash(i + vec2(1.0, 1.0));

    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(a, b, u.x) + (c - a) * u.y * (1.0 - u.x) + (d - b) * u.x * u.y;
}

void main() {
    vec4 result = vec4(0.0);
    vec4 _gain = gain;

    vec2 shift = vec2(100);
    mat2 rot = mat2(cos(0.5), sin(0.5), -sin(0.5), cos(0.50));
    vec2 x = ((v_texCoord0+seed) * scale);
    for (int o = 0; o < octaves; ++o) {
        result += noise(x) * _gain;
        x = rot * x * lacunarity + shift;
        _gain *= decay;
    }
    o_output = result + bias;

    if (premultipliedAlpha) {
        o_output.rgb *= o_output.a;
    }
}    
""", "value-noise"
)) {
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
class SimplexNoise3D : Filter(filterShaderFromCode(
"""
// uniforms
uniform vec4 gain;
uniform vec4 bias;
uniform vec3 seed;
uniform vec3 scale;

uniform vec3 lacunarity;
uniform vec4 decay;
uniform int octaves;
uniform bool premultipliedAlpha;

// varyings
in vec2 v_texCoord0;

// outputs
out vec4 o_output;

// Simplex Noise 3D Implementation
// Description : Array and textureless GLSL 2D/3D/4D simplex
//               noise functions.
//      Author : Ian McEwan, Ashima Arts.
//  Maintainer : ijm
//     Lastmod : 20110822 (ijm)
//     License : Copyright (C) 2011 Ashima Arts. All rights reserved.
//               Distributed under the MIT License. See LICENSE file.
//               https://github.com/ashima/webgl-noise
//               https://github.com/stegu/webgl-noise
//
//
vec3 mod289(vec3 x) {
    return x - floor(x * (1.0 / 289.0)) * 289.0;
}

vec4 mod289(vec4 x) {
    return x - floor(x * (1.0 / 289.0)) * 289.0;
}

vec4 permute(vec4 x) {
    return mod289(((x*34.0)+1.0)*x);
}

vec4 taylorInvSqrt(vec4 r) {
    return 1.79284291400159 - 0.85373472095314 * r;
}

float snoise(vec3 v) {
    const vec2  C = vec2(1.0/6.0, 1.0/3.0) ;
    const vec4  D = vec4(0.0, 0.5, 1.0, 2.0);

    // First corner
    vec3 i  = floor(v + dot(v, C.yyy) );
    vec3 x0 =   v - i + dot(i, C.xxx) ;

    // Other corners
    vec3 g = step(x0.yzx, x0.xyz);
    vec3 l = 1.0 - g;
    vec3 i1 = min( g.xyz, l.zxy );
    vec3 i2 = max( g.xyz, l.zxy );

    //   x0 = x0 - 0.0 + 0.0 * C.xxx;
    //   x1 = x0 - i1  + 1.0 * C.xxx;
    //   x2 = x0 - i2  + 2.0 * C.xxx;
    //   x3 = x0 - 1.0 + 3.0 * C.xxx;
    vec3 x1 = x0 - i1 + C.xxx;
    vec3 x2 = x0 - i2 + C.yyy; // 2.0*C.x = 1/3 = C.y
    vec3 x3 = x0 - D.yyy;      // -1.0+3.0*C.x = -0.5 = -D.y

    // Permutations
    i = mod289(i);
    vec4 p = permute( permute( permute(
    i.z + vec4(0.0, i1.z, i2.z, 1.0 ))
    + i.y + vec4(0.0, i1.y, i2.y, 1.0 ))
    + i.x + vec4(0.0, i1.x, i2.x, 1.0 ));

    // Gradients: 7x7 points over a square, mapped onto an octahedron.
    // The ring size 17*17 = 289 is close to a multiple of 49 (49*6 = 294)
    float n_ = 0.142857142857; // 1.0/7.0
    vec3  ns = n_ * D.wyz - D.xzx;

    vec4 j = p - 49.0 * floor(p * ns.z * ns.z);  //  mod(p,7*7)

    vec4 x_ = floor(j * ns.z);
    vec4 y_ = floor(j - 7.0 * x_ );    // mod(j,N)

    vec4 x = x_ *ns.x + ns.yyyy;
    vec4 y = y_ *ns.x + ns.yyyy;
    vec4 h = 1.0 - abs(x) - abs(y);

    vec4 b0 = vec4( x.xy, y.xy );
    vec4 b1 = vec4( x.zw, y.zw );

    //vec4 s0 = vec4(lessThan(b0,0.0))*2.0 - 1.0;
    //vec4 s1 = vec4(lessThan(b1,0.0))*2.0 - 1.0;
    vec4 s0 = floor(b0)*2.0 + 1.0;
    vec4 s1 = floor(b1)*2.0 + 1.0;
    vec4 sh = -step(h, vec4(0.0));

    vec4 a0 = b0.xzyw + s0.xzyw*sh.xxyy ;
    vec4 a1 = b1.xzyw + s1.xzyw*sh.zzww ;

    vec3 p0 = vec3(a0.xy,h.x);
    vec3 p1 = vec3(a0.zw,h.y);
    vec3 p2 = vec3(a1.xy,h.z);
    vec3 p3 = vec3(a1.zw,h.w);

    //Normalise gradients
    vec4 norm = taylorInvSqrt(vec4(dot(p0,p0), dot(p1,p1), dot(p2, p2), dot(p3,p3)));
    p0 *= norm.x;
    p1 *= norm.y;
    p2 *= norm.z;
    p3 *= norm.w;

    // Mix final noise value
    vec4 m = max(0.6 - vec4(dot(x0,x0), dot(x1,x1), dot(x2,x2), dot(x3,x3)), 0.0);
    m = m * m;
    return 42.0 * dot( m*m, vec4( dot(p0,x0), dot(p1,x1),
    dot(p2,x2), dot(p3,x3) ) );
}

void main() {
    vec4 result = vec4(0.0);
    vec4 _gain = gain;

    vec3 shift = vec3(100);
    vec3 uv = vec3(v_texCoord0, 1.0) * 2.0 - 1.0;
    vec3 x = ((uv + seed) * scale);

    for (int o = 0; o < octaves; ++o) {
        result += snoise(x) * _gain;
        x = x * lacunarity + shift;
        _gain *= decay;
    }

    o_output = result + bias;

    if (premultipliedAlpha) {
        o_output.rgb *= o_output.a;
    }
}
""", "simplex-noise-3d"
)) {
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
class WorleyNoise : Filter(filterShaderFromCode(
"""
// uniforms
uniform float scale;
uniform vec2 offset;
uniform bool premultipliedAlpha;

// varyings
in vec2 v_texCoord0;

// outputs
out vec4 o_output;


vec3 permute(vec3 x) {
    return mod((34.0 * x + 1.0) * x, 289.0);
}

vec3 dist(vec3 x, vec3 y,  bool manhattanDistance) {
    return manhattanDistance ?  abs(x) + abs(y) :  (x * x + y * y);
}

vec2 worley(vec2 P, float jitter, bool manhattanDistance) {
    float K= 0.142857142857; // 1/7
    float Ko= 0.428571428571 ;// 3/7
    vec2 Pi = mod(floor(P), 289.0);
    vec2 Pf = fract(P);
    vec3 oi = vec3(-1.0, 0.0, 1.0);
    vec3 of = vec3(-0.5, 0.5, 1.5);
    vec3 px = permute(Pi.x + oi);
    vec3 p = permute(px.x + Pi.y + oi); // p11, p12, p13
    vec3 ox = fract(p*K) - Ko;
    vec3 oy = mod(floor(p*K),7.0)*K - Ko;
    vec3 dx = Pf.x + 0.5 + jitter*ox;
    vec3 dy = Pf.y - of + jitter*oy;
    vec3 d1 = dist(dx,dy, manhattanDistance); // d11, d12 and d13, squared
    p = permute(px.y + Pi.y + oi); // p21, p22, p23
    ox = fract(p*K) - Ko;
    oy = mod(floor(p*K),7.0)*K - Ko;
    dx = Pf.x - 0.5 + jitter*ox;
    dy = Pf.y - of + jitter*oy;
    vec3 d2 = dist(dx,dy, manhattanDistance); // d21, d22 and d23, squared
    p = permute(px.z + Pi.y + oi); // p31, p32, p33
    ox = fract(p*K) - Ko;
    oy = mod(floor(p*K),7.0)*K - Ko;
    dx = Pf.x - 1.5 + jitter*ox;
    dy = Pf.y - of + jitter*oy;
    vec3 d3 = dist(dx,dy, manhattanDistance); // d31, d32 and d33, squared
    // Sort out the two smallest distances (F1, F2)
    vec3 d1a = min(d1, d2);
    d2 = max(d1, d2); // Swap to keep candidates for F2
    d2 = min(d2, d3); // neither F1 nor F2 are now in d3
    d1 = min(d1a, d2); // F1 is now in d1
    d2 = max(d1a, d2); // Swap to keep candidates for F2
    d1.xy = (d1.x < d1.y) ? d1.xy : d1.yx; // Swap if smaller
    d1.xz = (d1.x < d1.z) ? d1.xz : d1.zx; // F1 is in d1.x
    d1.yz = min(d1.yz, d2.yz); // F2 is now not in d2.yz
    d1.y = min(d1.y, d1.z); // nor in  d1.z
    d1.y = min(d1.y, d2.x); // F2 is in d1.y, we're done.
    return sqrt(d1.xy);
}


void main() {
    vec2 F = worley(v_texCoord0 * scale + offset, 1.0, false);
    float F1 = F.x;
    float F2 = F.y;

    o_output = vec4(vec3(F2-F1), 1.0);

    if (premultipliedAlpha) {
        o_output.rgb *= o_output.a;
    }
}
""", "worley-noise"
)) {
    @DoubleParameter("Scale", 0.1, 200.0)
    var scale: Double by parameters

    @BooleanParameter("Premultiplied alpha")
    var premultipliedAlpha: Boolean by parameters

    @Vector2Parameter("Offset")
    var offset: Vector2 by parameters

    init {
        premultipliedAlpha = true
        scale = 5.0
        offset = Vector2.ZERO
    }
}
