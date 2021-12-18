uniform sampler2D tex0;// signed distance
uniform vec4 skeletonColor;
uniform vec4 backgroundColor;
uniform vec4 foregroundColor;
uniform float angleTreshold;

in vec2 v_texCoord0;

out vec4 o_color;

void main() {
    float centerDistance = texture(tex0, v_texCoord0).r;
    vec2 step = 1.0 / textureSize(tex0, 0);

    float minDistance = 1000.0;

    float nd = texture(tex0, v_texCoord0 + step * vec2(0.0, -1.0)).r;
    float ed = texture(tex0, v_texCoord0 + step * vec2(1.0, 0.0)).r;
    float wd = texture(tex0, v_texCoord0 + step * vec2(-1.0, 0.0)).r;
    float sd = texture(tex0, v_texCoord0 + step * vec2(0.0, 1.0)).r;

    float nd2 = texture(tex0, v_texCoord0 + step * vec2(-1.0, -1.0)).r;
    float ed2 = texture(tex0, v_texCoord0 + step * vec2(-1.0, 1.0)).r;
    float wd2 = texture(tex0, v_texCoord0 + step * vec2(1.0, -1.0)).r;
    float sd2 = texture(tex0, v_texCoord0 + step * vec2(1.0, 1.0)).r;

    float r = -centerDistance * 8.0 + nd + ed + wd + sd + nd2 + ed2 + wd2 + sd2;

    vec4 fc = vec4(0.0);

    if (centerDistance < 0.0) {
        fc += foregroundColor * foregroundColor.a;
    } else {
        fc += backgroundColor * backgroundColor.a;
    }

    if (r > 0.0 && centerDistance < 0.0) {
        fc = fc * (1.0 - skeletonColor.a) + (skeletonColor * skeletonColor.a);
    }

    o_color = fc;
}