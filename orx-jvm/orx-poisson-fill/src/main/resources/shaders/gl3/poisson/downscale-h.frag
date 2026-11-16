// the horizontal half of downscale.frag: the h1 filter along x, halving the width
in vec2 v_texCoord0;
uniform vec2 targetSize;
uniform sampler2D tex0;
out vec4 o_output;
uniform float h1[5];

void main(){
    ivec2 size = textureSize(tex0, 0).xy;
    ivec2 target = ivec2(floor(targetSize * v_texCoord0));
    int x = target.x * 2 - 10;
    vec4 accum = vec4(0.0);
    for (int dx = -2; dx <= 2; dx++) {
        int px = x + dx;
        if (px < 0 || px > size.x) {
            continue;
        }
        accum += h1[dx + 2] * texelFetch(tex0, ivec2(px, target.y), 0);
    }
    o_output = accum;
}
