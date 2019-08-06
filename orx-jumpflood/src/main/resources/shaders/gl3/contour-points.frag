#version 330 core

uniform sampler2D tex0;
in vec2 v_texCoord0;

out vec4 o_color;

void main() {
    vec2 stepSize = 1.0 / textureSize(tex0, 0);
    float ref = step(0.5 , texture(tex0, v_texCoord0).r);

    float laplacian = -4 * ref;

    laplacian += step(0.5, texture(tex0, v_texCoord0 + vec2(stepSize.x, 0.0)).r);
    laplacian += step(0.5, texture(tex0, v_texCoord0 - vec2(stepSize.x, 0.0)).r);
    laplacian += step(0.5, texture(tex0, v_texCoord0 + vec2(0.0, stepSize.y)).r);
    laplacian += step(0.5, texture(tex0, v_texCoord0 - vec2(0.0, stepSize.y)).r);

    float contour = 1.0 - step(0.0, laplacian);

    o_color = vec4(contour, contour, contour, 1.0);
}