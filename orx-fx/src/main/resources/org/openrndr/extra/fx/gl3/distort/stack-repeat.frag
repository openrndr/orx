#version 330 core

in vec2 v_texCoord0;
uniform sampler2D tex0;// input
uniform int repeats;
uniform float zoom;
uniform float xOrigin;
uniform float yOrigin;
uniform float xOffset;
uniform float yOffset;
uniform float rotation;

out vec4 o_color;
void main() {
    vec2 origin = vec2((xOrigin+1.0)/2.0, (yOrigin+1.0)/2.0);
    vec2 ts = textureSize(tex0, 0);
    float r = ts.x/ts.y;
    vec2 offset = vec2(1.0, r) * vec2(xOffset, yOffset);
    vec2 uv = v_texCoord0 - vec2(origin);
    float rad = (rotation/180) * 3.1415926535;
    vec2 cs0 = vec2(cos(rad), -sin(rad));
    vec2 cs1 = vec2(sin(rad), cos(rad));
    mat2 rotStep = mat2(cs0, cs1);

    mat2 rot = rotStep;
    vec4 c = texture(tex0, v_texCoord0);
    for (int i = 1; i <= repeats; ++i) {
        //vec2 s = (uv * (1.0 + zoom) * i) + vec2(0.5);
        vec2 s = (rot * uv * pow(1.0 + zoom,i*1.0) )+ vec2(origin) + vec2(offset) * i;
        float f = s.x >= 0.0 && s.y > 0.0 && s.x < 1.0 && s.y < 1.0? 1.0 : 0.0;
        vec4 sc = texture(tex0, s) * f;

        c = c * (1.0-sc.a) + sc;
        if (c.a > 1.0) {
            c.a = 1.0;
        }
        rot *= rotStep;
    }


    o_color = c;
}
