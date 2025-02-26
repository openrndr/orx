package org.openrndr.extra.shapes.primitives

import org.openrndr.math.Vector3
import org.openrndr.shape.Box

fun Box.place(item: Box, anchor: Vector3 = Vector3(0.5, 0.5, 0.5), itemAnchor: Vector3 = anchor): Box {
    return Box(
        corner = corner + (dimensions * anchor - item.dimensions * itemAnchor),
        width = item.width,
        height = item.height,
        depth = item.depth
    )
}