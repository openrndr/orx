#version 330 core

in vec2 v_texCoord0;
uniform sampler2D tex0; // input
uniform vec2 center;
uniform float strength;
uniform vec2 dimensions;

out vec4 o_color;

float random(vec3 scale, float seed) {
    /* use the fragment position for a different seed per-pixel */
    return fract(sin(dot(gl_FragCoord.xyz + seed, scale)) * 43758.5453 + seed);
}

// Implementation by Evan Wallace (glfx.js)
void main() {
    vec4 color = vec4(0.0);
    float total = 0.0;
    vec2 toCenter = center - v_texCoord0;

    /* randomize the lookup values to hide the fixed number of samples */
    float offset = random(vec3(12.9898, 78.233, 151.7182), 0.0);

    for (float t = 0.0; t <= 40.0; t++) {
        float percent = (t + offset) / 40.0;
        float weight = 4.0 * (percent - percent * percent);
        vec4 tex = texture(tex0, v_texCoord0 + toCenter * percent * strength);

        /* switch to pre-multiplied alpha to correctly blur transparent images */
        tex.rgb *= tex.a;

        color += tex * weight;
        total += weight;
    }

    o_color = color / total;

    /* switch back from pre-multiplied alpha */
    o_color.rgb /= o_color.a + 0.00001;
}