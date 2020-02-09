#version 330

in vec2 v_texCoord0;
uniform sampler2D tex0;
uniform sampler2D tex1;

float blendColorBurn(float base, float blend) {
	return (blend==0.0) ? blend : max((1.0 - ((1.0 - base) / blend)), 0.0);
}

out vec4 o_color;
void main() {
    vec4 a = texture(tex0, v_texCoord0);
    vec4 b = texture(tex1, v_texCoord0);

    vec3 na = a.a == 0.0 ? vec3(0.0): a.rgb / a.a;
    vec3 nb = b.a == 0.0 ? vec3(0.0): b.rgb / b.a;

    vec3 m = vec3(
        blendColorBurn(na.r, nb.r),
        blendColorBurn(na.g, nb.g),
        blendColorBurn(na.b, nb.b)
        );

    o_color = vec4(na * (1.0 - b.a) + b.a * m, 1.0) * a.a;
}