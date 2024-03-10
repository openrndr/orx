/*  Based on CMYK Halftone by tsone https://www.shadertoy.com/view/Mdf3Dn */
uniform float dotSize;

#define SST 0.888
#define SSQ 0.288

uniform float scale;
uniform float rotation;

in vec2 v_texCoord0;
uniform sampler2D tex0;

out vec4 o_color;

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
        return c.rgb/c.a;
    }

}
vec4 cmyki2rgba(in vec4 cmyk){
    vec4 c = vec4(0.0, 1.0, 1.0, 1.0)*(1.0-cmyk.r);
    vec4 m = vec4(1.0, 0.0, 1.0, 1.0)*(1.0-cmyk.g);
    vec4 y = vec4(1.0, 1.0, 0.0, 1.0)*(1.0-cmyk.b);
    vec4 k = vec4(0.0, 0.0, 0.0, 1.0)*(1.0-cmyk.a);

    vec4 f = c;
    f = (1.0-f.a) * m + f.a * vec4(u(f)*u(m),1.0) * m.a + (1.0-m.a) * f;
    f = (1.0-f.a) * y + f.a * vec4(u(f)*u(y),1.0) * y.a + (1.0-y.a) * f;
    f = (1.0-f.a) * k + f.a * vec4(u(f)*u(k),1.0) * k.a + (1.0-k.a) * f;
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
    return smoothstep(vec4(SST-SSQ), vec4(SST+SSQ), v);
}

vec4 halftone(in vec2 fc,in mat2 m)
{
    vec2 smp = (grid(m*fc) + 0.5*scale) * m;
    float s = min(length(fc-smp) / (dotSize*0.5*scale), 1.0);
    vec3 texc = texture(tex0, px2uv(smp+vec2(textureSize(tex0, 0))/2.0)).rgb;
    vec4 c = rgb2cmyki(texc);
    return c+s;
}

mat2 rotm(in float r)
{
    float cr = cos(r);
    float sr = sin(r);
    return mat2(
    cr,-sr,
    sr,cr
    );
}

void main() {
    vec2 fc = v_texCoord0 * vec2(textureSize(tex0, 0)) - vec2(textureSize(tex0, 0))/2.0;

    mat2 mc = rotm(rotation + radians(15.0));
    mat2 mm = rotm(rotation + radians(75.0));
    mat2 my = rotm(rotation);
    mat2 mk = rotm(rotation + radians(45.0));

    float k = halftone(fc, mk).a;
    vec4 c = cmyki2rgba(ss(vec4(
    halftone(fc, mc).r,
    halftone(fc, mm).g,
    halftone(fc, my).b,
    halftone(fc, mk).a
    )));

    o_color = c;
}