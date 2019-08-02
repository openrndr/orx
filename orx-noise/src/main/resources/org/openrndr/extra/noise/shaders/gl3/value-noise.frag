#version 330
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