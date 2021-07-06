// Licensed under the MIT license:
// https://opensource.org/licenses/MIT.

// Ad[a|o]pted from shader by "noby" https://www.shadertoy.com/view/3sGSWV
uniform sampler2D tex0;

#ifdef OR_IN_OUT
in vec2 v_texCoord0;
#else
varying vec2 v_texCoord0;
#endif

uniform bool useColor;// false
uniform float time;
uniform float grainLiftRatio;// = 0.5;
uniform float grainStrength;//= 1.0;
uniform float grainRate;// = 1.0;
// Range: [0.5, 1.0].
uniform float grainPitch;// = 1.0;

uniform float colorLevel;// = 1.0;

#ifndef OR_GL_FRAGCOLOR
out vec4 o_output;
#endif

// From Dave Hoskins: https://www.shadertoy.com/view/4djSRW.
float hash(vec3 p3){
    p3 = fract(p3 * 0.1031);
    p3 += dot(p3, p3.yzx + 19.19);
    return fract((p3.x + p3.y) * p3.z);
}

// From iq: https://www.shadertoy.com/view/4sfGzS.
float noise(vec3 x){
    vec3 i = floor(x);
    vec3 f = fract(x);
    f = f*f*(3.0-2.0*f);
    return mix(mix(mix(hash(i+vec3(0, 0, 0)),
    hash(i+vec3(1, 0, 0)), f.x),
    mix(hash(i+vec3(0, 1, 0)),
    hash(i+vec3(1, 1, 0)), f.x), f.y),
    mix(mix(hash(i+vec3(0, 0, 1)),
    hash(i+vec3(1, 0, 1)), f.x),
    mix(hash(i+vec3(0, 1, 1)),
    hash(i+vec3(1, 1, 1)), f.x), f.y), f.z);
}

// Slightly high-passed continuous value-noise.
float grain_source(vec3 x, float strength, float pitch){
    float center = noise(x);
    float v1 = center - noise(vec3(1, 0, 0)/pitch + x) + 0.5;
    float v2 = center - noise(vec3(0, 1, 0)/pitch + x) + 0.5;
    float v3 = center - noise(vec3(-1, 0, 0)/pitch + x) + 0.5;
    float v4 = center - noise(vec3(0, -1, 0)/pitch + x) + 0.5;

    float total = (v1 + v2 + v3 + v4) / 4.0;
    return mix(1.0, 0.5 + total, strength);
}

void main() {
    vec2 uv = v_texCoord0;
    vec2 x = gl_FragCoord.xy;

    // Alternatively use iTime here instead and change the grain_rate
    // parameter to correspond to frames-per-second.
    float t = time;
    #ifndef OR_GL_TEXTURE2D
    vec4 colorAlpha = texture(tex0, uv);
    #else
    vec4 colorAlpha = texture2D(tex0, uv);
    #endif

    vec3 color = colorAlpha.rgb;
    vec3 grain = vec3(0);

    if (useColor) {
        float rg = grain_source(vec3(x, floor(grainRate*(t))), grainStrength, grainPitch);
        float gg = grain_source(vec3(x, floor(grainRate*(t+9.0))), grainStrength, grainPitch);
        float bg = grain_source(vec3(x, floor(grainRate*(t-9.0))), grainStrength, grainPitch);

        // Consider using values outside the [0, 1] range as well
        // to introduce interesting color shifts to the source.

        vec3 color_grain = vec3(rg, gg, bg);
        color_grain = mix(vec3(dot(color_grain, vec3(0.2126, 0.7152, 0.0722))), color_grain, colorLevel);
        grain = color_grain;
    } else {
        const float neutral_grain_factor = sqrt(2.0);
        grain = vec3(grain_source(vec3(x, floor(grainRate*t)), grainStrength/neutral_grain_factor, grainPitch));
    }

    // Control whether to add or multiply or lift the source with the grain.
    // Multiply (0.0) should be more true to life, but adjust to taste.

    color = max(mix(color*grain, color+(grain-1.0), grainLiftRatio), 0.0);

    // After this you would normally perform tone mapping,
    // apply the grain before that.
    #ifndef OR_GL_FRACOLOR
    o_output.rgb = color;
    o_output.a = 1.0;
    #else
    gl_FragColor.rgb = color;
    gl_FragColor.a = 1.0;
    #endif
}