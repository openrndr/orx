package org.openrndr.extra.shapes.primitives

import org.openrndr.math.Vector3
import org.openrndr.shape.Box

/**
 * Places a given box relative to this box using specified anchor points.
 * This method computes the position of the placed box based on the anchor points of both the current box
 * and the given box. The resulting box maintains the dimensions of the given box and is positioned
 * at the calculated location.
 *
 * @param item The box to be placed relative to this box.
 * @param anchor The anchor point on this box, specified as a [Vector3] where each component ranges from 0.0 to 1.0.
 *               The default is the center of this box (0.5, 0.5, 0.5).
 * @param itemAnchor The anchor point on the item being placed, specified as a [Vector3] where each component ranges
 *                   from 0.0 to 1.0. The default is the same as the `anchor` parameter.
 * @return A new [Box] representing the placed item with adjusted position and the same dimensions as the input item box.
 */
fun Box.place(item: Box, anchor: Vector3 = Vector3(0.5, 0.5, 0.5), itemAnchor: Vector3 = anchor): Box {
    return Box(
        corner = corner + (dimensions * anchor - item.dimensions * itemAnchor),
        width = item.width,
        height = item.height,
        depth = item.depth
    )
}

/**
 * Places this box inside the specified container box using anchor points
 * to determine the relative positioning.
 *
 * The placement is computed based on the anchor points specified for the container
 * and the item being placed. By default, the anchor points are set to the center
 * of the respective boxes. The dimensions of the placed box remain unchanged.
 *
 * @param container The box that will contain this box.
 * @param anchor The anchor point on the container, defined as a [Vector3] where each
 *               component ranges from 0.0 to 1.0. The default is the center of the container (0.5, 0.5, 0.5).
 * @param itemAnchor The anchor point on this box, defined as a [Vector3] where each component
 *                   ranges from 0.0 to 1.0. The default is the same as the `anchor` parameter.
 * @return A new [Box] representing this box placed inside the container at the calculated position.
 */
fun Box.placeIn(container: Box, anchor: Vector3 = Vector3(0.5, 0.5, 0.5), itemAnchor: Vector3 = anchor): Box {
    return container.place(this, anchor, itemAnchor)
}