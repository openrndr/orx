package org.openrndr.extra.computeshaders

import org.openrndr.draw.ColorBuffer
import org.openrndr.math.IntVector2
import org.openrndr.math.IntVector3
import kotlin.math.ceil

val ColorBuffer.resolution get() = IntVector2(width, height)

fun computeShaderExecuteDimensionsFor2D(
    resolution: IntVector2,
    localSizeX: Int,
    localSizeY: Int
): IntVector3 = IntVector3(
    workGroupDimension(resolution.x, localSizeX),
    workGroupDimension(resolution.y, localSizeY),
    z = 1
)

private fun workGroupDimension(
    size: Int,
    layout: Int
) = ceil(size.toDouble() / layout.toDouble()).toInt()
