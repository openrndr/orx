#version 330 core

in vec2 v_texCoord0;
uniform sampler2D tex0; // input
uniform float amount;
out vec4 o_color;

// Implementation by Evan Wallace (glfx.js)
void main() {
    vec4 color = texture(tex0, v_texCoord0);
    float r = color.r;
    float g = color.g;
    float b = color.b;

    color.r = min(1.0, (r * (1.0 - (0.607 * amount))) + (g * (0.769 * amount)) + (b * (0.189 * amount)));
    color.g = min(1.0, (r * 0.349 * amount) + (g * (1.0 - (0.314 * amount))) + (b * 0.168 * amount));
    color.b = min(1.0, (r * 0.272 * amount) + (g * 0.534 * amount) + (b * (1.0 - (0.869 * amount))));

    o_color = color;
}