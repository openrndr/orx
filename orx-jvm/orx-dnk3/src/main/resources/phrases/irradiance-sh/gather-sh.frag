void gatherSH(samplerBuffer btex, vec3 p, ivec3 probeCounts, vec3 offset, float spacing, out vec3[9] blend) {
    vec3[9] c000;
    vec3[9] c001;
    vec3[9] c010;
    vec3[9] c011;
    vec3[9] c100;
    vec3[9] c101;
    vec3[9] c110;
    vec3[9] c111;

    vec3 f;
    ivec3 io = gridCoordinates(p, f, probeCounts, offset, spacing);

    fetchSH(btex, gridIndex(io + ivec3(0,0,0), probeCounts), c000);
    fetchSH(btex, gridIndex(io + ivec3(0,0,1), probeCounts), c001);
    fetchSH(btex, gridIndex(io + ivec3(0,1,0), probeCounts), c010);
    fetchSH(btex, gridIndex(io + ivec3(0,1,1), probeCounts), c011);
    fetchSH(btex, gridIndex(io + ivec3(1,0,0), probeCounts), c100);
    fetchSH(btex, gridIndex(io + ivec3(1,0,1), probeCounts), c101);
    fetchSH(btex, gridIndex(io + ivec3(1,1,0), probeCounts), c110);
    fetchSH(btex, gridIndex(io + ivec3(1,1,1), probeCounts), c111);

    for (int i = 0; i < 9; ++i) {
        blend[i] =  mix( mix( mix(c000[i], c001[i], f.z), mix(c010[i], c011[i], f.z), f.y), mix( mix(c100[i], c101[i], f.z), mix(c110[i], c111[i], f.z), f.y), f.x);
    }
}