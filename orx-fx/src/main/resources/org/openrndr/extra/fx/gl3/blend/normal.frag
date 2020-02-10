#version 330

in vec2 v_texCoord0;
uniform sampler2D tex0;
uniform sampler2D tex1;
uniform bool clip;

out vec4 o_color;
void main() {
    vec4 a = texture(tex0, v_texCoord0);
    vec4 b = texture(tex1, v_texCoord0);
    float alpha = min(1,max(0, b.a));

    if (!clip) {
        o_color = a * (1.0-alpha) + b;
        o_color.a = clamp(o_color.a, 0.0, 1.0);
    } else {
        o_color = a * (1.0-alpha) + b * a.a;
    }
}