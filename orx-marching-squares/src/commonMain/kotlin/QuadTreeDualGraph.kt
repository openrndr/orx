package org.openrndr.extra.marchingsquares

import org.openrndr.math.Vector2

/**
 * Finds the dual graph of [this], as described in https://www.mattkeeter.com/projects/contours/:
 * one point per leaf cell (its center), and one polygon for every grid vertex shared by two
 * or more leaves -- a quad where four same-depth leaves meet, or a triangle at a "hanging"
 * T-junction where one coarser leaf borders two finer ones.
 *
 * This only depends on [this]'s shape, not on any [ShapeFunction]: every leaf cell -- whether
 * [QuadTree.isFull], [QuadTree.isEmpty] or a genuine boundary-straddling leaf -- contributes a
 * point and participates in polygons the same way.
 *
 * @return a list of points (one per leaf cell that borders at least one other, at its
 * center) and a list of polygons, each a list of indices into that point list, wound
 * counter-clockwise
 */
fun QuadTree.findDualGraph(): Pair<List<Vector2>, List<List<Int>>> {
    val points = mutableListOf<Vector2>()
    val indices = mutableMapOf<QuadTree, Int>()
    val polygons = mutableListOf<List<Int>>()

    fun indexOf(leaf: QuadTree): Int = indices.getOrPut(leaf) {
        points.add(leaf.cell.center)
        points.size - 1
    }

    // The single point shared by all of [nodes] (lower-left, lower-right, upper-left,
    // upper-right, in that order), narrowing into whichever ones are subdivided -- always
    // picking the child diagonally closest to that point -- until all four name one leaf.
    fun narrowToCorner(nodes: List<QuadTree>): List<QuadTree> {
        if (nodes.all { it.children.isEmpty() }) {
            return nodes
        }
        val (ll, lr, ul, ur) = nodes
        return narrowToCorner(
            listOf(
                if (ll.children.isEmpty()) ll else ll.children[3],
                if (lr.children.isEmpty()) lr else lr.children[2],
                if (ul.children.isEmpty()) ul else ul.children[1],
                if (ur.children.isEmpty()) ur else ur.children[0]
            )
        )
    }

    // Resolves the point shared by [ll]/[lr]/[ul]/[ur] down to the actual leaves touching
    // it (via [narrowToCorner], which handles either side being arbitrarily deeper than the
    // other) and returns them in cyclic (ll, lr, ur, ul) order. When one side of the point
    // didn't subdivide at all, the *same* node is passed in for two of the four roles (it
    // spans both), so after narrowing two cyclically adjacent entries can be the same leaf
    // -- a hanging T-junction -- which collapses into one vertex, yielding a triangle
    // instead of a quad.
    fun resolveCorner(ll: QuadTree, lr: QuadTree, ul: QuadTree, ur: QuadTree): List<QuadTree> {
        val (nll, nlr, nul, nur) = narrowToCorner(listOf(ll, lr, ul, ur))
        val cyclic = listOf(nll, nlr, nur, nul)
        val distinct = mutableListOf<QuadTree>()
        for (node in cyclic) {
            if (distinct.isEmpty() || distinct.last() != node) {
                distinct.add(node)
            }
        }
        if (distinct.size > 1 && distinct.first() == distinct.last()) {
            distinct.removeAt(distinct.size - 1)
        }
        return distinct
    }

    fun edgeH(left: QuadTree, right: QuadTree) {
        if (left.children.isEmpty() && right.children.isEmpty()) {
            // No subdivision on either side: no new grid vertex along this boundary.
            return
        }
        when {
            left.children.isEmpty() -> {
                val (a, _, c, _) = right.children
                edgeH(left, a)
                edgeH(left, c)
            }

            right.children.isEmpty() -> {
                val (_, b, _, d) = left.children
                edgeH(b, right)
                edgeH(d, right)
            }

            else -> {
                val (_, b, _, d) = left.children
                val (a, _, c, _) = right.children
                edgeH(b, a)
                edgeH(d, c)
            }
        }
        val ll = if (left.children.isEmpty()) left else left.children[1]
        val ul = if (left.children.isEmpty()) left else left.children[3]
        val lr = if (right.children.isEmpty()) right else right.children[0]
        val ur = if (right.children.isEmpty()) right else right.children[2]
        val face = resolveCorner(ll, lr, ul, ur)
        if (face.size >= 3) {
            polygons.add(face.map(::indexOf))
        }
    }

    fun edgeV(lower: QuadTree, upper: QuadTree) {
        if (lower.children.isEmpty() && upper.children.isEmpty()) {
            // No subdivision on either side: no new grid vertex along this boundary.
            return
        }
        when {
            upper.children.isEmpty() -> {
                val (_, _, c, d) = lower.children
                edgeV(c, upper)
                edgeV(d, upper)
            }

            lower.children.isEmpty() -> {
                val (a, b, _, _) = upper.children
                edgeV(lower, a)
                edgeV(lower, b)
            }

            else -> {
                val (_, _, c, d) = lower.children
                val (a, b, _, _) = upper.children
                edgeV(c, a)
                edgeV(d, b)
            }
        }
        val ll = if (lower.children.isEmpty()) lower else lower.children[2]
        val lr = if (lower.children.isEmpty()) lower else lower.children[3]
        val ul = if (upper.children.isEmpty()) upper else upper.children[0]
        val ur = if (upper.children.isEmpty()) upper else upper.children[1]
        val face = resolveCorner(ll, lr, ul, ur)
        if (face.size >= 3) {
            polygons.add(face.map(::indexOf))
        }
    }

    fun cell(node: QuadTree) {
        if (node.children.isEmpty()) {
            return
        }
        val (a, b, c, d) = node.children
        cell(a)
        cell(b)
        cell(c)
        cell(d)
        edgeH(a, b)
        edgeH(c, d)
        edgeV(a, c)
        edgeV(b, d)
        polygons.add(resolveCorner(a, b, c, d).map(::indexOf))
    }

    cell(this)
    return points to polygons
}

