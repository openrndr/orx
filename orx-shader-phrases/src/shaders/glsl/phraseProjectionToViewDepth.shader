float projectionToViewDepth(float projectionDepth, mat4 projectionInverse) {
    float z = (projectionDepth*2.0-1.0) * projectionInverse[2].z + projectionInverse[3].z;
    float w = (projectionDepth*2.0-1.0) * projectionInverse[2].w + projectionInverse[3].w;
    return z / w;
}