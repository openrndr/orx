package org.openrndr.extra.objloader

import org.openrndr.math.Vector2
import org.openrndr.math.Vector3

class VertexData(
    val positions: Array<Vector3> = emptyArray(),
    val normals: Array<Vector3> = emptyArray(),
    val textureCoords: Array<Vector2> = emptyArray()
)