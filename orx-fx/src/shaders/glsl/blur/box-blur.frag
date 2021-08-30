#ifdef OR_IN_OUT
in vec2 v_texCoord0;
#else
varying vec2 v_texCoord0;
#endif

uniform sampler2D tex0;
uniform vec2 textureSize0;
uniform vec2 blurDirection;

uniform int window;
uniform float sigma;
uniform float gain;
uniform vec4 subtract;
uniform float spread;

uniform bool wrapX;
uniform bool wrapY;


#ifndef OR_GL_FRAGCOLOR
out vec4 o_color;
#endif

vec2 wrap(vec2 uv) {
    vec2 res = uv;
    if (wrapX) {
        res.x = mod(res.x, 1.0);
    }
    if (wrapY) {
        res.y = mod(res.y, 1.0);
    }
    return res;

}
void main() {
    vec2 s = textureSize0;
    s = vec2(1.0 / s.x, 1.0 / s.y);

    #ifndef OR_WEBGL1
    int w = window;
    int WS = -window;
    int WE = window;
    #else
    int w = 3;
    #define WS -3
    #define WE 3
    #endif

    vec4 sum = vec4(0.0, 0.0, 0.0, 0.0);
    float weight = 0.0;
    for (int x = WS; x<= WE; ++x) {
        float lw = 1.0;
        #ifndef OR_GL_TEXTURE2D
        sum += texture(tex0, wrap(v_texCoord0 + float(x) * blurDirection * s * spread));
        #else
        sum += texture2D(tex0, wrap(v_texCoord0 + float(x) * blurDirection * s * spread));
        #endif

        weight += lw;
    }

    vec4 result = (sum / weight) * gain;
    #ifdef OR_GL_FRAGCOLOR
    gl_FragColor = result;
    #else
    o_color = result;
    #endif
}