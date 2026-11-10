package org.openrndr.extra.shapes.bezierpatches

import org.openrndr.draw.ShadeStyle
import org.openrndr.draw.shadeStyle
import org.openrndr.math.Vector3
import org.openrndr.shape.Box

/**
 * GLSL that deforms a vertex in a [BezierVolume] given as arguments, so that a shader can deform by several volumes:
 * `void bezierVolumeDeform(inout vec3 position, inout vec3 normal, vec3 points[64], vec3 corner, vec3 size)` places
 * [position], in the coordinates of the basis box with [corner] and [size], at the point of the volume at its u, v, w in
 * the box, and maps [normal] by the inverse transpose of the Jacobian of that mapping, normalized. [points] are the 64
 * control points in the order `points[w][v][u]`, at index `w * 16 + v * 4 + u`.
 *
 * It reads no uniforms: pass those that [setBezierVolume] sets with a prefix, like
 * `bezierVolumeDeform(x_position, x_normal, p_e0_Points, p_e0_Corner, p_e0_Size)` after `setBezierVolume(basis, volume,
 * "e0_")`. Put it in the vertex preamble once for all volumes.
 */
const val bezierVolumeDeformFunctionGlsl = """
vec4 bezierVolume_b(float t) {
    float s = 1.0 - t;
    return vec4(s * s * s, 3.0 * s * s * t, 3.0 * s * t * t, t * t * t);
}

vec4 bezierVolume_db(float t) {
    float s = 1.0 - t;
    return vec4(-3.0 * s * s, 3.0 * s * s - 6.0 * s * t, 6.0 * s * t - 3.0 * t * t, 3.0 * t * t);
}

void bezierVolumeDeform(inout vec3 position, inout vec3 normal, vec3 points[64], vec3 corner, vec3 size) {
    vec3 uvw = (position - corner) / size;
    vec4 bu = bezierVolume_b(uvw.x);
    vec4 bv = bezierVolume_b(uvw.y);
    vec4 bw = bezierVolume_b(uvw.z);
    vec4 dbu = bezierVolume_db(uvw.x);
    vec4 dbv = bezierVolume_db(uvw.y);
    vec4 dbw = bezierVolume_db(uvw.z);
    vec3 p = vec3(0.0);
    vec3 ju = vec3(0.0);
    vec3 jv = vec3(0.0);
    vec3 jw = vec3(0.0);
    for (int k = 0; k < 4; k++) {
        for (int j = 0; j < 4; j++) {
            for (int i = 0; i < 4; i++) {
                vec3 c = points[k * 16 + j * 4 + i];
                p += c * (bu[i] * bv[j] * bw[k]);
                ju += c * (dbu[i] * bv[j] * bw[k]);
                jv += c * (bu[i] * dbv[j] * bw[k]);
                jw += c * (bu[i] * bv[j] * dbw[k]);
            }
        }
    }
    // the derivatives of the point by the position in the box
    ju /= size.x;
    jv /= size.y;
    jw /= size.z;
    position = p;
    // the inverse transpose of the Jacobian is its cofactor matrix divided by its determinant
    vec3 r0 = cross(jv, jw);
    vec3 r1 = cross(jw, ju);
    vec3 r2 = cross(ju, jv);
    float det = dot(ju, r0);
    vec3 n = r0 * normal.x + r1 * normal.y + r2 * normal.z;
    float len = length(n);
    if (len > 0.0) {
        normal = n / len * sign(det);
    }
}
"""

/**
 * GLSL that deforms a vertex in a [BezierVolume], the GPU form of
 * [org.openrndr.extra.shapes.distort.envelope] for meshes: [bezierVolumeDeformFunctionGlsl], and
 * `void bezierVolumeDeform(inout vec3 position, inout vec3 normal)`, which deforms by the volume that [setBezierVolume]
 * sets without a prefix: the uniforms `p_bezierVolumePoints`, `p_bezierVolumeCorner` and `p_bezierVolumeSize`. Put it
 * in the vertex preamble of a shade style and call it in its vertex transform, before anything that uses `x_position`
 * and `x_normal`, as [bezierVolumeShadeStyle] does. For several volumes in one shader, use
 * [bezierVolumeDeformFunctionGlsl] with prefixes.
 */
const val bezierVolumeDeformGlsl = bezierVolumeDeformFunctionGlsl + """
void bezierVolumeDeform(inout vec3 position, inout vec3 normal) {
    bezierVolumeDeform(position, normal, p_bezierVolumePoints, p_bezierVolumeCorner, p_bezierVolumeSize);
}
"""

/**
 * Sets the uniforms of a volume on this shade style: the control points of [volume] and the corner and size of the
 * [basis] box, as `p_${prefix}Points` (64 vec3, `points[w][v][u]` at index `w * 16 + v * 4 + u`), `p_${prefix}Corner`
 * and `p_${prefix}Size`. The default [prefix] gives the uniforms that the two-argument `bezierVolumeDeform` of
 * [bezierVolumeDeformGlsl] reads; other prefixes are for [bezierVolumeDeformFunctionGlsl], one per volume. Call it
 * again when the volume changes.
 */
fun ShadeStyle.setBezierVolume(basis: Box, volume: BezierVolume, prefix: String = "bezierVolume") {
    parameter("${prefix}Points", Array(64) { volume.points[it / 16][(it / 4) % 4][it % 4] })
    parameter("${prefix}Corner", basis.corner)
    parameter("${prefix}Size", Vector3(basis.width, basis.height, basis.depth))
}

/**
 * A shade style that deforms meshes in [volume] by their places in the [basis] box, on the GPU, positions and normals
 * alike; see [bezierVolumeDeformGlsl]. Instanced meshes are deformed in their own coordinates, before their instance
 * transforms. Update it with [setBezierVolume] when the volume changes, and combine it with a shade style that colors
 * the mesh, like with `+`.
 */
fun bezierVolumeShadeStyle(basis: Box, volume: BezierVolume): ShadeStyle = shadeStyle {
    vertexPreamble = bezierVolumeDeformGlsl
    vertexTransform = "bezierVolumeDeform(x_position, x_normal);"
}.apply { setBezierVolume(basis, volume) }
