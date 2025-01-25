package org.openrndr.extra.mesh.noise

import org.openrndr.extra.mesh.IIndexedPolygon
import org.openrndr.extra.mesh.IVertexData

/**
 * A type alias representing a weight function for vertices in a polygon.
 *
 * The function calculates the weight of a vertex within a given polygon based on its vertex data,
 * the polygon's geometry, and the specific vertex index.
 *
 * @param vertexData The vertex data containing attributes such as positions, normals, and other properties.
 * @param polygon The indexed polygon for which the weight is being calculated.
 * @param vertexIndex The index of the vertex within the polygon for which the weight is being computed.
 * @return The computed weight as a Double.
 */
typealias WeightFunction = (vertexData: IVertexData, polygon: IIndexedPolygon, vertexIndex: Int) -> Double

/**
 * A constant weight function for barycentric coordinates that always returns a weight of 1.0.
 *
 * This function can be used in scenarios where uniform weighting is required across
 * all components of a barycentric coordinate, effectively resulting in no modification
 * to the original weights of the components.
 */
val identityWeightFunction: WeightFunction = { _, _, _ -> 1.0 }