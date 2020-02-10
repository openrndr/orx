#version 330

in vec2 v_texCoord0;
uniform sampler2D tex0;
uniform sampler2D tex1;

uniform bool clip;

out vec4 o_color;

vec3 u(vec4 x) {
    return x.a == 0.0? vec3(0.0) : x.rgb / x.a;
}

void main() {
    vec4 a = texture(tex0, v_texCoord0);
    vec4 b = texture(tex1, v_texCoord0);
    vec3 na = u(a);
    vec3 nb = u(b);
    vec3 mulColor = mix(vec3(1.0), nb, b.a);

    if (clip) {
        o_color = vec4(a.rgb * mulColor, a.a);
    } else {
        o_color = (1.0-a.a) * b + a.a * b.a * vec4(na * nb, 1.0) + (1.0-b.a) * a;
    }
}

