in vec2 v_texCoord0;

uniform bool centerWindow;
uniform sampler2D tex0;// image
uniform sampler2D tex1;// blurDirection
uniform vec2 textureSize0;

uniform int window;
uniform float gain;
uniform float spread;

uniform bool wrapX;
uniform bool wrapY;
uniform bool perpendicular;
uniform bool skipSelf;

out vec4 o_color;

vec2 wrap(vec2 uv) {
    vec2 res = uv;
    if (wrapX) { res.x = fract(res.x); }
    if (wrapY) { res.y = fract(res.y); }
    return res;
}

void main() {
    vec2 s = textureSize0;
    s = vec2(1.0 / s.x, 1.0 / s.y);

    vec4 sum = vec4(0.0, 0.0, 0.0, 0.0);
    vec2 blurDirection = texture(tex1, v_texCoord0).xy;
    if (perpendicular) {
        blurDirection = vec2(-blurDirection.y, blurDirection.x);
    }
    float weight = 0.0;

    int start = centerWindow ? -window / 2 : 0;
    int end = centerWindow ? window / 2 + 1 : window;

    for (int x = start; x < end; ++x) {
        sum += texture(tex0, wrap(v_texCoord0 + float(skipSelf ? x + 1 : x) * blurDirection * s * spread));
        weight += 1.0;
    }

    vec4 result = (sum / weight) * gain;
    o_color = result;
}