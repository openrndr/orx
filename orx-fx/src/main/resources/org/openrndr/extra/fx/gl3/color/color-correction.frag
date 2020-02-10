/* based on "Brightness, contrast, saturation" by WojtaZam: https://www.shadertoy.com/view/XdcXzn */

#version 330 core

uniform float brightness;
uniform float saturation;
uniform float contrast;
uniform float hueShift;

uniform sampler2D tex0;
in vec2 v_texCoord0;
out vec4 o_color;

mat4 brightnessMatrix(float brightness) {
    return mat4(1, 0, 0, 0,
    0, 1, 0, 0,
    0, 0, 1, 0,
    brightness, brightness, brightness, 1);
}

mat4 contrastMatrix(float contrast) {
    float t = (1.0 - contrast) / 2.0;
    return mat4(contrast, 0, 0, 0,
    0, contrast, 0, 0,
    0, 0, contrast, 0,
    t, t, t, 1 );
}

mat4 saturationMatrix(float saturation) {
    vec3 luminance = vec3(0.3086, 0.6094, 0.0820);
    float oneMinusSat = 1.0 - saturation;
    vec3 red = vec3(luminance.x * oneMinusSat);
    red += vec3(saturation, 0, 0);

    vec3 green = vec3(luminance.y * oneMinusSat);
    green += vec3(0, saturation, 0);

    vec3 blue = vec3(luminance.z * oneMinusSat);
    blue += vec3(0, 0, saturation);

    return mat4(red, 0,
        green, 0,
        blue, 0,
        0, 0, 0, 1 );
}

// from starea's https://www.shadertoy.com/view/MdjBRy, which in turn remixed it from mAlk's https://www.shadertoy.com/view/MsjXRt
vec3 shiftHue(in vec3 col, in float Shift) {
    vec3 P = vec3(0.55735) * dot(vec3(0.55735), col);
    vec3 U = col - P;
    vec3 V = cross(vec3(0.55735), U);
    col = U * cos(Shift * 6.2832) + V * sin(Shift * 6.2832) + P;
    return col;
}

void main() {
    vec4 color = texture(tex0, v_texCoord0);
    vec4 nc = (color.a == 0.0) ? vec4(0.0) : vec4(color.rgb / color.a, color.a);
    nc.rgb = shiftHue(nc.rgb, (hueShift/360.0));
    vec4 cc = brightnessMatrix(brightness) * contrastMatrix((contrast + 1)) * saturationMatrix(saturation + 1) * nc;
    o_color = vec4(cc.rgb, 1.0) * color.a;
}