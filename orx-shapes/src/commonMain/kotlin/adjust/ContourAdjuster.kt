package org.openrndr.extra.shapes.adjust

import org.openrndr.extra.shapes.vertex.ContourVertex
import org.openrndr.shape.ShapeContour

/**
 * Adjusts [ShapeContour] using an accessible interface.
 *
 * [ContourAdjuster]
 */
class ContourAdjuster(var contour: ShapeContour) {
    /**
     * selected vertex indices
     */
    var vertexIndices = listOf(0)

    /**
     * selected edge indices
     */
    var edgeIndices = listOf(0)

    private var vertexWorkingSet = emptyList<Int>()
    private var edgeWorkingSet = emptyList<Int>()

    private var vertexHead = emptyList<Int>()
    private var edgeHead = emptyList<Int>()

    /**
     * the selected vertex
     */
    val vertex: ContourAdjusterVertex
        get() {
            return ContourAdjusterVertex(this, { vertexIndices.first() } )
        }

    val vertices: Sequence<ContourAdjusterVertex>
        get() {
            vertexWorkingSet = vertexIndices
            return sequence {
                while (vertexWorkingSet.isNotEmpty()) {
                    vertexHead = vertexWorkingSet.take(1)
                    vertexWorkingSet = vertexWorkingSet.drop(1)
                    yield(ContourAdjusterVertex(this@ContourAdjuster, { vertexHead.first() }))
                }
            }
        }


    /**
     * the selected edge
     */
    val edge: ContourAdjusterEdge
        get() {
            return ContourAdjusterEdge(this, { edgeIndices.first() })
        }

    val edges: Sequence<ContourAdjusterEdge>
        get() {
            edgeWorkingSet = edgeIndices
            return sequence {
                while (edgeWorkingSet.isNotEmpty()) {
                    edgeHead = edgeWorkingSet.take(1)
                    edgeWorkingSet = edgeWorkingSet.drop(1)
                    yield(ContourAdjusterEdge(this@ContourAdjuster, { edgeHead.first() }))
                }
            }
        }

    /**
     * select a vertex by index
     */
    fun selectVertex(index: Int) {
        vertexIndices = listOf(index)
    }

    /**
     * deselect a vertex by index
     */
    fun deselectVertex(index: Int) {
        vertexIndices = vertexIndices.filter { it != index }
    }

    /**
     * select multiple vertices
     */
    fun selectVertices(vararg indices: Int) {
        vertexIndices = indices.toList().distinct()
    }

    /**
     * select multiple vertices using an index based [predicate]
     */
    fun selectVertices(predicate: (Int) -> Boolean) {
        vertexIndices =
            (0 until if (contour.closed) contour.segments.size else contour.segments.size + 1).filter(predicate)
    }

    /**
     * select multiple vertices using an index-vertex based [predicate]
     */
    fun selectVertices(predicate: (Int, ContourVertex) -> Boolean) {
        vertexIndices =
            (0 until if (contour.closed) contour.segments.size else contour.segments.size + 1).filter { index ->
                predicate(index, ContourVertex(contour, index))
            }
    }

    /**
     * select an edge by index
     */
    fun selectEdge(index: Int) {
        selectEdges(index)
    }

    /**
     * select multiple edges by index
     */
    fun selectEdges(vararg indices: Int) {
        edgeIndices = indices.toList().distinct()
    }

    /**
     * select multiple vertices using an index based [predicate]
     */
    fun selectEdges(predicate: (Int) -> Boolean) {
        edgeIndices =
            contour.segments.indices.filter(predicate)
    }

    /**
     * select multiple edges using an index-edge based [predicate]
     */
    fun selectEdges(predicate: (Int, ContourEdge) -> Boolean) {
        vertexIndices =
            (0 until if (contour.closed) contour.segments.size else contour.segments.size + 1).filter { index ->
                predicate(index, ContourEdge(contour, index))
            }
    }

    fun updateSelection(adjustments: List<SegmentOperation>) {

        for (adjustment in adjustments) {
            when (adjustment) {
                is SegmentOperation.Insert -> {
                    fun insert(list: List<Int>) = list.map {
                        if (it >= adjustment.index) {
                            it + adjustment.amount
                        } else {
                            it
                        }
                    }
                    vertexIndices = insert(vertexIndices)
                    edgeIndices = insert(edgeIndices)
                    vertexWorkingSet = insert(vertexWorkingSet)
                    edgeWorkingSet = insert(edgeWorkingSet)
                }

                is SegmentOperation.Remove -> {

                    fun remove(list: List<Int>) = list.mapNotNull {
                        if (it in adjustment.index..<adjustment.index + adjustment.amount) {
                            null
                        } else if (it > adjustment.index) {
                            it - adjustment.amount
                        } else {
                            it
                        }
                    }
                    // TODO: handling of vertices in open contours is wrong here
                    vertexIndices = remove(vertexIndices)
                    edgeIndices = remove(edgeIndices)
                    vertexWorkingSet = remove(vertexWorkingSet)
                    edgeWorkingSet = remove(edgeWorkingSet)
                }
            }
        }
    }
}

/**
 * Build a contour adjuster
 */
fun adjustContour(contour: ShapeContour, adjuster: ContourAdjuster.() -> Unit): ShapeContour {
    val ca = ContourAdjuster(contour)
    ca.apply(adjuster)
    return ca.contour
}