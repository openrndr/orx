package org.openrndr.extra.marchingsquares

import org.openrndr.math.Vector2
import org.openrndr.shape.LineSegment
import org.openrndr.shape.Rectangle


/**
 * A node in an adaptive quadtree over a [Rectangle], as described in
 * https://www.mattkeeter.com/projects/contours/
 *
 * A region is recursively split into four [children] -- lower-left, lower-right,
 * upper-left and upper-right, in that order -- down to some maximum depth, where subtrees
 * that are fully inside ([isFull]) or fully outside ([isEmpty]) the shape collapse into a
 * single childless node instead. A childless node that is neither [isFull] nor [isEmpty]
 * ([isLeaf]) is a cell that straddles the boundary.
 */
data class QuadTree(
    val cell: Rectangle,
    val children: List<QuadTree> = emptyList(),
    val isFull: Boolean = false,
    val isEmpty: Boolean = false
) {
    init {
        require(children.isEmpty() || children.size == 4) { "a QuadTree node has either 0 or 4 children" }
        require(!(isFull && isEmpty)) { "a QuadTree node cannot be both full and empty" }
        require(children.isEmpty() || !(isFull || isEmpty)) { "a subdivided QuadTree node cannot be full or empty" }
    }

    /** True if this node is a childless cell that straddles the shape's boundary. */
    val isLeaf: Boolean get() = children.isEmpty() && !isFull && !isEmpty

    companion object {

    }
}

/**
 * Builds an adaptive quadtree over [area] for [shapeFunction], down to [maxDepth],
 * collapsing every subtree that turns out to be fully inside or fully outside the shape
 * into a single [QuadTree.Full]/[QuadTree.Empty] node.
 *
 * The referenced article builds a full, uniform tree first and only collapses it in a
 * second pass, relying on Haskell's laziness to fuse the two at runtime so the full tree
 * is never actually resident in memory. This does that fusion directly and eagerly: each
 * subtree is collapsed as soon as its four children are known, so a uniform region is
 * discarded down to a single node right away instead of momentarily existing as up to
 * `4^maxDepth` separate leaf nodes.
 *
 * Note that this still evaluates [shapeFunction] at every leaf's corners everywhere in
 * [area]: there is no sound way to tell a coarse cell is uniform from just its own four
 * corners (a shape can hide entirely inside a cell without ever reaching a corner), so full
 * resolution still has to be sampled once before anything can collapse back up. What's
 * avoided is only ever materializing that full-resolution tree as a whole in memory.
 *
 * [shapeFunction] is evaluated through [clampToBorder], so a shape extending past [area]
 * still collapses to a tree whose boundary-straddling leaves close off along the edge of
 * [area] -- see [findContoursAdaptive].
 */
fun QuadTree.Companion.buildQuadTree(shapeFunction: ShapeFunction, area: Rectangle, maxDepth: Int): QuadTree =
    buildQuadTreeNode(clampToBorder(shapeFunction, area), area, maxDepth)

internal fun buildQuadTreeNode(shapeFunction: ShapeFunction, area: Rectangle, maxDepth: Int): QuadTree {
    if (maxDepth <= 0) {
        val values = corners(area).map { shapeFunction(it) }
        return when {
            values.all { it < 0.0 } -> QuadTree(area, isFull = true)
            values.all { it >= 0.0 } -> QuadTree(area, isEmpty = true)
            else -> QuadTree(area)
        }
    }
    val halfWidth = area.width * 0.5
    val halfHeight = area.height * 0.5
    val a = buildQuadTreeNode(shapeFunction, Rectangle(area.x, area.y, halfWidth, halfHeight), maxDepth - 1)
    val b = buildQuadTreeNode(
        shapeFunction,
        Rectangle(area.x + halfWidth, area.y, halfWidth, halfHeight),
        maxDepth - 1
    )
    val c = buildQuadTreeNode(
        shapeFunction,
        Rectangle(area.x, area.y + halfHeight, halfWidth, halfHeight),
        maxDepth - 1
    )
    val d = buildQuadTreeNode(
        shapeFunction,
        Rectangle(area.x + halfWidth, area.y + halfHeight, halfWidth, halfHeight),
        maxDepth - 1
    )
    return when {
        a.isEmpty && b.isEmpty && c.isEmpty && d.isEmpty -> QuadTree(area, isEmpty = true)
        a.isFull && b.isFull && c.isFull && d.isFull -> QuadTree(area, isFull = true)
        else -> QuadTree(area, children = listOf(a, b, c, d))
    }
}

/** The four corners of [cell], in lower-left, lower-right, upper-left, upper-right order. */
private fun corners(cell: Rectangle): List<Vector2> = listOf(
    Vector2(cell.x, cell.y),
    Vector2(cell.x + cell.width, cell.y),
    Vector2(cell.x, cell.y + cell.height),
    Vector2(cell.x + cell.width, cell.y + cell.height)
)

internal enum class Side { LEFT, RIGHT, LOWER, UPPER }

/**
 * Marching-squares case table: for each of the 16 corner sign combinations, the pairs of
 * cell sides that a contour segment crosses between (cases 6 and 9 are ambiguous saddles
 * and contribute two segments). Each pair is ordered so that walking from its first side's
 * crossing point to its second's keeps the inside of the shape consistently on the same
 * side, for every case -- this matters for [findContoursAdaptiveMarchingSquares], which
 * chains these segments into contours; it doesn't matter for [feature]'s use of this table,
 * which only collects crossing points regardless of order.
 */
private val lut: List<List<Pair<Side, Side>>> = listOf(
    listOf(),                                              // 0000
    listOf(Side.RIGHT to Side.UPPER),                      // 000d
    listOf(Side.UPPER to Side.LEFT),                       // 00c0
    listOf(Side.RIGHT to Side.LEFT),                       // 00cd
    listOf(Side.LOWER to Side.RIGHT),                      // 0b00
    listOf(Side.LOWER to Side.UPPER),                      // 0b0d
    listOf(Side.LOWER to Side.RIGHT, Side.UPPER to Side.LEFT), // 0bc0
    listOf(Side.LOWER to Side.LEFT),                       // 0bcd
    listOf(Side.LEFT to Side.LOWER),                       // a000
    listOf(Side.LEFT to Side.LOWER, Side.RIGHT to Side.UPPER), // a00d
    listOf(Side.UPPER to Side.LOWER),                      // a0c0
    listOf(Side.RIGHT to Side.LOWER),                      // a0cd
    listOf(Side.LEFT to Side.RIGHT),                       // ab00
    listOf(Side.LEFT to Side.UPPER),                       // ab0d
    listOf(Side.UPPER to Side.RIGHT),                      // abc0
    listOf()                                               // abcd
)

/** The marching-squares case index of [cell]: bit 3=lower-left, 2=lower-right, 1=upper-left, 0=upper-right. */
private fun cellIndex(shapeFunction: ShapeFunction, cell: Rectangle): Int {
    val c = corners(cell)
    var index = 0
    for (i in c.indices) {
        if (shapeFunction(c[i]) < 0.0) {
            index += 1 shl (3 - i)
        }
    }
    return index
}


/** The zero-crossing segments of [cell] found via the marching-squares case table. */
internal fun crossingSegments(shapeFunction: ShapeFunction, cell: Rectangle): List<LineSegment> =
    lut[cellIndex(shapeFunction, cell)].map { (s0, s1) ->
        LineSegment(sidePoint(shapeFunction, cell, s0), sidePoint(shapeFunction, cell, s1))
    }
