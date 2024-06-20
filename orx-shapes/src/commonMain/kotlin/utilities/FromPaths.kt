package org.openrndr.extra.shapes.utilities

import org.openrndr.shape.Path3D
import org.openrndr.shape.path3D

/**
 * Create a [Path3D] from a list of paths
 */
fun Path3D.Companion.fromPaths(contours: List<Path3D>, closed: Boolean, connectEpsilon:Double=1E-6) : Path3D {
    @Suppress("NAME_SHADOWING") val contours = contours.filter { !it.empty }
    if (contours.isEmpty()) {
        return EMPTY
    }
    return path3D {
        moveTo(contours.first().position(0.0))
        for (c in contours.windowed(2,1,true)) {
            copy(c[0])
            if (c.size == 2) {
                val d = c[0].position(1.0).distanceTo(c[1].position(0.0))
                if (d > connectEpsilon ) {
                    lineTo(c[1].position(0.0))
                }
            }
        }
        if (closed) {
            close()
        }
    }
}