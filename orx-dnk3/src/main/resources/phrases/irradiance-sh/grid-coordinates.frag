ivec3 gridCoordinates(vec3 p, out vec3 f, ivec3 probeCounts, vec3 offset, float spacing) {
    float x = (p.x - offset.x) / spacing;
    float y = (p.y - offset.y)/ spacing;
    float z = (p.z - offset.z) / spacing;

    int ix = int(floor(x)) + probeCounts.x / 2;
    int iy = int(floor(y)) + probeCounts.y / 2;
    int iz = int(floor(z)) + probeCounts.z / 2;

    f.x = fract((x));
    f.y = fract((y));
    f.z = fract((z));

    return ivec3(ix, iy, iz);
}
