out vec4 o_output;
uniform sampler2D tex0;
in vec2 v_texCoord0;
uniform float radius;
uniform float amp0;
uniform float amp1;
uniform vec2 center;
uniform float vignette;
uniform float vignetteSize;
uniform float aberration;
uniform bool linearInput;
uniform bool linearOutput;

void main() {
    vec4 i0 = texture(tex0, v_texCoord0);
    if (!linearInput) {
        i0.rgb = pow(i0.rgb, vec3(2.2));
    }
    vec2 vt = (v_texCoord0 - vec2(0.5, 0.5) + center) * radius + vec2(0.5, 0.5) - center;

    vec2 size = vec2(textureSize(tex0, 0));
    vec2 l = (v_texCoord0 - vec2(0.5, 0.5) + center) * vec2(1.0, size.y/size.x);
    float d = length(l);

    if (vt.x >= 0.0 && vt.y >= 0.0 && vt.x <= 1.0 && vt.y <= 1.0) {
        vec4 i1r = texture(tex0, (v_texCoord0 - vec2(0.5, 0.5) + center) * (radius*(1.0 + aberration)) + vec2(0.5, 0.5) - center);
        vec4 i1g = texture(tex0, (v_texCoord0 - vec2(0.5, 0.5) + center) * (radius*(1.0)) + vec2(0.5, 0.5) - center);
        vec4 i1b = texture(tex0, (v_texCoord0 - vec2(0.5, 0.5) + center) * (radius*(1.0 - aberration)) + vec2(0.5, 0.5) - center);

        i1r.rgb = i1r.a > 0.0 ? i1r.rgb / i1r.a : vec3(0.0);
        i1g.rgb = i1g.a > 0.0 ? i1g.rgb / i1g.a : vec3(0.0);
        i1b.rgb = i1b.a > 0.0 ? i1b.rgb / i1b.a : vec3(0.0);

        vec4 i1 = vec4(i1r.r, i1g.g, i1b.b, 1.0) *  (i1r.a + i1g.a + i1b.a) / 3.0;
        if (!linearInput) {
            i1.rgb = pow(i1.rgb, vec3(2.2));
        }
        o_output = i0 * amp0 + i1 * amp1;
    } else {
        o_output = i0 * 0.5;
    }

    o_output.rgb *= mix(1.0, smoothstep(vignetteSize, 0.0, d), vignette);
    if (!linearOutput) {
        o_output.rgb = pow(o_output.rgb, vec3(1.0 / 2.2));
    }
}