void fetchSH0(samplerBuffer btex, int probeID, out vec3 _SH) {
    int offset = probeID * 9;
    _SH = texelFetch(btex, offset).rgb;
}