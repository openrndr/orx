#version 330

in vec2 v_texCoord0;
uniform vec4 foreground;
uniform vec4 background;
uniform vec2 targetSize;
uniform float size;
uniform float opacity;
out vec4 o_color;
void main() {
    float r = targetSize.x/targetSize.y;
    vec2 uv = v_texCoord0-vec2(0.5);
    uv.x *= r;

    vec2 cell = (uv / size);
    ivec2 cellIndex = ivec2(floor(cell));
    vec2 cellUV = cell - cellIndex;

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
    float fx = smoothstep(s, 0.0, cellUV.x) + smoothstep(1.0-s, 1.0, cellUV.x);
    float fy = smoothstep(s, 0.0, cellUV.y) + smoothstep(1.0-s, 1.0, cellUV.y);

    o_color = mix(ca, cb, min(0.5, fx*0.5+ fy*0.5)) * opacity;
}