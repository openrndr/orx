void fetchSH(samplerBuffer btex, int probeID, out vec3[9] _SH) {
    int offset = probeID * 9;
    _SH[0] = texelFetch(btex, offset).rgb;
    _SH[1] = texelFetch(btex, offset+1).rgb;
    _SH[2] = texelFetch(btex, offset+2).rgb;
    _SH[3] = texelFetch(btex, offset+3).rgb;
    _SH[4] = texelFetch(btex, offset+4).rgb;
    _SH[5] = texelFetch(btex, offset+5).rgb;
    _SH[6] = texelFetch(btex, offset+6).rgb;
    _SH[7] = texelFetch(btex, offset+7).rgb;
    _SH[8] = texelFetch(btex, offset+8).rgb;
}