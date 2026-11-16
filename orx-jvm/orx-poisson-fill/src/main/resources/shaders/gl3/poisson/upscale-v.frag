// the vertical part of upscale.frag: the g filter of the current level and the h1 filter of the previous level
in vec2 v_texCoord0;
uniform sampler2D tex0; // the current level filtered along x with g
uniform sampler2D tex1; // the previous level filtered along x with h1, on its own rows
out vec4 o_output;
uniform float h1[5];
uniform float h2;
uniform float g[3];

void main(){
    ivec2 size = textureSize(tex0, 0).xy;
    ivec2 coords = ivec2(v_texCoord0 * vec2(size));
    vec4 accum = vec4(0.0);
    for (int dy = -1; dy <= 1; dy++) {
        int py = coords.y + dy;
        if (py < 0 || py >= size.y) {
            continue;
        }
        accum += g[dy + 1] * texelFetch(tex0, ivec2(coords.x, py), 0);
    }
    for (int dy = -2; dy <= 2; dy++) {
        int py = coords.y + dy;
        if (py % 2 != 0) {
            continue;
        }
        accum += h2 * h1[dy + 2] * texelFetch(tex1, ivec2(coords.x, py / 2 + 5), 0);
    }
    o_output = accum;
}
