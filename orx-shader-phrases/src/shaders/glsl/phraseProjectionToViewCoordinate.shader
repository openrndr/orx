vec3 projectionToViewCoordinate(vec2 uv, float projectionDepth, mat4 projectionInverse) {
    vec4 projectionCoordinate = vec4(uv * 2.0 - 1.0, projectionDepth*2.0-1.0, 1.0);
    vec4 viewCoordinate = projectionInverse * projectionCoordinate;
    return viewCoordinate.xyz / viewCoordinate.w;
}
