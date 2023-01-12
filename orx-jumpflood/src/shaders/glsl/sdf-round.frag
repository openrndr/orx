uniform sampler2D tex0; // signed distance
uniform float radius;

in vec2 v_texCoord0;
out vec4 o_color;

void main() {
    float d0 = texture(tex0, v_texCoord0).r - radius;
    o_color = vec4(d0, 0.0, 0.0, 1.0);
}