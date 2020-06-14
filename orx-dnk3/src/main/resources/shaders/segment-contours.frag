#version 330

uniform usampler2D tex0;
in vec2 v_texCoord0;
out vec4 o_output;

void main() {
    ivec2 ts = textureSize(tex0, 0);
    ivec2 pixel = ivec2(v_texCoord0 * ts);

    ivec2 c = pixel;
    ivec2 n = c + ivec2(0, -1);
    ivec2 s = c + ivec2(0, 1);
    ivec2 w = c + ivec2(-1, 0);
    ivec2 e = c + ivec2(1, 0);

    float sf = 0.0;
    for (int i = 0; i < 1; ++i) {
        float f = 1.0;
        uint sc = texelFetch(tex0, c, i).r;
        uint sn = texelFetch(tex0, n, i).r;
        uint ss = texelFetch(tex0, s, i).r;
        uint se = texelFetch(tex0, e, i).r;
        uint sw = texelFetch(tex0, w, i).r;

        if (sc == se) f -= 0.25;
        if (sc == sw) f -= 0.25;
        if (sc == sn) f -= 0.25;
        if (sc == ss) f -= 0.25;
        sf+= f;
    }
    o_output = vec4(vec3(sf/0.5), 1.0);
}