package org.openrndr.extra.wireframes

import org.openrndr.draw.ComputeShader
import org.openrndr.extra.computeshaders.computeShaderExecuteDimensions
import org.openrndr.math.IntVector2

internal fun ComputeShader.execute2D(resolution: IntVector2) {
    execute(
        computeShaderExecuteDimensions(
            resolution,
            localSizeX = 8,
            localSizeY = 8
        )
    )
}
