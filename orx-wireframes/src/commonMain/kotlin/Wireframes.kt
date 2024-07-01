package org.openrndr.extra.wireframes

import org.openrndr.draw.*
import org.openrndr.extra.computeshaders.appendAfterVersion
import org.openrndr.math.IntVector2

/**
 * Generates wireframe out of supplied point cloud.
 */
class PointCloudToWireframeGenerator {

    private val shader = ComputeShader.fromCode(
        code = wireframes_point_cloud_to_wireframe,
        name = "point-cloud-to-wireframe"
    )

    fun populate(
        wireframe: VertexBuffer,
        pointCloud: VertexBuffer,
        resolution: IntVector2
    ) {
        shader.setUniforms(
            wireframe,
            pointCloud,
            resolution
        )
        shader.execute2D(resolution)
    }

    fun generate(
        pointCloud: VertexBuffer,
        resolution: IntVector2
    ): VertexBuffer = wireframeVertexBuffer(
        resolution
    ).also {
        populate(it, pointCloud, resolution)
    }

}

/**
 * Generates colored wireframe out of supplied colored point cloud.
 */
class ColoredPointCloudToWireframeGenerator {

    private val shader = ComputeShader.fromCode(
        code = wireframes_point_cloud_to_wireframe.appendAfterVersion(
            "#define COLORED"
        ),
        name = "colored-point-cloud-to-wireframe"
    )

    fun populate(
        wireframe: VertexBuffer,
        pointCloud: VertexBuffer,
        resolution: IntVector2
    ) {
        shader.setUniforms(
            wireframe,
            pointCloud,
            resolution
        )
        shader.execute2D(resolution)
    }

    fun generate(
        pointCloud: VertexBuffer,
        resolution: IntVector2
    ): VertexBuffer = coloredWireframeVertexBuffer(
        resolution
    ).also {
        populate(it, pointCloud, resolution)
    }

}

private fun ComputeShader.setUniforms(
    wireframe: VertexBuffer,
    pointCloud: VertexBuffer,
    resolution: IntVector2
) {
    uniform("resolution", resolution)
    uniform("resolutionMinus1", resolution - IntVector2(1, 1)) // TODO replace with IntVector2.ONE once it is in OPENRNDR
    buffer("pointCloud", pointCloud)
    buffer("wireframe", wireframe)
}
