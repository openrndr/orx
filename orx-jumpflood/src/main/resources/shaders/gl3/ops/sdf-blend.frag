#version 330 core

uniform sampler2D tex0;// signed distance
uniform sampler2D tex1;// signed distance
uniform float factor;

in vec2 v_texCoord0;
out vec4 o_color;

void main() {
    float d0 = texture(tex0, v_texCoord0).r;
    float d1 = texture(tex1, v_texCoord0).r;
    float d = mix(d0, d1, factor);
    o_color = vec4(d, 0.0, 0.0, 1.0);
}