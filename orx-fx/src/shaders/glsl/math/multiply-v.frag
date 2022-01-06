#ifdef OR_IN_OUT
in vec2 v_texCoord0;
#else
varying vec2 v_texCoord0;
#endif

uniform sampler2D tex0;
uniform float bias;
uniform bool invertV;
#ifndef OR_GL_FRAGCOLOR
out vec4 o_color;
#endif

void main() {
    #ifndef OR_GL_TEXTURE2D
    vec4 a = texture(tex0, v_texCoord0);
    #else
    vec4 a = texture2D(tex0, v_texCoord0);
    #endif

    float v = invertV ? (1.0 - v_texCoord0.y) : v_texCoord0.y;
    vec4 result = a * (v + bias);
    #ifdef OR_GL_FRAGCOLOR
    gl_FragColor = result;
    #else
    o_color = result;
    #endif
}