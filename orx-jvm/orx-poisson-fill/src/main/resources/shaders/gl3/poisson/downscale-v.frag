// the vertical half of downscale.frag: the h1 filter along y, halving the height
in vec2 v_texCoord0;
uniform vec2 targetSize;
uniform sampler2D tex0;
out vec4 o_output;
uniform float h1[5];

void main(){
    ivec2 size = textureSize(tex0, 0).xy;
    ivec2 target = ivec2(floor(targetSize * v_texCoord0));
    int y = target.y * 2 - 10;
    vec4 accum = vec4(0.0);
    for (int dy = -2; dy <= 2; dy++) {
        int py = y + dy;
        if (py < 0 || py > size.y) {
            continue;
        }
        accum += h1[dy + 2] * texelFetch(tex0, ivec2(target.x, py), 0);
    }
    o_output = accum;
}
