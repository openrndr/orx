#version 330

in vec2 v_texCoord0;
uniform sampler2D tex0;
uniform sampler2D tex1;
uniform bool clip;
out vec4 o_color;


void main() {
    vec4 a = texture(tex0, v_texCoord0);
    vec4 b = texture(tex1, v_texCoord0);
    vec3 na = a.a > 0 ? a.rgb/a.a : vec3(0.0);
    vec3 nb = b.a > 0 ? b.rgb/b.a : vec3(0.0);

    vec3 addColor = b.rgb; //mix(vec3(0.0), nb, b.a);

    if (clip) {
        o_color = vec4((na + addColor), 1) * a.a;
    } else {
        o_color = (1.0-a.a) * b + a.a * b.a * vec4(min(na + nb, vec3(1.0)), 1.0) + (1.0-b.a) * a;
    }
}