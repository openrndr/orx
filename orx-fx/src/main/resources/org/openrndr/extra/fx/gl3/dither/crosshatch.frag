#version 330

in vec2 v_texCoord0;
uniform sampler2D tex0;
out vec4 o_color;

// thresholds
uniform float t1;
uniform float t2;
uniform float t3;
uniform float t4;


// glsl-luma
float luma(vec3 color) {
    return dot(color, vec3(0.299, 0.587, 0.114));
}

float luma(vec4 color) {
    return dot(color.rgb, vec3(0.299, 0.587, 0.114));
}

// glsl-crosshatch
vec3 crosshatch(vec3 texColor, float t1, float t2, float t3, float t4) {
    float lum = luma(texColor);
    vec3 color = vec3(1.0);
    if (lum < t1) {
        if (mod(gl_FragCoord.x + gl_FragCoord.y, 10.0) == 0.0) {
            color = vec3(0.0);
        }
    }
    if (lum < t2) {
        if (mod(gl_FragCoord.x - gl_FragCoord.y, 10.0) == 0.0) {
            color = vec3(0.0);
        }
    }
    if (lum < t3) {
        if (mod(gl_FragCoord.x + gl_FragCoord.y - 5.0, 10.0) == 0.0) {
            color = vec3(0.0);
        }
    }
    if (lum < t4) {
        if (mod(gl_FragCoord.x - gl_FragCoord.y - 5.0, 10.0) == 0.0) {
            color = vec3(0.0);
        }
    }
    return color;
}


void main() {
    vec4 color = texture(tex0, v_texCoord0);
    o_color.rgb = crosshatch(color.rgb, t1, t2, t3, t4);
    o_color.a = color.a;
}