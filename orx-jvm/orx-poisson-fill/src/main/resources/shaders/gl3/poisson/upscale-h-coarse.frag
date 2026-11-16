// the horizontal part of the h1 filter of upscale.frag, on the rows of the previous level upscaled by inserting zeros
in vec2 v_texCoord0;
uniform vec2 targetSize;
uniform sampler2D tex0;
out vec4 o_output;
uniform float h1[5];

void main(){
    ivec2 target = ivec2(floor(targetSize * v_texCoord0));
    vec4 accum = vec4(0.0);
    for (int dx = -2; dx <= 2; dx++) {
        int px = target.x + dx;
        if (px % 2 != 0) {
            continue;
        }
        accum += h1[dx + 2] * texelFetch(tex0, ivec2(px / 2 + 5, target.y), 0);
    }
    o_output = accum;
}
