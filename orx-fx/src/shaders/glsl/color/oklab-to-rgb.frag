#pragma import color.oklab_to_linear_rgb
#pragma import color.linear_rgb_to_srgb
out vec4 o_output;

in vec2 v_texCoord0;
uniform sampler2D tex0;

void main() {
    vec4 lab = texture(tex0, v_texCoord0);
    vec4 rgba = oklab_to_linear_rgb(lab);
    o_output = linear_rgb_to_srgb(rgba);
}