#pragma import colormap.turbo_colormap

#ifdef OR_IN_OUT
in vec2 v_texCoord0;
#else
varying vec2 v_texCoord0;
#endif

uniform sampler2D tex0;
uniform float     minValue;
uniform float     maxValue;
uniform float     curve;

#ifndef OR_GL_FRAGCOLOR
out vec4 o_color;
#endif

void main() {
    #ifndef OR_GL_TEXTURE2D
    float red = texture(tex0, v_texCoord0).r;
    #else
    float red = texture2D(tex0, v_texCoord0).r;
    #endif
    float value = (red - minValue) / (maxValue - minValue);
    vec3 color = turbo_colormap(pow(value, curve));
    color *= step(value, 1.) * step(0., value);
    vec4 result = vec4(color, 1.);
    #ifdef OR_GL_FRAGCOLOR
    gl_FragColor = result;
    #else
    o_color = result;
    #endif
}
