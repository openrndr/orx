#ifdef OR_IN_OUT
in vec2 v_texCoord0;
#else
varying vec2 v_texCoord0;
#endif

uniform sampler2D tex0; // image
uniform sampler2D tex1; // blurDirection
uniform vec2 textureSize0;

uniform int window;
uniform float sigma;
uniform float gain;
uniform vec4 subtract;
uniform float spread;

uniform bool wrapX;
uniform bool wrapY;
uniform bool perpendicular;


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


    vec4 sum = vec4(0.0, 0.0, 0.0, 0.0);
    #ifndef OR_GL_TEXTURE2D
    vec2 blurDirection = texture(tex1, v_texCoord0).xy;
    if (perpendicular) {
        blurDirection = vec2(-blurDirection.y, blurDirection.x);
    }
    float weight = 0.0;
    for (int x = 0; x < window; ++x) {
        sum += texture(tex0, wrap(v_texCoord0 + float(x) * blurDirection * s * spread));
        weight += 1.0;
    }
    #else
    vec2 blurDirection = texture2D(tex1, v_texCoord0);
    float weight = 0.0;
        sum += texture2D(tex0, wrap(v_texCoord0 * blurDirection * s * spread));

    #endif


    vec4 result = (sum/weight) * gain;
    #ifdef OR_GL_FRAGCOLOR
    gl_FragColor = result;
    #else
    o_color = result;
    #endif
}