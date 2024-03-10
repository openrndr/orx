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

void main() {
    #ifndef OR_GL_TEXTURE2D
    vec4 a = texture(tex0, v_texCoord0);
    vec4 b = texture(tex1, v_texCoord0);
    #else
    vec4 a = texture2D(tex0, v_texCoord0);
    vec4 b = texture2D(tex1, v_texCoord0);
    #endif

    float alpha = min(1.0, max(0.0, b.a));

    vec4 result;
    if (!clip) {
        result = a * (1.0 - alpha) + b;
        result.a = clamp(o_color.a, 0.0, 1.0);
    } else {
        result = a * (1.0 - alpha) + b * a.a;
    }

    #ifdef OR_GL_FRAGCOLOR
    gl_FragColor = result;
    #else
    o_color = result;
    #endif
}