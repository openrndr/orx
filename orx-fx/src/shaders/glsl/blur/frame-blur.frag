in vec2 v_texCoord0;
uniform sampler2D tex0; // input image
uniform sampler2D tex1; // accumulator image
uniform float blend;
out vec4 o_color;
void main() {
    vec4 inputColor = texture(tex0, v_texCoord0);
    vec4 accumulator = texture(tex1, v_texCoord0);
    o_color = accumulator * (1.0 - blend) + inputColor * blend;
}