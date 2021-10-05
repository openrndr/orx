uniform sampler2D tex0;
in vec2 v_texCoord0;
out vec4 o_output;
uniform float levels;
uniform float contourWidth;
uniform float contourOpacity;
uniform vec4 contourColor;
uniform float backgroundOpacity;

void main() {
    vec2 step = 1.0 / textureSize(tex0, 0);
    vec4 box = vec4(0.0);
    for (int j = -1; j <=1; ++j) {
        for (int i = -1; i <= 1; ++i) {
            box += texture(tex0, v_texCoord0 + step * vec2(i, j));
        }
    }
    box /= 9.0;
    float v = sin(3.1415926535 * levels * dot(vec3(1.0/3.0),box.xyz));
    float level = floor(dot(vec3(1.0/3.0),box.xyz) * levels) / levels;
    //int plateauIndex = min(levels-1, int(level * levels));
    float contour = 1.0 - smoothstep(0., contourWidth, 0.5 * abs(v) / fwidth(v));

    vec4 t = texture(tex0, v_texCoord0);
    o_output = t * backgroundOpacity * (1.0-contour) + contour * contourColor * contourOpacity * t.a;
}