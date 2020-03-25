#version 330

in vec2 v_texCoord0;
uniform sampler2D tex0;
uniform sampler2D tex1;

out vec4 o_color;

void main() {
    vec4 src = texture(tex0, v_texCoord0);
    vec4 dest = texture(tex1, v_texCoord0);

    float lsrc = src.a * (1.0 - dest.a);
    float lboth = src.a * dest.a;

    o_color = src * lsrc + dest * 0.0 + dest * lboth;
}