#version 330

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