in vec2 v_texCoord0;
uniform sampler2D tex0; // input
uniform float phase;
uniform float amplitude;
uniform float frequency;

out vec4 o_color;

uniform int segments;
float truncate(float x, int segments)  {
    if (segments == 0) {
        return x;
    } else {
        return floor(x*segments) / segments;
    }
}

void main() {
    vec2 uv = v_texCoord0;
    uv.y += amplitude * sin(truncate(uv.x, segments) * 3.1415926535 * frequency + phase * 3.1415926535);
    if (uv.y >= 0.0 && uv.y < 1.0) {
        if (segments == 0) {
            o_color = texture(tex0, uv);
        } else {
            o_color = textureLod(tex0, uv, 0.0);
        }
    } else {
        o_color = vec4(0.0);
    }
}
