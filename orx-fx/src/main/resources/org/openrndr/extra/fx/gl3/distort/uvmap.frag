#version 330 core

in vec2 v_texCoord0;
uniform sampler2D tex0;// uvmap
uniform sampler2D tex1;// input
out vec4 o_color;

void main() {
    vec2 uv = texture(tex0, v_texCoord0).xy;
    o_color = texture(tex1, uv);
}
