#version 330 core

uniform sampler2D tex0;// image
uniform sampler2D tex1;// distance

uniform float width;
uniform float noise;
uniform float shape;
uniform float imageOpacity;
uniform float opacity;
in vec2 v_texCoord0;

out vec4 o_color;
#define HASHSCALE 443.8975
vec2 hash22(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * HASHSCALE);
    p3 += dot(p3, p3.yzx+19.19);
    return fract(vec2((p3.x + p3.y)*p3.z, (p3.x+p3.z)*p3.y));
}

void main() {
    vec4 original = texture(tex0, v_texCoord0);
    vec2 ts = textureSize(tex0, 0);
    vec2 step = 1.0 / ts;
    vec2 distance = texture(tex1, v_texCoord0).rg;

    vec2 n = normalize(distance);

    vec2 uvOff = distance * step * vec2(1.0, -1.0);

    vec4 border = vec4(0.0);

    float w = 0.0;
    for (int j = -1; j <= 1; ++j) {
        for (int i = -1; i <= 1; ++i) {
            vec4 smp = texture(tex0, v_texCoord0 + uvOff + step * vec2(i, j));
            border += smp;
        }
    }

    vec4 nborder = border.a>0.0?vec4(border.rgb/border.a, 1.0):vec4(0.0);
    float d = length(distance);

    vec2 h = hash22(v_texCoord0)*10.0;
    float rwidth = max(ts.x, ts.y) * width;
    float e = shape > 0.0 ? exp(-( pow((d+h.x*noise)*1.0/rwidth, shape))) : 1.0;
    o_color = original * imageOpacity + (1.0-original.a)* nborder * e * opacity;

}