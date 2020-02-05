#version 330

uniform vec4 tint;
in vec2 v_texCoord0;
uniform sampler2D tex0;

out vec4 o_color;
void main() {
    vec4 c = texture(tex0, v_texCoord0);
    o_color = vec4(c.rgb * tint.rgb, c.a) * tint.a;
}