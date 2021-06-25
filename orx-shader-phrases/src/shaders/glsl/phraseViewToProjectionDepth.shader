float viewToProjectionDepth(float viewDepth, mat4 projection) {
    float z = viewDepth * projection[2].z + projection[3].z;
    float w = viewDepth * projection[2].w + projection[3].w;
    return z / w;
}