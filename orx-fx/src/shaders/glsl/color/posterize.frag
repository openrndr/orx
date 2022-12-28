in vec2 v_texCoord0;
uniform int window;
uniform sampler2D tex0;
uniform int levels;
out vec4 o_output;
void main() {
    vec4 c = texture(tex0, v_texCoord0);
    vec2 step = 1.0 / textureSize(tex0, 0);
    float w = 0.0;
    vec3 s = vec3(0.0);
    for (int v = -window; v <= window; ++v) {
        for (int u = -window; u <= window; ++u) {
            vec4 c = texture(tex0, v_texCoord0 + (step/(2.0*float(window))) * vec2(u,v) );
            if (c.a != 0.0) {
                c.rgb /= c.a;
            }
            vec3 q = min(floor(c.rgb * float(levels))/float(levels-1.0), vec3(1.0));
            s += q;
            w += 1.0;
        }
    }
    vec3 q = s / w;
    o_output = vec4(q * c.a, c.a);
}