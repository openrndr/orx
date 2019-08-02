#version 330

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