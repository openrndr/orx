#pragma import color.oklab_to_linear_rgb
#pragma import color.linear_rgb_to_oklab
#pragma import color.linear_rgb_to_srgb
#pragma import color.srgb_to_linear_rgb

uniform vec4 tint;
in vec2 v_texCoord0;
uniform sampler2D tex0;
uniform vec4 backgroundColor0;
uniform vec4 foregroundColor0;

uniform vec4 backgroundColor1;
uniform vec4 foregroundColor1;

uniform bool labInterpolation;
uniform float rotation;
out vec4 o_color;
void main() {
    vec4 c = texture(tex0, v_texCoord0);
    if (c.a != 0.0) {
        c.rgb /= c.a;
    }
    float ca = cos(radians(rotation));
    float sa = sin(radians(rotation));
    mat2 rm = mat2(vec2(ca, sa), vec2(-sa, ca));

    float f = (rm * (v_texCoord0 - vec2(0.5)) + vec2(0.5)).x;

    vec4 bg0 = backgroundColor0;
    bg0.rgb *= backgroundColor0.a;
    vec4 fg0 = foregroundColor0;
    fg0.rgb *= foregroundColor0.a;

    vec4 bg1 = backgroundColor1;
    bg1.rgb *= backgroundColor1.a;
    vec4 fg1 = foregroundColor1;
    fg1.rgb *= foregroundColor1.a;


    if (!labInterpolation) {
        vec4 bg = mix(bg0, bg1, f);
        vec4 fg = mix(fg0, fg1, f);

        o_color = mix(bg, fg, c.r) * c.a;
    } else {
        bg0 = srgb_to_linear_rgb(bg0);
        bg0 = linear_rgb_to_oklab(bg0);
        fg0 = srgb_to_linear_rgb(fg0);
        fg0 = linear_rgb_to_oklab(fg0);
        bg1 = srgb_to_linear_rgb(bg1);
        bg1 = linear_rgb_to_oklab(bg1);
        fg1 = srgb_to_linear_rgb(fg1);
        fg1 = linear_rgb_to_oklab(fg1);

        vec4 bg = mix(bg0, bg1, f);
        vec4 fg = mix(fg0, fg1, f);

        vec4 m = mix(bg, fg, c.r);
        m = oklab_to_linear_rgb(m);
        m *= c.a;
        m = linear_rgb_to_srgb(m);
        o_color = m;
    }
}