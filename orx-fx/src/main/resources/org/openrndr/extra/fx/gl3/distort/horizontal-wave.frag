#version 330 core

in vec2 v_texCoord0;
uniform sampler2D tex0; // input
uniform float phase;
uniform float amplitude;
uniform float frequency;

out vec4 o_color;
void main() {
    vec2 uv = v_texCoord0;
    uv.x += amplitude * cos(uv.y * 3.1415926535 * frequency + phase * 3.1415926535);
    if (uv.x >= 0.0 && uv.x < 1.0) {
        o_color = texture(tex0, uv);
    } else {
        o_color = vec4(0.0);
    }
}
