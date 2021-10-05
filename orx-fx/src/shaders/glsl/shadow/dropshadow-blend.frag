in vec2 v_texCoord0;
uniform sampler2D tex0;
uniform sampler2D tex1;
uniform vec2 shift;

out vec4 o_color;
void main() {
    vec4 a = texture(tex0, v_texCoord0-shift);
    vec4 b = texture(tex1, v_texCoord0);
    float alpha = min(1,max(0, b.a));
    o_color = a * (1.0-alpha) + b;
}