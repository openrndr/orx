out vec4 o_output;
uniform sampler2D tex0;
in vec2 v_texCoord0;
uniform float time;

uniform float gain;
uniform float noiseLow;
uniform float noiseHigh;
uniform vec4 tint;
uniform bool monochrome;
uniform float deformAmplitude;
uniform float deformFrequency;
uniform float gapFrequency;
uniform float gapLow;
uniform float gapHigh;

#define HASHSCALE 443.8975
vec2 hash22(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * HASHSCALE);
    p3 += dot(p3, p3.yzx+19.19);
    return fract(vec2((p3.x + p3.y)*p3.z, (p3.x+p3.z)*p3.y));
}

vec3 saturate(vec3 x) {
    return clamp(x, vec3(0.0), vec3(1.0));
}

vec3 aberrationColor(float f) {
    f = f * 3.0 - 1.5;
    return saturate(vec3(-f, 1.0 - abs(f), f));
}

void main() {
    float dk = 1.0/600.0;
    o_output = vec4(0.0);
    for (int k = 0; k < 10; ++k ) {
        vec2 duv = v_texCoord0;
        duv.y += smoothstep(pow(cos(time+float(k)*dk+v_texCoord0.y*1.0),10.0)*0.1+0.1, 0.0, v_texCoord0.x)*deformAmplitude * cos((time+float(k)*dk)*deformFrequency);
        duv.y += smoothstep(pow(1.0-cos(time+float(k)*dk+v_texCoord0.y*1.0),10.0)*0.1+0.1, 0.9, v_texCoord0.x)*deformAmplitude * cos((time+float(k)*dk)*deformFrequency);
        duv.y += sin(v_texCoord0.x*3.1415926535)*0.0;
        float bc = floor(hash22(vec2(time+float(k)*dk, (time+float(k)*dk)*0.1)).x*20.0);

        float gb3 = floor(duv.y*bc)/bc;

        vec2 v = hash22(duv.xy*0.003+time+float(k)*dk);
        vec2 v2 = hash22(duv.xy*0.03+time+float(k)*dk);
        vec2 v2b = hash22(duv.yx*0.03+time+float(k)*dk);
        float stretch = (cos(time+float(k)*dk)*0.001+0.002)*0.3+0.001;
        vec2 h = hash22(duv.yy*stretch+time+float(k)*dk);
        float gap = smoothstep(gapLow, gapHigh, cos(gb3*(gapFrequency+duv.y*gapFrequency + (time+float(k)*dk)*gapFrequency) +duv.x*gapFrequency)) * (cos(gb3)*0.5+0.5);

        float r = smoothstep(noiseLow, noiseHigh, h.x*gap*v2.x)*1.0;
        float g = smoothstep(noiseLow, noiseHigh, h.x*gap*v2.y)*1.0;
        float b = smoothstep(noiseLow, noiseHigh, h.x*gap*v2b.x)*1.0;
        float a = smoothstep(noiseLow, noiseHigh, h.x*gap*v2b.y)*1.0;
        if (!monochrome) {
            o_output += vec4(r, g, b, a)*gain * tint;
        } else {
            o_output += vec4(r, r, r, a)*gain * tint;
        }
    }
    o_output *= o_output.a;
    o_output += texture(tex0, v_texCoord0);
}