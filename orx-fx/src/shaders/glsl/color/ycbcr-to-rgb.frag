uniform vec4 tint;
in vec2 v_texCoord0;
uniform sampler2D tex0;

out vec4 o_color;
void main() {
    vec2 ts = textureSize(tex0, 0);
    vec4 c = texture(tex0, v_texCoord0);

    if (c.a != 0.0) {
        c.rgb /= c.a;
    }
    c.rgb *= 255.0;

    float y = c.r;
    float cb = c.g;
    float cr = c.b;

    float r = y + 1.402 * (cr - 128.0);
    float g = y - 0.344136 * (cb - 128.0) - 0.714136 * (cr - 128.0);
    float b = y + 1.772 * (cb - 128.0);

    o_color = vec4(r/255.0, g/255.0, b/255.0, 1.0) * c.a;
}