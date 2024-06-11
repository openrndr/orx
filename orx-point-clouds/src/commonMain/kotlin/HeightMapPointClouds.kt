package org.openrndr.extra.pointclouds

import org.openrndr.draw.*
import org.openrndr.draw.font.BufferAccess
import org.openrndr.extra.computeshaders.resolution
import org.openrndr.math.Vector2

/**
 * Generates organized point clouds out of height maps where the layout of `XY` coordinates is preserved
 * in the layout of points in the cloud, and the `Z` coordinate is extruded.
 */
class HeightMapToPointCloudGenerator(

    /**
     * Preserves the original proportions of the supplied height map image, centering the resulting point cloud
     * in point `[0, 0, 0]` and normalizing the width in the `-1..1` range (default).
     * When set to `false`, the `XY` coordinates of the resulting point cloud will be normalized in the
     * `0..1` range.
     */
    private val preserveProportions: Boolean = true,

    /**
     * How much height should be expressed on the `Z`-axis.
     */
    var heightScale: Double = 1.0
) {

    private val shader = ComputeShader.fromCode(
        code = pointclouds_height_map_to_point_cloud
            .maybePreserveProportions(preserveProportions),
        name = "height-map-to-point-cloud"
    )

    /**
     * Populates [VertexBuffer] with ordered point cloud data.
     *
     * Note: this function is intended for continuous writes of changing data to allocated point
     * cloud [VertexBuffer]. For one time generation shortcut see [generate].
     *
     * @param pointCloud the point cloud buffer to write to.
     * @param heightMap an image where the RED channel encodes height.
     * @see generate
     * @see pointCloudVertexBuffer
     */
    fun populate(
        pointCloud: VertexBuffer,
        heightMap: ColorBuffer
    ) {
        shader.setUniforms(
            pointCloud,
            heightMap,
            heightScale,
            preserveProportions
        )
        shader.execute2D(heightMap.resolution)
    }

    /**
     * Generates ordered point cloud and returns corresponding [VertexBuffer].
     *
     * @param heightMap an image where the RED channel encodes height.
     * @return the generated point fo
     * @see populate
     * @see pointCloudVertexBuffer
     */
    fun generate(
        heightMap: ColorBuffer,
    ): VertexBuffer = pointCloudVertexBuffer(
        heightMap.resolution
    ).also {
        populate(it, heightMap)
    }

}

/**
 * Generates organized point clouds out of height maps and color information,
 * where the layout of `XY` coordinates is preserved in the layout of points in the cloud,
 * and additional `Z` coordinate is extruded.
 */
class ColoredHeightMapToPointCloudGenerator(

    /**
     * Preserves the original proportions of the supplied height map image, centering the resulting point cloud
     * in point `[0, 0, 0]` and normalizing the width in the `-1..1` range (default).
     * When set to `false`, the `XY` coordinates of the resulting point cloud will be normalized in the
     * `0..1` range.
     */
    val preserveProportions: Boolean = true,

    /**
     * How much height should be expressed on the `Z`-axis.
     */
    var heightScale: Double = 1.0
) {

    private val shader = ComputeShader.fromCode(
        code = pointclouds_colored_height_map_to_point_cloud
            .maybePreserveProportions(preserveProportions),
        name = "colored-height-map-to-point-cloud"
    )

    fun populate(
        pointCloud: VertexBuffer,
        heightMap: ColorBuffer,
        colors: ColorBuffer,
    ) {
        shader.setUniforms(
            pointCloud,
            heightMap,
            heightScale,
            preserveProportions
        )
        shader.image(
            "colors",
            1,
            colors.imageBinding(imageAccess = ImageAccess.READ)
        )
        shader.execute2D(heightMap.resolution)
    }

    fun generate(
        heightMap: ColorBuffer,
        colors: ColorBuffer,
    ): VertexBuffer = coloredPointCloudVertexBuffer(
        heightMap.resolution
    ).also {
        populate(it, heightMap, colors)
    }

}

private fun ComputeShader.setUniforms(
    pointCloud: VertexBuffer,
    heightMap: ColorBuffer,
    heightScale: Double,
    preserveProportions: Boolean
) {
    val resolution = heightMap.resolution
    val floatResolution = heightMap.resolution.vector2
    val scale = Vector2(1.0, floatResolution.y / floatResolution.x)
    val offset = Vector2(-.5, -scale.y * .5)
    uniform("resolution", resolution)
    uniform("floatResolution", floatResolution)
    uniform("heightScale", heightScale)
    if (preserveProportions) {
        uniform("scale", scale)
        uniform("offset", offset)
    }
    image(
        "heightMap",
        0,
        heightMap.imageBinding(imageAccess = BufferAccess.READ)
    )
    buffer("pointCloud", pointCloud)
}
