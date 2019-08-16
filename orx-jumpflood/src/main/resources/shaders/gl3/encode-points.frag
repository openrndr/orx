#version 330 core

uniform sampler2D tex0;
in vec2 v_texCoord0;

out vec4 o_color;

void main() {
    vec4 t = texture(tex0, v_texCoord0);
    vec4 outc = vec4(-1.0, -1.0, t.r, 1.0);

    if (t.r > 0.0) {
        outc.xy = v_texCoord0.xy;
    }
    o_color = outc;
}