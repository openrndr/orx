void gatherSH0(samplerBuffer btex, vec3 p, ivec3 probeCounts, vec3 offset, float spacing, out vec3 blend) {
    vec3 c000;
    vec3 c001;
    vec3 c010;
    vec3 c011;
    vec3 c100;
    vec3 c101;
    vec3 c110;
    vec3 c111;

    vec3 f;
    ivec3 io = gridCoordinates(p, f, probeCounts, offset, spacing);

    fetchSH0(btex, gridIndex(io + ivec3(0,0,0), probeCounts), c000);
    fetchSH0(btex, gridIndex(io + ivec3(0,0,1), probeCounts), c001);
    fetchSH0(btex, gridIndex(io + ivec3(0,1,0), probeCounts), c010);
    fetchSH0(btex, gridIndex(io + ivec3(0,1,1), probeCounts), c011);
    fetchSH0(btex, gridIndex(io + ivec3(1,0,0), probeCounts), c100);
    fetchSH0(btex, gridIndex(io + ivec3(1,0,1), probeCounts), c101);
    fetchSH0(btex, gridIndex(io + ivec3(1,1,0), probeCounts), c110);
    fetchSH0(btex, gridIndex(io + ivec3(1,1,1), probeCounts), c111);

    blend =  mix( mix( mix(c000, c001, f.z), mix(c010, c011, f.z), f.y), mix( mix(c100, c101, f.z), mix(c110, c111, f.z), f.y), f.x);

}