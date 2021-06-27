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
    vec4 a = texture(tex0, v_texCoord0);
    vec4 b = texture(tex1, v_texCoord0);
    #else
    vec4 a = texture2D(tex0, v_texCoord0);
    vec4 b = texture2D(tex1, v_texCoord0);
    #endif

    float ai = max(a.z, max(a.x, a.y));
    float bi = max(b.z, max(b.x, b.y));

    vec3 f = a.rgb - (1.0-b.rgb)*2.0*b.a;

    vec4 result;
    result.rgb = max(vec3(0.0), f) * (1.0) + b.rgb * (1.0-a.a);
    result.a = 1.0;

    #ifdef OR_GL_FRAGCOLOR
    gl_FragColor = result;
    #else
    o_color = result;
    #endif
}