#ifdef OR_IN_OUT
in vec2 v_texCoord0;
#else
varying vec2 v_texCoord0;
#endif

uniform sampler2D tex0;
uniform sampler2D tex1;

#ifndef OR_GL_FRAGCOLOR
out vec4 o_color;
#endif

void main() {
    #ifndef OR_GL_TEXTURE2D
    vec4 src = texture(tex0, v_texCoord0);
    vec4 dest = texture(tex1, v_texCoord0);
    #else
    vec4 src = texture2D(tex0, v_texCoord0);
    vec4 dest = texture2D(tex1, v_texCoord0);
    #endif

    float ldest = dest.a * (1.0 - src.a);
    float lboth = src.a * dest.a;

    vec4 result = dest * ldest + src * lboth;
    #ifdef OR_GL_FRAGCOLOR
    gl_FragColor = result;
    #else
    o_color = result;
    #endif
}