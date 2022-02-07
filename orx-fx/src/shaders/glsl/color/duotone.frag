#pragma import color.oklab_to_linear_rgb
#pragma import color.linear_rgb_to_oklab
#pragma import color.linear_rgb_to_srgb
#pragma import color.srgb_to_linear_rgb

uniform vec4 tint;
in vec2 v_texCoord0;
uniform sampler2D tex0;
uniform vec4 backgroundColor;
uniform vec4 foregroundColor;
uniform bool labInterpolation;
out vec4 o_color;
void main() {
    vec4 c = texture(tex0, v_texCoord0);
    if (c.a != 0.0) {
        c.rgb /= c.a;
    }

    vec4 bg = backgroundColor;
    bg.rgb *= backgroundColor.a;
    vec4 fg = foregroundColor;
    fg.rgb *= foregroundColor.a;

    if (!labInterpolation) {
        o_color = mix(bg, fg, c.r) * c.a;
    } else {
        bg = srgb_to_linear_rgb(bg);
        bg = linear_rgb_to_oklab(bg);
        fg = srgb_to_linear_rgb(fg);
        fg = linear_rgb_to_oklab(fg);

        vec4 m = mix(bg, fg, c.r);
        m = oklab_to_linear_rgb(m);
        m *= c.a;
        m = linear_rgb_to_srgb(m);
        o_color = m;
    }
}