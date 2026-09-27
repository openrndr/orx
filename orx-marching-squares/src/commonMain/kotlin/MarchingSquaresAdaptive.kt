package org.openrndr.extra.marchingsquares

import org.openrndr.shape.LineSegment
import org.openrndr.shape.Rectangle
import org.openrndr.shape.ShapeContour

/**
 * Finds contours for [shapeFunction] over an adaptive quadtree, like [findContoursAdaptive],
 * but resolves each straddling leaf independently with plain marching squares instead of
 * dual contouring -- there is no dual vertex, no [faceProc]-style stitching between cells,
 * just every leaf's own [crossingSegments] gathered up directly.
 *
 * The referenced article (https://www.mattkeeter.com/projects/contours/) motivates dual
 * contouring with two problems this naive approach has: cracks where differently-sized
 * cells meet, and bevelled corners. Only the second one actually shows up here: this file's
 * [buildQuadTree] collapses a cell to [QuadTree.isFull]/[QuadTree.isEmpty] only when all four
 * corners agree, so any straddling ([QuadTree.isLeaf]) cell is always driven all the way to
 * [maxDepth] -- meaning two straddling leaves are never adjacent at different sizes, and
 * there is nothing here for cracks to form between. (A more aggressive strategy that can
 * leave a straddling region coarse -- e.g. an error-tolerance-based one -- would reintroduce
 * exactly that crack problem, which is what makes dual contouring's cell-independent
 * stitching valuable in general, even though this specific tree-building strategy sidesteps
 * it.) Bevelling, on the other hand, is unavoidable here: a marching-squares crossing point
 * is always found along one of the cell's four sides, so it can only approximate a sharp
 * interior feature, never land on it -- unlike [feature]'s least-squares placement, which
 * can. Concretely, on a square with a sharp corner at depth 7, [findContoursAdaptive] lands
 * within `2e-5` of the true corner; this function is off by about `0.09`.
 *
 * @param shapeFunction the scalar field to contour; negative is "inside"
 * @param area the rectangular area in which [shapeFunction] is evaluated
 * @param maxDepth the maximum quadtree depth, i.e. the area is split into up to `2^maxDepth`
 * cells along each axis
 * @return a list of [ShapeContour] instances
 */
fun findContoursAdaptiveMarchingSquares(
    shapeFunction: ShapeFunction,
    area: Rectangle,
    maxDepth: Int
): List<ShapeContour> {
    val clamped = clampToBorder(shapeFunction, area)

    // buildQuadTreeNode halves width and height independently at every level, so a non-square
    // area would keep that same aspect ratio all the way down instead of ever reaching a
    // square cell -- building over the square, centered superset of area instead keeps cells
    // square at every depth. clamped already reads 0.0 (outside) beyond area's own bounds, so
    // the margin this can add on the shorter axis just collapses straight to empty.
    val side = maxOf(area.width, area.height)
    val squareArea = Rectangle.fromCenter(area.center, side, side)

    val tree = buildQuadTreeNode(clamped, squareArea, maxDepth)
    val segments = mutableListOf<LineSegment>()

    fun collect(node: QuadTree) {
        if (node.children.isEmpty()) {
            if (node.isLeaf) {
                segments.addAll(crossingSegments(clamped, node.cell))
            }
        } else {
            for (child in node.children) {
                collect(child)
            }
        }
    }

    collect(tree)
    return assembleContours(segments)
}
