#ifdef OR_IN_OUT
in vec2 v_texCoord0;
#else
varying vec2 v_texCoord0;
#endif

uniform vec4 foreground;
uniform vec4 background;
uniform vec2 targetSize;
uniform float size;
uniform float opacity;

#ifndef OR_GL_FRAGCOLOR
out vec4 o_color;
#endif

void main() {
    float r = targetSize.x / targetSize.y;
    vec2 uv = v_texCoord0 - vec2(0.5);
    uv.x *= r;

    vec2 cell = (uv / size);
    ivec2 cellIndex = ivec2(floor(cell));
    vec2 cellUV = cell - vec2(cellIndex);

    int c = (cellIndex.x + cellIndex.y) % 2;
    vec2 w = fwidth(cell);

    vec4 ca;
    vec4 cb;
    if (c == 0) {
        ca = background;
        cb = foreground;
    } else {
        ca = foreground;
        cb = background;
    }
    float s = w.x;
    float fx = smoothstep(s, 0.0, cellUV.x) + smoothstep(1.0 - s, 1.0, cellUV.x);
    float fy = smoothstep(s, 0.0, cellUV.y) + smoothstep(1.0 - s, 1.0, cellUV.y);

    vec4 result = mix(ca, cb, min(0.5, fx * 0.5 + fy * 0.5)) * opacity;

    #ifndef OR_GL_FRAGCOLOR
    o_color = result;
    #else
    gl_FragCoord = result;
    #endif
}