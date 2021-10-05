#ifdef OR_IN_OUT
in vec2 v_texCoord0;
#else
varying vec2 v_texCoord0;
#endif

uniform sampler2D tex0;
uniform sampler2D tex1;
uniform bool clip;

float dodge(float base, float blend) {
	return (blend==1.0)?blend:min(base/(1.0-blend),1.0);
}

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

    vec3 na = a.a == 0.0 ? vec3(0.0): a.rgb / a.a;
    vec3 nb = b.a == 0.0 ? vec3(0.0): b.rgb / b.a;

    vec3 m = vec3(
        dodge(na.r, nb.r),
        dodge(na.g, nb.g),
        dodge(na.b, nb.b)
        );

    vec4 result;
    if (clip) {
        result = vec4(na * (1.0 - b.a) + b.a * m, 1.0) * a.a;
    } else {
        result = (1.0-a.a) * b + a.a * b.a * vec4(m, 1.0) + (1.0-b.a) * a;
    }

    #ifdef OR_GL_FRAGCOLOR
    gl_FragColor = result;
    #else
    o_color = result;
    #endif

}