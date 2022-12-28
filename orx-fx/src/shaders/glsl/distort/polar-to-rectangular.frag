#ifdef OR_IN_OUT
in vec2 v_texCoord0;
#else
varying vec2 v_texCoord0;
#endif

uniform vec2 textureSize0;
uniform sampler2D tex0;

uniform bool logPolar;

#ifndef OR_GL_FRAGCOLOR
out vec4 o_color;
#endif

#define PI 3.141592653589793

void main() {
    vec2 uv = v_texCoord0 - vec2(0.5);
    float arg = atan(uv.y, uv.x);

    float bias = 0.0;
    float radius = logPolar? log(1.0 + length(uv)*(exp(1.0)-bias)) / log(1.0+(exp(1.0)-bias)*sqrt(0.5)) : (length(uv) / sqrt(0.5));

    vec2 sourceUV = vec2(arg / (2.0 * PI) + 0.5, radius);

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