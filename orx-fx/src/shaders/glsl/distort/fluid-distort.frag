// created by florian berger (flockaroo) - 2016
// License Creative Commons Attribution-NonCommercial-ShareAlike 3.0 Unported License.

// single pass CFD
// ---------------
// this is some "computational flockarooid dynamics" ;)
// the self-advection is done purely rotational on all scales.
// therefore i dont need any divergence-free velocity field.
// with stochastic sampling i get the proper "mean values" of rotations
// over time for higher order scales.
//
// try changing "RotNum" for different accuracies of rotation calculation
// for even RotNum uncomment the line #define SUPPORT_EVEN_ROTNUM

#define RotNum 5
//#define SUPPORT_EVEN_ROTNUM

//#define keyTex iChannel3
//#define KEY_I texture(keyTex,vec2((105.5-32.0)/256.0,(0.5+0.0)/3.0)).x

const float ang = 2.0*3.1415926535/float(RotNum);
mat2 m = mat2(cos(ang), sin(ang), -sin(ang), cos(ang));
mat2 mh = mat2(cos(ang*0.5), sin(ang*0.5), -sin(ang*0.5), cos(ang*0.5));

uniform sampler2D tex0;
uniform float time;
uniform float random;

in vec2 v_texCoord0;
uniform vec2 targetSize;

uniform float blend;

out vec4 o_color;

float getRot(vec2 pos, vec2 b) {
    vec2 Res = textureSize(tex0, 0);
    vec2 p = b;
    float rot = 0.0;
    for (int i = 0; i < RotNum; i++) {
        rot += dot(texture(tex0, fract((pos + p) / Res.xy)).xy -vec2(0.5), p.yx * vec2(1, -1));
        p = m * p;
    }
    return rot / float(RotNum)/dot(b, b);
}

void main() {
    vec2 pos = v_texCoord0 * targetSize;
    vec2 Res = textureSize(tex0, 0);

    vec2 b = vec2(cos(ang * random), sin(ang * random));
    vec2 v = vec2(0);
    float bbMax = 0.5 * Res.y;
    bbMax *= bbMax;
    for (int l = 0; l < 20; l++) {
        if (dot(b, b) > bbMax) break;
        vec2 p = b;
        for (int i = 0; i < RotNum; i++) {
            #ifdef SUPPORT_EVEN_ROTNUM
            v += p.yx * getRot(pos + p, -mh * b);
            #else
            // this is faster but works only for odd RotNum
            v += p.yx * getRot(pos + p, b);
            #endif
            p = m*p;
        }
        b *= 2.0;
    }
    o_color = vec4(0.0, 0.0, 0.0, 1.0);
    o_color.xy = texture(tex0, fract((pos + v * vec2(-1, 1) * 2.0) / Res.xy)).xy * (1.0-blend) + v_texCoord0 * blend;
}