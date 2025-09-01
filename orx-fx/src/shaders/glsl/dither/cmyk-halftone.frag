/*  Based on CMYK Halftone by tsone https://www.shadertoy.com/view/Mdf3Dn */
uniform float dotSize;

uniform float scale;
uniform float rotation;
uniform float phase;

uniform float blackRotation;
uniform float yellowRotation;
uniform float magentaRotation;
uniform float cyanRotation;

uniform vec4 blackColor;
uniform vec4 yellowColor;
uniform vec4 magentaColor;
uniform vec4 cyanColor;

in vec2 v_texCoord0;
uniform sampler2D tex0;

out vec4 o_color;

#pragma INSERT_PHRASES

vec4 rgb2cmyki(in vec3 c)
{
    float k = max(max(c.r, c.g), c.b);
    return min(vec4(c.rgb / k, k), 1.0);
}

vec3 cmyki2rgb(in vec4 c)
{
    return c.rgb * c.a;
}
vec3 u(vec4 c) {
    if (c.a == 0.0) {
        return vec3(0.0);
    } else {
        return c.rgb / c.a;
    }

}
vec4 cmyki2rgba(in vec4 cmyk) {
    vec4 c = cyanColor * (1.0 - cmyk.r);
    vec4 m = magentaColor * (1.0 - cmyk.g);
    vec4 y = yellowColor * (1.0 - cmyk.b);
    vec4 k = blackColor * (1.0 - cmyk.a);

    vec4 f = c;
    f = (1.0 - f.a) * m + f.a * vec4(u(f) * u(m), 1.0) * m.a + (1.0 - m.a) * f;
    f = (1.0 - f.a) * y + f.a * vec4(u(f) * u(y), 1.0) * y.a + (1.0 - y.a) * f;
    f = (1.0 - f.a) * k + f.a * vec4(u(f) * u(k), 1.0) * k.a + (1.0 - k.a) * f;
    return f;
}


vec2 px2uv(in vec2 px)
{
    return vec2(px / vec2(textureSize(tex0, 0)));
}

vec2 grid(in vec2 px)
{
    return px - mod(px, scale);
}

vec4 ss(in vec4 v)
{
    vec4 vw = fwidth(v);
    return smoothstep(vec4(-vw), vec4(vw), v);
}


float halftone(in vec2 fc, in mat2 m, int channel)
{
    vec2 smp = (grid(m * fc) + 0.5 * scale) * m;

    float s = length(fc - smp) / (dotSize * 2.0 * scale);
    vec2 d2 = m * ((fc) - smp) / (dotSize * 0.5 * scale);
    vec3 texc = texture(tex0, px2uv(smp + vec2(textureSize(tex0, 0)) / 2.0)).rgb;
    float c = 1.0 - rgb2cmyki(texc)[channel];
    return element(d2, c);
}

mat2 rotm(in float r) {
    float cr = cos(r);
    float sr = sin(r);
    return mat2(cr, -sr, sr, cr);
}

void main() {
    vec2 fc = (v_texCoord0 - vec2(0.5)) * vec2(textureSize(tex0, 0));
    fc = domainWarp(fc);

    mat2 mc = rotm(rotation + radians(cyanRotation));
    mat2 mm = rotm(rotation + radians(magentaRotation));
    mat2 my = rotm(rotation + radians(yellowRotation));
    mat2 mk = rotm(rotation + radians(blackRotation));

    vec4 c = cmyki2rgba(
        ss(vec4(
           halftone(fc, mc, 0),
           halftone(fc, mm, 1),
           halftone(fc, my, 2),
           halftone(fc, mk, 3)
           )
        )
    );

    o_color = c;
}