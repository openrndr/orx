@file:JvmName("Depth")
@file:ShaderPhrases([])
package org.openrndr.extra.shaderphrases.phrases

import org.openrndr.extra.shaderphrases.annotations.ShaderPhrases

/**
 * phrase for conversion from view to projection depth
 * @param viewDepth depth in view space ([0.0 .. -far])
 * @param projection projection matrix
 * @return depth in projection space ([0.0 .. 1.0]]
 */
const val viewToProjectionDepth = """
float viewToProjectionDepth(float viewDepth, mat4 projection) {
    float z = viewDepth * projection[2].z + projection[3].z;
    float w = viewDepth * projection[2].w + projection[3].w;
    return z / w;
}
"""

/**
 * phrase for conversion from view to projection depth
 * @param projectionDepth depth in projection space ([0.0 .. 1.0])
 * @param projectionInversed inverse of the projection matrix
 * @return depth in view space ([0.0 .. -far]]
 */
const val projectionToViewDepth = """
float projectionToViewDepth(float projectionDepth, mat4 projectionInverse) {
    float z = projectionDepth * projectionInverse[2].z + projectionInverse[3].z;
    float w = projectionDepth * projectionInverse[2].w + projectionInverse[3].w;
    return z / w;
}
"""
