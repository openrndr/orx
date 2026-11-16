// the horizontal part of the g filter of upscale.frag, on the current level
in vec2 v_texCoord0;
uniform sampler2D tex0;
out vec4 o_output;
uniform float g[3];

void main(){
    ivec2 size = textureSize(tex0, 0).xy;
    ivec2 coords = ivec2(v_texCoord0 * vec2(size));
    vec4 accum = vec4(0.0);
    for (int dx = -1; dx <= 1; dx++) {
        int px = coords.x + dx;
        if (px < 0 || px >= size.x) {
            continue;
        }
        accum += g[dx + 1] * texelFetch(tex0, ivec2(px, coords.y), 0);
    }
    o_output = accum;
}
