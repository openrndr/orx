#version 330

in vec2 v_texCoord0;
uniform sampler2D tex0;
uniform sampler2D tex1;

out vec4 o_color;
void main() {
    vec4 a = texture(tex0, v_texCoord0);
    vec4 b = texture(tex1, v_texCoord0);
    vec3 na = a.a > 0 ? a.rgb/a.a : vec3(0.0);
    vec3 subColor = b.rgb;
    o_color = vec4(max(na - subColor, vec3(0.0)), 1) * a.a;
}