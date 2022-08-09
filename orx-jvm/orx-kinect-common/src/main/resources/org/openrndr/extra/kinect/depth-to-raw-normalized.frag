#ifndef KINECT_FLIPV
layout(origin_upper_left) in vec4 gl_FragCoord;
#endif

uniform usampler2D  tex0;             // kinect raw
uniform float       maxDepthValue;
#ifdef KINECT_FLIPH
uniform int         resolutionXMinus1;
#endif
out     float       outDepth;         // measured in meters

void main() {
    ivec2 uv = ivec2(gl_FragCoord);
    #ifdef KINECT_FLIPH
    uv = ivec2(resolutionXMinus1 - uv.x, uv.y);
    #endif
    uint uintDepth = texelFetch(tex0, uv, 0).r;
    outDepth = float(uintDepth) / maxDepthValue;
}
