package org.openrndr.extra.color.colormaps

import org.openrndr.extra.shaderphrases.ShaderPhrase
import org.openrndr.extra.shaderphrases.ShaderPhraseBook

/**
 * Colormaps represent a class of functions taking a value in the range `0.0..1.0` and returning particular RGB color
 * values. Colormaps can be used in data visualization for representing additional information/dimension of the data
 * e.g:
 * - depth
 * - elevation
 * - heat
 *
 * Note: the [ShaderPhrase] GLSL functions gathered in this [ShaderPhraseBook] also have respective Kotlin
 * implementations.
 *
 * @see org.openrndr.extra.color.colormaps.turboColormap
 * @see org.openrndr.extra.color.colormaps.spectralZucconi6
 */
object ColormapPhraseBook : ShaderPhraseBook("colormap") {

    // Copyright 2019 Google LLC.
    // SPDX-License-Identifier: Apache-2.0
    /**
     * Polynomial approximation in GLSL for the Turbo colormap.
     *
     * See [Turbo, An Improved Rainbow Colormap for Visualization](https://research.google/blog/turbo-an-improved-rainbow-colormap-for-visualization/),
     * [the source of this code](https://gist.github.com/mikhailov-work/0d177465a8151eb6ede1768d51d476c7),
     *
     * @author Anton Mikhailov (mikhailov@google.com) - Colormap Design
     * @author Ruofei Du (ruofei@google.com) - GLSL Approximation
     * @see org.openrndr.extra.color.colormaps.turboColormap
     */
    val turboColormap = ShaderPhrase("""
        |vec3 turbo_colormap(in float x) {
        |    const vec4 kRedVec4 = vec4(0.13572138, 4.61539260, -42.66032258, 132.13108234);
        |    const vec4 kGreenVec4 = vec4(0.09140261, 2.19418839, 4.84296658, -14.18503333);
        |    const vec4 kBlueVec4 = vec4(0.10667330, 12.64194608, -60.58204836, 110.36276771);
        |    const vec2 kRedVec2 = vec2(-152.94239396, 59.28637943);
        |    const vec2 kGreenVec2 = vec2(4.27729857, 2.82956604);
        |    const vec2 kBlueVec2 = vec2(-89.90310912, 27.34824973);
        |
        |    x = clamp(x, 0.0, 1.0);
        |    vec4 v4 = vec4( 1.0, x, x * x, x * x * x);
        |    vec2 v2 = v4.zw * v4.z;
        |    return vec3(
        |        dot(v4, kRedVec4)   + dot(v2, kRedVec2),
        |        dot(v4, kGreenVec4) + dot(v2, kGreenVec2),
        |        dot(v4, kBlueVec4)  + dot(v2, kBlueVec2)
        |    );
        |}""".trimMargin())

    /**
     * Accurate spectral colormap developed by Alan Zucconi.
     *
     * See [Improving the Rainbow](https://www.alanzucconi.com/2017/07/15/improving-the-rainbow/) article,
     * [the source of this code](https://www.shadertoy.com/view/ls2Bz1)
     *
     * @author Alan Zucconi
     * @see org.openrndr.extra.color.colormaps.spectralZucconi6
     */
    val spectralZucconi6 = ShaderPhrase("""
        |#pragma import colormap.bump3y
        |
        |vec3 spectral_zucconi6(in float x) {
        |
        |	   const vec3 c1 = vec3(3.54585104, 2.93225262, 2.41593945);
        |	   const vec3 x1 = vec3(0.69549072, 0.49228336, 0.27699880);
        |	   const vec3 y1 = vec3(0.02312639, 0.15225084, 0.52607955);
        |
        |	   const vec3 c2 = vec3(3.90307140, 3.21182957, 3.96587128);
        |	   const vec3 x2 = vec3(0.11748627, 0.86755042, 0.66077860);
        |	   const vec3 y2 = vec3(0.84897130, 0.88445281, 0.73949448);
        |
        |	   return
        |		     bump3y(c1 * (x - x1), y1) +
        |		     bump3y(c2 * (x - x2), y2) ;
        |}""".trimMargin())

    /**
     * A function used internally by [spectralZucconi6].
     *
     * @author Alan Zucconi
     */
    val bump3y = ShaderPhrase("""
        |vec3 bump3y(in vec3 x, in vec3 yoffset) {
        |    vec3 y = vec3(1.0) - x * x;
        |    return clamp(y - yoffset, vec3(0.0), vec3(1.0));
        |}""".trimMargin())

}
