#version 330 core

uniform sampler2D tex0; // image
uniform sampler2D tex1; // distance

uniform float angle;
uniform float width;
uniform float noise;

in vec2 v_texCoord0;

out vec4 o_color;
#define HASHSCALE 443.8975
vec2 hash22(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * HASHSCALE);
    p3 += dot(p3, p3.yzx+19.19);
    return fract(vec2((p3.x + p3.y)*p3.z, (p3.x+p3.z)*p3.y));
}


void main() {
    float r = radians(angle);

    vec4 color = texture(tex0, v_texCoord0);


    vec2 step = 1.0 / textureSize(tex0, 0);

    vec2 distance = vec2(0.0);
    float totalWeight = 0.0;
    for (int j = 0; j < 1; ++j) {
        for (int i =0; i < 1; ++i) {
            vec2 hn = (hash22(v_texCoord0)-0.5) * noise;
            vec2 s = texture(tex1, v_texCoord0 + step * vec2(i,j)).xy + hn*0.0;
            distance += s;
            totalWeight += 1.0;
        }
    }
    distance /= totalWeight;

    //vec2 distance = texture(tex1, v_texCoord0).xy + hn;

    float d = length(distance);
    vec2 n = normalize(distance);
    vec2 l = vec2(cos(r), sin(r));

    float e = smoothstep(0.0, width, d) * smoothstep(width*2.0, width, d);
    float o = max(0.0,dot(n, l))*e ;
    float o2 = max(0.0,-dot(n, l))*e ;
    //o_color = vec4(vec3(o),1.0) * color.a;

    vec3 nc = color.a > 0.0?
        color.rgb/color.a : vec3(0.0);


    o_color = vec4(nc+vec3(o)-vec3(o2),1.0) * color.a;
}