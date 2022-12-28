in vec2 v_texCoord0;
uniform sampler2D tex0; // input
uniform float rotation;
uniform int xSegments;
uniform int ySegments;

out vec4 o_color;

uniform int segments;
float truncate(float x, int segments)  {
    if (segments == 0) {
        return x;
    } else {
        return floor(x * float(segments)) / float(segments);
    }
}

void main() {
    vec2 uv = v_texCoord0 - vec2(0.5);
    float cr = cos(radians(rotation));
    float sr = sin(radians(rotation));

    mat2 rm =  mat2(cr, -sr, sr, cr);

    vec2 ruv = rm * uv;
    vec2 truv = vec2(truncate(ruv.x, xSegments), truncate(ruv.y, ySegments));
    vec2 tuv = transpose(rm) * truv + vec2(0.5);

    vec4 c = vec4(0.0);
    tuv.x = clamp(tuv.x, 0.0, 1.0);
    tuv.y = clamp(tuv.y, 0.0, 1.0);
    c = texture(tex0, tuv);

    o_color = c * texture(tex0, v_texCoord0).a;
}
