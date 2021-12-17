in vec2 v_texCoord0;
uniform sampler2D tex0; // input
uniform float amount;
out vec4 o_color;

void main() {
    vec4 color = texture(tex0, v_texCoord0);

    color.rgb = mix(color.rgb, 1.0 - color.rgb, amount);

    o_color = color;
}