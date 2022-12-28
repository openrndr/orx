// based on https://github.com/svofski/CRT

in vec2 v_texCoord0;
uniform sampler2D tex0; // input
uniform float amount;
uniform float pixelation;
out vec4 o_color;

// Implementation by Evan Wallace (glfx.js)

uniform float filter_gain; // 1.0 is kind of normal
uniform float filter_invgain; // 1.6 is normal

#define PI          3.14159265358
#define FSC         4433618.75
#define FLINE       15625
#define VISIBLELINES 312

#define RGB_to_YIQ  mat3x3( 0.299 , 0.595716 , 0.211456 ,   0.587    , -0.274453 , -0.522591 ,      0.114    , -0.321263 , 0.311135 )
#define YIQ_to_RGB  mat3x3( 1.0   , 1.0      , 1.0      ,   0.9563   , -0.2721   , -1.1070   ,      0.6210   , -0.6474   , 1.7046   )

#define RGB_to_YUV  mat3x3( 0.299 , -0.14713 , 0.615    ,   0.587    , -0.28886  , -0.514991 ,      0.114    , 0.436     , -0.10001 )
#define YUV_to_RGB  mat3x3( 1.0   , 1.0      , 1.0      ,   0.0      , -0.39465  , 2.03211   ,      1.13983  , -0.58060  , 0.0      )

#define fetch(ofs,center,invx) texture(tex0, vec2((ofs) * (invx) + center.x, center.y))

#define FIRTAPS 20
const float FIR[FIRTAPS] = float[FIRTAPS] (-0.008030271,0.003107906,0.016841352,0.032545161,0.049360136,0.066256720,0.082120150,0.095848433,0.106453014,0.113151423,0.115441842,0.113151423,0.106453014,0.095848433,0.082120150,0.066256720,0.049360136,0.032545161,0.016841352,0.003107906);

//#define FIR_GAIN 2.0
//#define FIR_INVGAIN 1.02
#define FIR_GAIN filter_gain
#define FIR_INVGAIN filter_invgain

float width_ratio;
float height_ratio;
float altv;
float invx;


float modulated(vec2 xy, float sinwt, float coswt) {
    vec3 rgb = fetch(0.0, xy, invx).xyz;
    vec3 yuv = RGB_to_YUV * rgb;

    // scanline modulation hack
    // yuv.x *= 0.8 + 0.2 * sin(xy.y*2.0*3.1415*200.0);

    return clamp(yuv.x + yuv.y * sinwt + yuv.z * coswt, 0.0, 1.0);
}

vec2 modem_uv(vec2 xy, int ofs) {
    float t = (xy.x + float(ofs) * invx) * float(textureSize(tex0, 0).x);
    float wt = t * 2.0 * PI / width_ratio;

    float sinwt = sin(wt);
    float coswt = cos(wt + altv);

    vec3 rgb = fetch(float(ofs), xy, invx).xyz;
    vec3 yuv = RGB_to_YUV * rgb;
    float signal = clamp(yuv.x + yuv.y * sinwt + yuv.z * coswt, 0.0, 1.0);

    return vec2(signal * sinwt, signal * coswt);
}


vec3 shadow_mask(vec2 pos){
    const mat2 rot = mat2(0.707,0.707,-0.707,0.707);
    vec3 offset = vec3( 0. , 1./3. , 2./3. );
    vec2 spos = pos * rot * vec2(200.0);
    vec3 ret = vec3(1);
    ret.r = length( fract( spos + vec2(offset.r) ) -.5);
    ret.g = length( fract( spos + vec2(offset.g) ) -.5);
    ret.b = length( fract( spos + vec2(offset.b) ) -.5);
    return clamp( 1.5-ret*2.5 , 0.0, 1.0 );
}

//
//void mainmaskImage(out vec4 fragColor, in vec2 fragCoord ){
//    vec2 xy = fragCoord.st / iResolution.xy;
//
//    fragColor.rgb = shadow_mask( fragCoord.st/ iResolution.y ) * texture(iChannel0, xy).rgb;
//
//
//    if ( fragCoord.y  > iResolution.y*.5 ) {
//        fragColor = texture(iChannel0, xy);
//    }
//}


void main() {
    // vec2 xy = fragCoord.st / iResolution.xy;
    vec2 xy = v_texCoord0;
    width_ratio = float(textureSize(tex0, 0).x) / (float(FSC) / float(FLINE));
    height_ratio = float(textureSize(tex0, 0).y) / float(VISIBLELINES);
    altv = mod(floor(xy.y * float(VISIBLELINES) + 0.5), 2.0) * PI;
    invx = 0.25 / (float(FSC)/float(FLINE)); // equals 4 samples per Fsc period

    // lowpass U/V at baseband
    vec2 filtered = vec2(0.0, 0.0);
    for (int i = 0; i < FIRTAPS; i++) {
        vec2 uv = modem_uv(xy, i - FIRTAPS/2);
        filtered += FIR_GAIN * uv * FIR[i];
    }

    float t = xy.x * float(textureSize(tex0, 0).x);
    float wt = t * 2.0 * PI / width_ratio;

    float sinwt = sin(wt);
    float coswt = cos(wt + altv);

    float luma = modulated(xy, sinwt, coswt) - FIR_INVGAIN * (filtered.x * sinwt + filtered.y * coswt);
    vec3 yuv_result = vec3(luma, filtered.x, filtered.y);

    vec3 rgbmask = shadow_mask( xy * vec2(1.0, float(textureSize(tex0,0).x) / float(textureSize(tex0,0).y)) ); // needs anisotropy like: fragCoord.st/ iResolution.y );
    rgbmask = vec3(1.0,1.0,1.0) * (1.0-pixelation) + rgbmask * pixelation;
    o_color =  texture(tex0,xy) * (1.0-amount) + amount * vec4(rgbmask * ( YUV_to_RGB * yuv_result ), 1.0);

//    if (xy.y>0.5) {
//        o_color = texture(tex0, xy);
//    }
}