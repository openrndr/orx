#version 330

uniform vec4 tint;
in vec2 v_texCoord0;
uniform sampler2D tex0;

out vec4 o_color;
void main() {
    vec4 c = texture(tex0, v_texCoord0);
    if (c.a != 0.0) {
        c.rgb /= c.a;
    }
    c.rgb *= 255.0;
    float y = 0.0 + 0.299 * c.r + 0.587 * c.g + 0.114 * c.b;
    float cb = 128 - (0.168736 * c.r) - (0.331264 * c.g) + (0.5 * c.b);
    float cr = 128 + (0.5 * c.r) - 0.418688 * c.g - 0.081312 * c.b;
    o_color = vec4(y/255.0, cb/255.0, cr/255.0, 1.0) * c.a;
}