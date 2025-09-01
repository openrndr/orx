package org.openrndr.extra.shapes.primitives

import org.openrndr.shape.Rectangle

/**
 * Creates a sub-rectangle from the current rectangle using the dimensions
 * defined by another rectangle.
 *
 * @param uv The rectangle defining the dimensions (relative to this rectangle)
 * to create the sub-rectangle. Its position and size are used to compute the
 * resulting sub-rectangle.
 */
fun Rectangle.sub(uv: Rectangle) = sub(uv.x, uv.y, uv.x + uv.width, uv.y + uv.height)