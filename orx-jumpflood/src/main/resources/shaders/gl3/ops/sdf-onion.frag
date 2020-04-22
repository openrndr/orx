#version 330 core

uniform sampler2D tex0;// signed distance
uniform float radius;

in vec2 v_texCoord0;
out vec4 o_color;

void main() {
    float d0 = texture(tex0, v_texCoord0).r;
    o_color = vec4(abs(d0)- radius, 0.0, 0.0, 1.0);
}