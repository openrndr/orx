in vec2 v_texCoord0;

uniform sampler2D tex0; // image
uniform sampler2D tex1; // displaceDirection
uniform vec2 textureSize0;

uniform float gain;
uniform float distance;

uniform bool wrapX;
uniform bool wrapY;
uniform bool perpendicular;

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

    vec4 result = texture(tex0, wrap(v_texCoord0 + blurDirection * s * distance))
        * gain;

    o_color = result;
}