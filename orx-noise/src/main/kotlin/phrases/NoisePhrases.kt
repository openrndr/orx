@file:ShaderPhrases(exports = ["hash22","hash21","valueNoise21"])
package org.openrndr.extra.noise.phrases

import org.openrndr.extra.shaderphrases.annotations.ShaderPhrases

val phraseHash22 = """vec2 hash22(vec2 p) {
    float n = sin(dot(p, vec2(41, 289)));
    return fract(vec2(262144, 32768)*n);
}    
"""

val phraseHash21 = "float hash21(vec2 p) { return fract(1e4 * sin(17.0 * p.x + p.y * 0.1) * (0.1 + abs(sin(p.y * 13.0 + p.x)))); }"

val phraseValueNoise21 = """
    
float noise(vec2 x) {
    vec2 i = floor(x);
    vec2 f = fract(x);

    float a = hash21(i);
    float b = hash21(i + vec2(1.0, 0.0));
    float c = hash21(i + vec2(0.0, 1.0));
    float d = hash21(i + vec2(1.0, 1.0));

    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(a, b, u.x) + (c - a) * u.y * (1.0 - u.x) + (d - b) * u.x * u.y;
}   
""".trimIndent()
