#version 330 core

uniform sampler2D tex0;
in vec2 v_texCoord0;
uniform float threshold;
out vec4 o_color;

void main() {
    float ref = step(threshold , texture(tex0, v_texCoord0).a);
    o_color = vec4(ref, ref, ref, 1.0);
}