package org.openrndr.extras.color.phrases

import org.openrndr.extra.shaderphrases.ShaderPhrase
import org.openrndr.extra.shaderphrases.ShaderPhraseBook

object ColorPhraseBook : ShaderPhraseBook("color") {
    val phraseAtan2 = ShaderPhrase("""
        |float atan2(in float y, in float x) {
        |   bool s = (abs(x) > abs(y));
        |   return mix(PI/2.0 - atan(x,y), atan(y,x), float(s));
        |}""".trimMargin())

    val phraseLinearRgbToOKLab = ShaderPhrase("""
        |vec4 linear_rgb_to_oklab(vec4 c) {
        |   c.rgb = max(vec3(0.0), c.rgb);
        |   const mat3 kCONEtoLMS = mat3(
        |       0.4122214708,  0.2119034982,  0.0883024619,
        |       0.5363325363,  0.6806995451,  0.2817188376,
        |       0.0514459929,  0.1073969566,  0.6299787005);
        |
        |   const mat3 kRot = mat3(
        |       0.2104542553,  1.9779984951,  0.0259040371,
        |       0.7936177850, -2.4285922050,  0.7827717662,
        |       -0.0040720468,  0.4505937099, -0.8086757660);
        |   vec3 lms = pow(kCONEtoLMS * c.rgb, vec3(1.0/3.0));
        |   vec4 res = vec4((kRot) * lms, c.a);
        |   return res;
        |}""".trimMargin())

    val oklabToLinearRgb = ShaderPhrase("""
        |vec4 oklab_to_linear_rgb(vec4 lab) {
        |   const mat3 kLMStoCONE = mat3(
        |       1.0,            1.0,           1.0,
        |       0.3963377774,  -0.1055613458, -0.0894841775,
        |       0.2158037573,  -0.0638541728,  -1.2914855480);
        |   const mat3 kRot = mat3(
        |       4.0767416621, -1.2684380046, -0.0041960863,
        |       -3.3077115913,  2.6097574011,  -0.7034186147,
        |       0.2309699292, -0.3413193965, 1.7076147010);
        |   vec3 lms = kLMStoCONE * lab.rgb;
        |   lms = lms * lms * lms;
        |   vec4 res = vec4(kRot * lms,lab.a);
        |   return res;
        |}""".trimMargin())

    val phraseLabToLch = ShaderPhrase( """
        |vec4 lab_to_lch(vec4 lab) {
        |   float r = length(lab.yz);
        |   float h = atan2(lab[2], lab[1]);
        |   return vec4(lab[0], c, h, lab.a);
        |}""".trimMargin())

    val phraseLchToLab = ShaderPhrase("""
        |vec4 lch_to_lab(vec4 lch) {
        |   float a = lch[1] * cos(lch[2]);
        |   float b = lch[1] * sin(lch[2]);
        |   return vec4(lab[0], a, b, lab.a);
        |}""".trimMargin())

    val linearRgbToSRgb = ShaderPhrase("""
        |vec4 linear_rgb_to_srgb(vec4 c) {
        |   const float t = 0.00313066844250063;
        |   return vec4(
        |       c.r <= t ? c.r * 12.92 : 1.055 * pow(c.r, 1 / 2.4) - 0.055,
        |       c.g <= t ? c.g * 12.92 : 1.055 * pow(c.g, 1 / 2.4) - 0.055,
        |       c.b <= t ? c.b * 12.92 : 1.055 * pow(c.b, 1 / 2.4) - 0.055,
        |       c.a);
        |}""".trimMargin())

    val phraseSRgbToLinearRgb = ShaderPhrase("""
        |vec4 srgb_to_linear_rgb(vec4 c) {
        |   const float t = 0.0404482362771082;
        |   return vec4(
        |       c.r <= t ? c.r / 12.92 : pow( (c.r + 0.055) / 1.055, 2.4),
        |       c.g <= t ? c.g / 12.92 : pow( (c.g + 0.055) / 1.055, 2.4),
        |       c.b <= t ? c.b / 12.92 : pow( (c.b + 0.055) / 1.055, 2.4),
        |       c.a);
        |}""".trimMargin())
}