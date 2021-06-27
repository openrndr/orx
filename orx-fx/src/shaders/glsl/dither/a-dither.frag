// this shader is based on the "a dither" work by Øyvind Kolås
// https://pippin.gimp.org/a_dither/

in vec2 v_texCoord0;
uniform sampler2D tex0;
uniform int pattern;
uniform int levels;

float mask1(int levels, float l, int x, int y, int c) {
    float mask = ((x ^ y * 149) * 1234& 511)/511.0;
    return floor(levels * l + mask)/levels;
}

float mask2(int levels, float l, int x, int y, int c) {
    float mask = (((x+c*17) ^ y * 149) * 1234 & 511)/511.0;
    return floor(levels * l + mask)/levels;
}
float mask3(int levels, float l, int x, int y, int c) {
    float mask =  ((x + y * 237) * 119 & 255)/255.0;
    return floor(levels * l + mask)/levels;
}

float mask4(int levels, float l, int x, int y, int c) {
    float mask = (((x+c*67) + y * 236) * 119 & 255)/255.0;
    return floor(levels * l + mask)/levels;
}

out vec4 o_color;
void main() {
    vec4 c = texture(tex0, v_texCoord0);
    if (c.a > 0.0) {
        c.rgb/=c.a;
    }
    ivec2 ic = ivec2(v_texCoord0 * textureSize(tex0, 0));

    vec4 rgba = vec4(0.0);
    if (pattern == 0) {
        rgba = vec4(mask1(levels, c.r, ic.x, ic.y, 0), mask1(levels, c.g, ic.x, ic.y, 1), mask1(levels, c.b, ic.x, ic.y, 2), mask1(levels, c.a, ic.x, ic.y, 3));
    } else if (pattern == 1) {
        rgba = vec4(mask2(levels, c.r, ic.x, ic.y, 0), mask2(levels, c.g, ic.x, ic.y, 1), mask2(levels, c.b, ic.x, ic.y, 2), mask2(levels, c.a, ic.x, ic.y, 3));
    } else if (pattern == 2) {
        rgba = vec4(mask3(levels, c.r, ic.x, ic.y, 0), mask3(levels, c.g, ic.x, ic.y, 1), mask3(levels, c.b, ic.x, ic.y, 2), mask3(levels, c.a, ic.x, ic.y, 3));
    } else {
        rgba = vec4(mask4(levels, c.r, ic.x, ic.y, 0), mask4(levels, c.g, ic.x, ic.y, 1), mask4(levels, c.b, ic.x, ic.y, 2), mask4(levels, c.a, ic.x, ic.y, 3));
    }

    rgba.rgb *= rgba.a;
    o_color = rgba;
}
