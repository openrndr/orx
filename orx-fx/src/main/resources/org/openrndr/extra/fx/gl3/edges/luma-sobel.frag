#version 330 core

in vec2 v_texCoord0;

uniform sampler2D tex0;

uniform vec4 backgroundColor;
uniform vec4 edgeColor;
uniform float backgroundOpacity;
uniform float edgeOpacity;
out vec4 o_color;

float step = 1.0;

float luma(vec4 color){
    vec3 n = color.a == 0.0? vec3(0.0) : color.rgb/color.a;
    return dot(n, vec3(1.0/3.0));
}

void main() {
    vec2 step = 1.0 / textureSize(tex0, 0);

    float tl = luma(texture(tex0, v_texCoord0 + vec2(-step.x, step.y)));
    float l = luma(texture(tex0, v_texCoord0 + vec2(-step.x, 0)));
    float bl = luma(texture(tex0, v_texCoord0 + vec2(-step.x, -step.y)));
    float t = luma(texture(tex0, v_texCoord0 + vec2(0, step.y)));
    float b = luma(texture(tex0, v_texCoord0 + vec2(0, -step.y)));
    float tr = luma(texture(tex0, v_texCoord0 + vec2(step.x, step.y)));
    float r = luma(texture(tex0, v_texCoord0 + vec2(step.x, 0)));
    float br = luma(texture(tex0, v_texCoord0 + vec2(step.x, -step.y)));

    // Sobel masks (see http://en.wikipedia.org/wiki/Sobel_operator)
    //        1 0 -1     -1 -2 -1
    //    X = 2 0 -2  Y = 0  0  0
    //        1 0 -1      1  2  1

    // You could also use Scharr operator:
    //        3 0 -3        3 10   3
    //    X = 10 0 -10  Y = 0  0   0
    //        3 0 -3        -3 -10 -3

    float x = tl + 2.0 * l + bl - tr - 2.0 * r - br;
    float y = -tl - 2.0 * t - tr + bl + 2.0 * b + br;
    float intensity = sqrt((x*x) + (y*y)) / sqrt(2);
    vec4 color = mix(vec4(backgroundColor.rgb, backgroundOpacity), vec4(edgeColor.rgb, edgeOpacity), intensity);

    vec4 a = texture(tex0, v_texCoord0);
    o_color = vec4(color.rgb, 1.0) * color.a * a.a;
}