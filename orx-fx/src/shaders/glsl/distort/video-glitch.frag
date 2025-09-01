out vec4 o_output;
uniform sampler2D tex0;
in vec2 v_texCoord0;
uniform float time;
uniform float amplitude;
uniform float vfreq;
uniform float pfreq;
uniform float hfreq;
uniform float poffset;
uniform float scrollOffset0;
uniform float scrollOffset1;

uniform float borderHeight;

#define HASHSCALE 443.8975
vec2 hash22(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * HASHSCALE);
    p3 += dot(p3, p3.yzx+19.19);
    return fract(vec2((p3.x + p3.y)*p3.z, (p3.x+p3.z)*p3.y));
}

vec3 saturate(vec3 x) {
    return clamp(x, vec3(0.0), vec3(1.0));
}

vec4 getVideo(vec2 uv, float amplitude, float seconds) {
    float iTime = seconds;
    vec2 look = mod(uv, vec2(1.0));
    float window = 1.0/(1.0 + 20.0*(look.y-mod(iTime*vfreq, 1.0))*(look.y-mod(iTime*vfreq, 1.)));
    look.x = look.x + sin(look.y*pfreq + poffset * 3.1415)/50.0 *(1.0+cos(iTime*hfreq))*window*amplitude;
    look.y = mod(look.y, 1.0);

    vec4 video = texture(tex0, look);
    return video;
}

vec4 aberrationColor(float f) {
    f = f * 3.0 - 1.5;
    return vec4(saturate(vec3(-f, 1.0 - abs(f), f)), 1.0);
}

void main() {
    vec4 c = vec4(0.0);
    float aa = amplitude + smoothstep(borderHeight, 0.0, v_texCoord0.y)*4.0 + smoothstep(1.0-borderHeight, 1.0, v_texCoord0.y)*4.0;
    float ds = scrollOffset1 - scrollOffset0;
    if (aa > 0.0 || ds > 0.0) {
        for (int i = 1; i < 16; ++i) {
            vec4 lc = getVideo(v_texCoord0 + vec2(0.0, scrollOffset0+ds*float(i)), aa, time-float(i)/(16.0*60.0));
            c += lc * (3.0/16.0) * aberrationColor(float(i)/16.0);
        }
        o_output = c;
    } else {
        vec4 lc = texture(tex0, mod(v_texCoord0 + vec2(0.0, scrollOffset1), vec2(1.0)));
        o_output = lc;
    }
}
