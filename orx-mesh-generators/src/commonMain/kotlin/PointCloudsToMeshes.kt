package org.openrndr.extra.meshgenerators

import org.openrndr.draw.*
import org.openrndr.extra.computeshaders.appendAfterVersion
import org.openrndr.extra.computeshaders.computeShaderExecuteDimensions
import org.openrndr.math.IntVector2

/**
 * Generates mesh out of supplied point cloud.
 */
class PointCloudToMeshGenerator {

    private val shader = ComputeShader.fromCode(
        code = meshgenerators_point_cloud_to_mesh,
        name = "point-cloud-to-mesh"
    )

    fun populate(
        mesh: VertexBuffer,
        pointCloud: VertexBuffer,
        resolution: IntVector2
    ) {
        shader.setUniforms(
            mesh,
            pointCloud,
            resolution
        )
        shader.execute2D(resolution)
    }

    fun generate(
        pointCloud: VertexBuffer,
        resolution: IntVector2
    ): VertexBuffer = meshVertexBuffer(
        resolution
    ).also {
        populate(it, pointCloud, resolution)
    }

}

/**
 * Generates colored mesh out of supplied colored point cloud.
 */
class ColoredPointCloudToMeshGenerator {

    private val shader = ComputeShader.fromCode(
        code = meshgenerators_point_cloud_to_mesh.appendAfterVersion(
            "#define COLORED"
        ),
        name = "colored-point-cloud-to-mesh"
    )

    fun populate(
        mesh: VertexBuffer,
        pointCloud: VertexBuffer,
        resolution: IntVector2
    ) {
        shader.setUniforms(
            mesh,
            pointCloud,
            resolution
        )
        shader.execute2D(resolution)
    }

    fun generate(
        pointCloud: VertexBuffer,
        resolution: IntVector2
    ): VertexBuffer = coloredMeshVertexBuffer(
        resolution
    ).also {
        populate(it, pointCloud, resolution)
    }

}

val meshVertexFormat: VertexFormat = vertexFormat {
    position(dimensions = 3)
    padding(4)
    normal(dimensions = 3)
    padding(4)
}

val coloredMeshVertexFormat: VertexFormat = vertexFormat {
    position(dimensions = 3)
    padding(4)
    normal(dimensions = 3)
    padding(4)
    color(dimensions = 4)
}

fun meshVertexBuffer(
    resolution: IntVector2
): VertexBuffer = vertexBuffer(
    meshVertexFormat,
    vertexCount = (resolution.x - 1) * (resolution.y - 1) * 6
)

fun coloredMeshVertexBuffer(
    resolution: IntVector2
): VertexBuffer = vertexBuffer(
    coloredMeshVertexFormat,
    vertexCount = (resolution.x - 1) * (resolution.y - 1) * 6
)

private fun ComputeShader.setUniforms(
    mesh: VertexBuffer,
    pointCloud: VertexBuffer,
    resolution: IntVector2
) {
    uniform("resolution", resolution)
    uniform("resolutionMinus1", resolution - IntVector2(1, 1)) // TODO replace with IntVector2.ONE once it is in OPENRNDR
    buffer("pointCloud", pointCloud)
    buffer("mesh", mesh)
}

private fun ComputeShader.execute2D(resolution: IntVector2) {
    execute(
        computeShaderExecuteDimensions(
            resolution,
            localSizeX = 8,
            localSizeY = 8
        )
    )
}
