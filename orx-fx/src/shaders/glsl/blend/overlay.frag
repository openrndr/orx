#ifdef OR_IN_OUT
in vec2 v_texCoord0;
#else
varying vec2 v_texCoord0;
#endif

uniform sampler2D tex0;
uniform sampler2D tex1;
uniform bool clip;

#ifndef OR_GL_FRAGCOLOR
out vec4 o_color;
#endif

vec3 demul(vec4 c) {
    if (c.a == 0.0) {
        return vec3(0.0);
    } else {
        return c.rgb / c.a;
    }
}

void main() {
    #ifndef OR_GL_TEXTURE2D
    vec4 a = texture(tex0, v_texCoord0);
    vec4 b = texture(tex1, v_texCoord0);
    #else
    vec4 a = texture2D(tex0, v_texCoord0);
    vec4 b = texture2D(tex1, v_texCoord0);
    #endif

    vec3 na = demul(a);
    vec3 nb = demul(b);

    vec3 m = vec3(
        na.r <= 0.5? 2.0 * na.r * nb.r : (1.0 - 2.0 * (1.0 - na.r) * (1.0 - nb.r)),
        na.g <= 0.5? 2.0 * na.g * nb.g : (1.0 - 2.0 * (1.0 - na.g) * (1.0 - nb.g)),
        na.b <= 0.5? 2.0 * na.b * nb.b : (1.0 - 2.0 * (1.0 - na.b) * (1.0 - nb.b))
        );

    vec4 result;
    if (clip) {
        vec3 fc = na * (1.0 - b.a) + m * b.a;
        result = vec4(fc, 1.0) * a.a;
    } else {
        result = (1.0-a.a) * b + a.a * b.a * vec4(m, 1.0) + (1.0-b.a) * a;
    }

    #ifdef OR_GL_FRAGCOLOR
    gl_FragColor = result;
    #else
    o_color = result;
    #endif

}