package org.openrndr.extra.force2d
import org.openrndr.math.Vector2
import org.openrndr.shape.ShapeContour


/**
 * Converts a given shape contour into a physical body representation with configurable nodes and links.
 *
 * The function creates a series of nodes based on equidistant positions along the contour
 * and connects them using links to form a flexible structure. Additional neighbor links can
 * be added for increased connectivity. The resulting [Body] can be further configured
 * using the provided [configure] block.
 *
 * @param contour the shape contour to be converted into a physical body representation.
 * @param density the density of nodes along the contour, defining the spacing between nodes. Default is 10.0.
 * @param linkNeighbors the number of additional neighboring links to create for each node. Default is 1.
 * @param configure a lambda to configure the resulting [Body] instance.
 * @return a [Body] instance representing the given contour with nodes and links.
 */
fun contourToBody(contour: ShapeContour, density: Double = 10.0, linkNeighbors: Int = 1, configure: Body.() -> Unit = {}): Body {
    val points = contour.equidistantPositions((contour.length / density).toInt())

    val nodes = points.map {
        Node(it, it, Vector2.ZERO, 1.0)
    }

    val boundaryNodes = nodes.indices.toList()

    val links = nodes.indices.map {
        Link(it, (it+1).mod(points.size))
    }

    val extraLinks = nodes.indices.flatMap {
        @Suppress("EmptyRange")
        // we actually want an empty range when linkNeighbors < 2
        (2 .. linkNeighbors).map { n ->
            Link(it, (it+n).mod(points.size))
        }
    }

    val boundaryLinks = links.indices.toList()

    return Body(nodes, boundaryNodes, links + extraLinks, boundaryLinks).apply {
        configure(this)
    }
}
