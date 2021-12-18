in vec2 v_texCoord0;
uniform sampler2D tex0; // input
uniform float amount;
out vec4 o_color;

void main() {
    vec4 color = texture(tex0, v_texCoord0);

    float a = color.a;
    vec3 rgb = a > 0.0 ? color.rgb / a : vec3(0.0);
    rgb = mix(rgb, 1.0 - rgb, amount);

    o_color = vec4(rgb * a, a);
}