uniform vec4 background;
uniform float backgroundOpacity;
in vec2 v_texCoord0;
uniform sampler2D tex0;

out vec4 o_color;
void main() {
    vec4 c = texture(tex0, v_texCoord0);
    o_color = c + (1.0 - c.a) * background * backgroundOpacity;
}