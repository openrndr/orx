package org.openrndr.extra.force2d
import org.openrndr.math.Vector2
import org.openrndr.shape.ShapeContour

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
