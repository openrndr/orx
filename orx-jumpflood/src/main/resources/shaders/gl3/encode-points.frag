#version 330 core

uniform sampler2D tex0;
in vec2 v_texCoord0;

out vec4 o_color;

void main() {
    float ref = texture(tex0, v_texCoord0).r;
    vec4 outc = vec4(-1.0, -1.0, 0.0, 1.0);

    if (ref > 0.5) {
        outc.xy = v_texCoord0.xy;
    }
    o_color = outc;
}