package org.openrndr.extra.meshgenerators

import org.openrndr.extra.shapes.loft.Loft
import org.openrndr.extra.shapes.loft.LoftVertex

/**
 * Writes the triangles of [loft]: its side on a grid of [along] steps along the path and [around] steps around the
 * cross-section, and with [caps] its ends, when it has them: a closed cross-section along a path that is not closed
 * makes a closed solid. Normals point out of the loft, triangles are wound counterclockwise seen from outside.
 * Texture coordinates are (u, v) on the side, and the bounds of the cross-section on the caps. With [sharpCorners] the
 * corners of the cross-section, its [Loft.corners], are sharp edges; see [Loft.side].
 */
fun TriangleMeshBuilder.loft(loft: Loft, along: Int, around: Int, caps: Boolean = true, sharpCorners: Boolean = false) {
    fun write(vertices: List<LoftVertex>) {
        for (v in vertices) write(v.position, v.normal, v.texCoord)
    }
    write(loft.side(along, around, sharpCorners))
    if (caps && loft.hasCaps) {
        write(loft.cap(end = false, around = around, sharpCorners = sharpCorners))
        write(loft.cap(end = true, around = around, sharpCorners = sharpCorners))
    }
}
