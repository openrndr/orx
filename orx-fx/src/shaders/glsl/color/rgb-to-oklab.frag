#pragma import color.linear_rgb_to_oklab
#pragma import color.srgb_to_linear_rgb
out vec4 o_output;

in vec2 v_texCoord0;
uniform sampler2D tex0;

void main() {
    vec4 srgba = texture(tex0, v_texCoord0);
    vec4 rgba = srgb_to_linear_rgb(srgba);
    o_output = linear_rgb_to_oklab(rgba);
}