#version 330

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