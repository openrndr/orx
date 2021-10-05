uniform sampler2D tex0;
in vec2 v_texCoord0;
out vec4 o_color;

uniform vec2 delta;
uniform int radius;

float random(vec3 scale, float seed) {
    /* use the fragment position for a different seed per-pixel */
    return fract(sin(dot(gl_FragCoord.xyz + seed, scale)) * 43758.5453 + seed);
}

// Implementation by Evan Wallace (glfx.js)
void main() {
    vec4 center = texture(tex0, v_texCoord0);
    vec2 color = vec2(0.0);
    vec2 total = vec2(0.0);

    /* randomize the lookup values to hide the fixed number of samples */
    float offset = random(vec3(12.9898, 78.233, 151.7182), 0.0);

    for (float t = -30.0; t <= 30.0; t++) {
        float percent = (t + offset - 0.5) / 30.0;
        float weight = 1.0 - abs(percent);
        vec2 tex = texture(tex0, v_texCoord0 + delta * percent).xy;
        color.x += tex.x * weight;
        total.x += weight;

        if (abs(t) < 15.0) {
            weight = weight * 2.0 - 1.0;
            color.y += tex.y * weight;
            total.y += weight;
        }
    }
    float c = clamp(10000.0 * (color.y / total.y - color.x / total.x) + 0.5, 0.0, 1.0);

    o_color = vec4(c, c, c, 1.0) * center.a;
}