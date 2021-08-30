#ifdef OR_IN_OUT
in vec2 v_texCoord0;
#else
varying vec2 v_texCoord0;
#endif

uniform vec2 textureSize0;
uniform sampler2D tex0;
uniform vec2 origin;

#ifndef OR_GL_FRAGCOLOR
out vec4 o_color;
#endif

#define PI 3.141592653589793

uniform int angleLevels;
uniform int radiusLevels;

void main() {
    vec2 uv = v_texCoord0 - origin;
    float arg = (uv.x-0.5) * 2 * PI;
    float radius = (uv.y) * sqrt(0.5);


    vec2 sourceUV = radius * vec2(cos(arg), sin(arg)) + vec2(0.5);

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

