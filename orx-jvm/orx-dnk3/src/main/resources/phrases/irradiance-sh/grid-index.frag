int gridIndex(ivec3 p, ivec3 probeCounts) {
    ivec3 c = clamp(p, ivec3(0), probeCounts - ivec3(1));
    return c.x + c.y * probeCounts.x + c.z * probeCounts.x * probeCounts.y;
}