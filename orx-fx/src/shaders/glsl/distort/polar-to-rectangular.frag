#ifdef OR_IN_OUT
in vec2 v_texCoord0;
#else
varying vec2 v_texCoord0;
#endif

uniform vec2 textureSize0;
uniform sampler2D tex0;

#ifndef OR_GL_FRAGCOLOR
out vec4 o_color;
#endif

#define PI 3.141592653589793

void main() {
    vec2 uv = v_texCoord0 - vec2(0.5);
    float arg = atan(uv.y, uv.x);
    float radius = length(uv);
    vec2 sourceUV = vec2(arg / (2*PI) + 0.5, radius/sqrt(0.5));

    #ifndef OR_GL_TEXTURE2D
    vec4 result = texture(tex0, sourceUV);
    #else
    vec4 result = texture2D(tex0, sourceUV);
    #endif

    #ifdef OR_GL_FRAGCOLOR
    gl_FragColor = result;
    #else
    o_color = result;
    #endif
}