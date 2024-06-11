package org.openrndr.extra.computeshaders

import org.openrndr.draw.ColorBuffer
import org.openrndr.math.IntVector2
import org.openrndr.math.IntVector3
import kotlin.math.ceil

val ColorBuffer.resolution get() = IntVector2(width, height)

/**
 * Computes dimensions needed to execute a compute shader when computing 2D data.
 *
 * @param resolution the resolution of the 2D data to compute.
 * @param localSizeX the `local_size_x` specified in the compute shader.
 * @param localSizeY the `local_size_y` specified in the compute shader.
 */
fun computeShaderExecuteDimensions(
    resolution: IntVector2,
    localSizeX: Int,
    localSizeY: Int
): IntVector3 = IntVector3(
    workGroupDimension(resolution.x, localSizeX),
    workGroupDimension(resolution.y, localSizeY),
    z = 1
)

/**
 * Appends given string after `#version x` statement.
 *
 * Useful for adding `#define` statements to existing shaders.
 *
 * @param string the string to append.
 */
fun String.appendAfterVersion(
    string: String
) = replace(versionRegex) {
    "${it.value}\n$string\n"
}

private fun workGroupDimension(
    size: Int,
    layout: Int
) = ceil(size.toDouble() / layout.toDouble()).toInt()

private val versionRegex = Regex("#version [0-9]+")

