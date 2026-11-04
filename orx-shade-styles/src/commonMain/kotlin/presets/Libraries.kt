package org.openrndr.extra.shadestyles.presets

import org.openrndr.draw.GlslLibrary
import org.openrndr.draw.glslLibrary
import org.openrndr.extra.color.colormaps.ColormapPhraseBook

/**
 * GLSL library with 3D simplex noise, `float snoise3(vec3 v)`, which returns values in [-1, 1]
 */
val snoise3Library: GlslLibrary = glslLibrary(
    "snoise3", """
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
vec3 snoise3_mod289(vec3 x) {
    return x - floor(x * (1.0 / 289.0)) * 289.0;
}

vec4 snoise3_mod289(vec4 x) {
    return x - floor(x * (1.0 / 289.0)) * 289.0;
}

vec4 snoise3_permute(vec4 x) {
    return snoise3_mod289(((x*34.0)+1.0)*x);
}

vec4 snoise3_taylorInvSqrt(vec4 r)
{
    return 1.79284291400159 - 0.85373472095314 * r;
}

float snoise3(vec3 v)
{
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
    i = snoise3_mod289(i);
    vec4 p = snoise3_permute( snoise3_permute( snoise3_permute(
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
    vec4 norm = snoise3_taylorInvSqrt(vec4(dot(p0,p0), dot(p1,p1), dot(p2, p2), dot(p3,p3)));
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
"""
)

/** GLSL library with `vec3 bump3y(vec3 x, vec3 yoffset)`, used by [spectralZucconi6Library] */
val bump3yLibrary: GlslLibrary = glslLibrary("bump3y", ColormapPhraseBook.bump3y.phrase)

/** GLSL library with the Turbo colormap, `vec3 turbo_colormap(float x)` */
val turboColormapLibrary: GlslLibrary = glslLibrary("turbo_colormap", ColormapPhraseBook.turboColormap.phrase)

/** GLSL library with Alan Zucconi's spectral colormap, `vec3 spectral_zucconi6(float x)` */
val spectralZucconi6Library: GlslLibrary = glslLibrary(
    "spectral_zucconi6",
    ColormapPhraseBook.spectralZucconi6.phrase.lines().filterNot { it.trim().startsWith("#pragma") }.joinToString("\n"),
    bump3yLibrary
)

/**
 * GLSL library with conversions between sRGB and OKLab: `oklab_srgb_to_linear`, `oklab_linear_to_srgb`,
 * `oklab_from_linear` and `oklab_to_linear`, all `vec3 f(vec3 c)`
 */
val oklabLibrary: GlslLibrary = glslLibrary(
    "oklab", """
vec3 oklab_srgb_to_linear(vec3 c) {
    c = max(c, vec3(0.0));
    return mix(c / 12.92, pow((c + 0.055) / 1.055, vec3(2.4)), step(vec3(0.04045), c));
}

vec3 oklab_linear_to_srgb(vec3 c) {
    c = max(c, vec3(0.0));
    return mix(c * 12.92, 1.055 * pow(c, vec3(1.0 / 2.4)) - 0.055, step(vec3(0.0031308), c));
}

vec3 oklab_from_linear(vec3 c) {
    float l = 0.4122214708 * c.r + 0.5363325363 * c.g + 0.0514459929 * c.b;
    float m = 0.2119034982 * c.r + 0.6806995451 * c.g + 0.1073969566 * c.b;
    float s = 0.0883024619 * c.r + 0.2817188376 * c.g + 0.6299787005 * c.b;
    vec3 lms = sign(vec3(l, m, s)) * pow(abs(vec3(l, m, s)), vec3(1.0 / 3.0));
    return vec3(
        0.2104542553 * lms.x + 0.7936177850 * lms.y - 0.0040720468 * lms.z,
        1.9779984951 * lms.x - 2.4285922050 * lms.y + 0.4505937099 * lms.z,
        0.0259040371 * lms.x + 0.7827717662 * lms.y - 0.8086757660 * lms.z
    );
}

vec3 oklab_to_linear(vec3 c) {
    vec3 lms = vec3(
        c.x + 0.3963377774 * c.y + 0.2158037573 * c.z,
        c.x - 0.1055613458 * c.y - 0.0638541728 * c.z,
        c.x - 0.0894841775 * c.y - 1.2914855480 * c.z
    );
    lms = lms * lms * lms;
    return vec3(
        4.0767416621 * lms.x - 3.3077115913 * lms.y + 0.2309699292 * lms.z,
        -1.2684380046 * lms.x + 2.6097574011 * lms.y - 0.3413193965 * lms.z,
        -0.0041960863 * lms.x - 0.7034186147 * lms.y + 1.7076147010 * lms.z
    );
}
"""
)

/**
 * GLSL library with the film grain of orx-fx: `float film_grain_source(vec3 x, float strength, float pitch)`
 */
val filmGrainLibrary: GlslLibrary = glslLibrary(
    "film_grain", """
float film_grain_hash(vec3 p3) {
    p3 = fract(p3 * 0.1031);
    p3 += dot(p3, p3.yzx + 19.19);
    return fract((p3.x + p3.y) * p3.z);
}

float film_grain_noise(vec3 x) {
    vec3 i = floor(x);
    vec3 f = fract(x);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(mix(film_grain_hash(i + vec3(0, 0, 0)), film_grain_hash(i + vec3(1, 0, 0)), f.x),
                   mix(film_grain_hash(i + vec3(0, 1, 0)), film_grain_hash(i + vec3(1, 1, 0)), f.x), f.y),
               mix(mix(film_grain_hash(i + vec3(0, 0, 1)), film_grain_hash(i + vec3(1, 0, 1)), f.x),
                   mix(film_grain_hash(i + vec3(0, 1, 1)), film_grain_hash(i + vec3(1, 1, 1)), f.x), f.y), f.z);
}

float film_grain_source(vec3 x, float strength, float pitch) {
    float center = film_grain_noise(x);
    float v1 = center - film_grain_noise(vec3(1, 0, 0) / pitch + x) + 0.5;
    float v2 = center - film_grain_noise(vec3(0, 1, 0) / pitch + x) + 0.5;
    float v3 = center - film_grain_noise(vec3(-1, 0, 0) / pitch + x) + 0.5;
    float v4 = center - film_grain_noise(vec3(0, -1, 0) / pitch + x) + 0.5;
    float total = (v1 + v2 + v3 + v4) / 4.0;
    return mix(1.0, 0.5 + total, strength);
}
"""
)

/**
 * GLSL library with the ordered dither masks of orx-fx: `float a_dither(int pattern, int levels, float l, ivec2 p,
 * int c)` quantizes `l` to `levels` levels with the mask `pattern` (0 to 3) at pixel `p` for channel `c`
 */
val aDitherLibrary: GlslLibrary = glslLibrary(
    "a_dither", """
float a_dither(int pattern, int levels, float l, ivec2 p, int c) {
    int x = p.x;
    int y = p.y;
    float mask;
    if (pattern == 0) {
        mask = float((x ^ y * 149) * 1234 & 511) / 511.0;
    } else if (pattern == 1) {
        mask = float(((x + c * 17) ^ y * 149) * 1234 & 511) / 511.0;
    } else if (pattern == 2) {
        mask = float((x + y * 237) * 119 & 255) / 255.0;
    } else {
        mask = float(((x + c * 67) + y * 236) * 119 & 255) / 255.0;
    }
    return floor(float(levels) * l + mask) / float(levels);
}
"""
)
