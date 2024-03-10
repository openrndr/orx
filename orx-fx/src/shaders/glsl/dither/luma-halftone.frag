uniform float scale;
uniform float rotation;
in vec2 v_texCoord0;
uniform sampler2D tex0;
out vec4 o_color;
uniform float threshold;

uniform float freq0;
uniform float freq1;
uniform float gain1;
uniform float phase0;
uniform float phase1;
uniform bool invert;

float cosine_sample(vec2 uv){
    float ca = cos(radians(rotation));
    float sa = sin(radians(rotation));

    vec2 ts = vec2(textureSize(tex0, 0));
    mat2 rm = mat2(1.0, 0.0, 0.0, ts.x/ts.y) * mat2(vec2(ca, sa), vec2(-sa, ca)) * mat2(1.0, 0.0, 0.0, ts.y/ts.x);

    vec2 cuv = (rm * (uv - vec2(0.5))) + vec2(0.5);

    float m = fract(phase0 + cuv.x*freq0 + cos(cuv.y*freq1+phase1*3.141592653)*gain1);
    vec4 c = texture(tex0, v_texCoord0);
    if (c.a != 0.0) {
        c.rgb /= c.a;
    }
    float l = dot(vec3(1.0/3.0), c.rgb);
    if (invert) {
        l = 1.0 - l;
    }

    float t = 0.0;
    t = step(threshold, l * m);
    return t;
}

float cosine_halftone(vec2 uv) {
    int w = 3;
    vec2 step = 1.0 / vec2(textureSize(tex0, 0));
    step /= (2.0*float(w));
    float weight = 0.0;
    float sum = 0.0;
    for (int v = -w; v <= w; ++v) {
        for (int u = -w; u <= w; ++u) {
            sum += cosine_sample(uv + step * vec2(u, v));
            weight+=1.0;
        }
    }
    return sum / weight;
}


void main() {
    vec4 c = texture(tex0, v_texCoord0);
    float t = cosine_halftone(v_texCoord0);
    if (invert) {
        t = 1.0 - t;
    }
    o_color = vec4(t, t, t, 1.0) * c.a;
}