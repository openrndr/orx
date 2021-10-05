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

vec3 u(vec4 x) {
    return x.a == 0.0? vec3(0.0) : x.rgb / x.a;
}

void main() {
    #ifndef OR_GL_TEXTURE2D
    vec4 a = texture(tex0, v_texCoord0);
    vec4 b = texture(tex1, v_texCoord0);
    #else
    vec4 a = texture2D(tex0, v_texCoord0);
    vec4 b = texture2D(tex1, v_texCoord0);
    #endif

    vec3 na = u(a);
    vec3 nb = u(b);
    vec3 mulColor = mix(vec3(1.0), nb, b.a);

    vec4 result;
    if (clip) {
        result = vec4(a.rgb * mulColor, a.a);
    } else {
        result = (1.0-a.a) * b + a.a * b.a * vec4(na * nb, 1.0) + (1.0-b.a) * a;
    }

    #ifdef OR_GL_FRAGCOLOR
    gl_FragColor = result;
    #else
    o_color = result;
    #endif
}

