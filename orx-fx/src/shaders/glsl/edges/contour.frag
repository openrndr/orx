uniform sampler2D tex0;
in vec2 v_texCoord0;
out vec4 o_output;
uniform float levels;
uniform float contourWidth;
uniform float contourOpacity;
uniform vec4 contourColor;
uniform float backgroundOpacity;
uniform int window;
uniform float bias;

float calc_contour(vec2 uv) {
    vec4 box = texture(tex0, uv);
    float v = sin(3.1415926535 * levels * (dot(vec3(1.0 / 3.0), box.xyz) + bias));
    float level = floor((dot(vec3(1.0 / 3.0), box.xyz) + bias) * levels) / levels;
    float contour = 1.0 - smoothstep(0., contourWidth, 0.5 * abs(v) / fwidth(v));
    return contour;
}

void main() {
    vec2 step = 1.0 / vec2(textureSize(tex0, 0));
    float contour = 0.0;
    float weight = 0.0;

    for (int i = -window; i <= window; ++i) {
        for (int j = -window; j <= window; ++j) {
            contour += calc_contour(v_texCoord0 + step / (float(window) + 1.0) * vec2(float(i), float(j)));
            weight += 1.0;
        }
    }
    contour /= weight;
    vec4 t = texture(tex0, v_texCoord0);
    o_output = t * backgroundOpacity * (1.0 - contour) + contour * contourColor * contourOpacity * t.a;
}