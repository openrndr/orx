// https://www.shadertoy.com/view/sdcSz2
// ref: (in japanese)
// https://imagingsolution.net/imaging/canny-edge-detector/
uniform float thickness;
uniform sampler2D tex0;

in vec2 v_texCoord0;
out vec4 o_output;

uniform float threshold0;
uniform float threshold1;

uniform vec4 backgroundColor;
uniform vec4 foregroundColor;
uniform float backgroundOpacity;
uniform float foregroundOpacity;

vec2 iResolution = textureSize(tex0, 0);
vec2 fragCoord = v_texCoord0 * iResolution;

float getAve(vec2 uv){
    vec3 rgb = texture(tex0, uv).rgb;
    vec3 lum = vec3(0.299, 0.587, 0.114);
    return dot(lum, rgb);
}

// Detect edge.
vec4 sobel(vec2 fragCoord, vec2 dir){
    vec2 uv = fragCoord/iResolution.xy;
    vec2 texel = 1./iResolution.xy;
    float np = getAve(uv + (vec2(-1,+1) + dir ) * texel * thickness);
    float zp = getAve(uv + (vec2( 0,+1) + dir ) * texel * thickness);
    float pp = getAve(uv + (vec2(+1,+1) + dir ) * texel * thickness);

    float nz = getAve(uv + (vec2(-1, 0) + dir ) * texel * thickness);
    // zz = 0
    float pz = getAve(uv + (vec2(+1, 0) + dir ) * texel * thickness);

    float nn = getAve(uv + (vec2(-1,-1) + dir ) * texel * thickness);
    float zn = getAve(uv + (vec2( 0,-1) + dir ) * texel * thickness);
    float pn = getAve(uv + (vec2(+1,-1) + dir ) * texel * thickness);

    // np zp pp
    // nz zz pz
    // nn zn pn

    #if 0
    float gx = (np*-1. + nz*-2. + nn*-1. + pp*1. + pz*2. + pn*1.);
    float gy = (np*-1. + zp*-2. + pp*-1. + nn*1. + zn*2. + pn*1.);
    #else
    // https://www.shadertoy.com/view/Wds3Rl
    float gx = (np*-3. + nz*-10. + nn*-3. + pp*3. + pz*10. + pn*3.);
    float gy = (np*-3. + zp*-10. + pp*-3. + nn*3. + zn*10. + pn*3.);
    #endif

    vec2 G = vec2(gx,gy);

    float grad = length(G);

    float angle = atan(G.y, G.x);

    return vec4(G, grad, angle);
}

// Make edge thinner.
vec2 hysteresisThr(vec2 fragCoord, float mn, float mx){

    vec4 edge = sobel(fragCoord, vec2(0));

    vec2 dir = vec2(cos(edge.w), sin(edge.w));
    dir *= vec2(-1,1); // rotate 90 degrees.

    vec4 edgep = sobel(fragCoord, dir);
    vec4 edgen = sobel(fragCoord, -dir);

    if(edge.z < edgep.z || edge.z < edgen.z ) edge.z = 0.;

    return vec2(
    (edge.z > mn) ? edge.z : 0.,
    (edge.z > mx) ? edge.z : 0.
    );
}

float cannyEdge(vec2 fragCoord, float mn, float mx){

    vec2 np = hysteresisThr(fragCoord + vec2(-1,+1), mn, mx);
    vec2 zp = hysteresisThr(fragCoord + vec2( 0,+1), mn, mx);
    vec2 pp = hysteresisThr(fragCoord + vec2(+1,+1), mn, mx);

    vec2 nz = hysteresisThr(fragCoord + vec2(-1, 0), mn, mx);
    vec2 zz = hysteresisThr(fragCoord + vec2( 0, 0), mn, mx);
    vec2 pz = hysteresisThr(fragCoord + vec2(+1, 0), mn, mx);

    vec2 nn = hysteresisThr(fragCoord + vec2(-1,-1), mn, mx);
    vec2 zn = hysteresisThr(fragCoord + vec2( 0,-1), mn, mx);
    vec2 pn = hysteresisThr(fragCoord + vec2(+1,-1), mn, mx);

    // np zp pp
    // nz zz pz
    // nn zn pn
    //return min(1., step(1e-3, zz.x) * (zp.y + nz.y + pz.y + zn.y)*8.);
    //return min(1., step(1e-3, zz.x) * (np.y + pp.y + nn.y + pn.y)*8.);
    return min(1., step(1e-2, zz.x*8.) * smoothstep(.0, .3, np.y + zp.y + pp.y + nz.y + pz.y + nn.y + zn.y + pn.y)*8.);
}

void main(){
    float edge = cannyEdge(fragCoord, threshold0, threshold1);
    o_output = mix(foregroundColor * foregroundOpacity, backgroundColor * backgroundOpacity, 1.-edge);
}