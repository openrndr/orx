package org.openrndr.extra.shapes.primitives

import org.openrndr.shape.Box

/**
 * Creates a sub-box from the current box using the dimensions
 * defined by another box.
 *
 * @param uvw The box defining the dimensions (relative to this box)
 * to create the sub-box. Its position and size are used to compute the
 * resulting sub-box.
 */
fun Box.sub(uvw: Box) =
    sub(
        uvw.corner.x,
        uvw.corner.y,
        uvw.corner.z,
        uvw.corner.x + uvw.width,
        uvw.corner.y + uvw.height,
        uvw.corner.z + uvw.depth
    )