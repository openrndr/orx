#version 330 core

uniform sampler2D tex0; // image
uniform sampler2D tex1; // distance

uniform float width;
uniform float noise;
uniform vec4 color;
uniform float shape;
uniform float imageOpacity;
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
    vec2 step = 1.0 / textureSize(tex0, 0);
    vec2 distance = texture(tex1, v_texCoord0).rg;
    float d = length(distance);
    vec2 n = normalize(distance);

    vec2 h = hash22(v_texCoord0)*10.0;
    float e = exp(-( pow((d+h.x*noise)*1.0/width, shape)) );
    o_color = original * imageOpacity + original.a* vec4(color.rgb, 1.0) * e * color.a;
    o_color.a = max(o_color.a, 1.0);
}