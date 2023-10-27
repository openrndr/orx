package org.openrndr.extra.shapes.adjust

import org.openrndr.collections.pop
import org.openrndr.extra.shapes.vertex.ContourVertex
import org.openrndr.shape.ShapeContour


/**
 * Adjusts [ShapeContour] using an accessible interface.
 *
 * [ContourAdjuster]
 */
class ContourAdjuster(var contour: ShapeContour) {
    data class Parameters(
        var selectInsertedEdges: Boolean = false,
        var selectInsertedVertices: Boolean = false,
        var clearSelectedEdges: Boolean = false,
        var clearSelectedVertices: Boolean = false
    )
    var parameters = Parameters()

    val parameterStack = ArrayDeque<Parameters>()

    fun pushParameters() {
        parameterStack.addLast(parameters.copy())
    }

    fun popParameters() {
        parameters = parameterStack.pop()
    }

    /**
     * selected vertex indices
     */
    var vertexSelection = listOf(0)

    /**
     * selected edge indices
     */
    var edgeSelection = listOf(0)

    private var vertexWorkingSet = emptyList<Int>()
    private var edgeWorkingSet = emptyList<Int>()

    private var vertexHead = emptyList<Int>()
    private var edgeHead = emptyList<Int>()


    private val vertexSelectionStack = ArrayDeque<List<Int>>()
    private val edgeSelectionStack = ArrayDeque<List<Int>>()

    fun pushVertexSelection() {
        vertexSelectionStack.addLast(vertexSelection)
    }

    fun popVertexSelection() {
        vertexSelection = vertexSelectionStack.removeLast()
    }

    fun pushEdgeSelection() {
        edgeSelectionStack.addLast(edgeSelection)
    }

    fun popEdgeSelection() {
        edgeSelection = edgeSelectionStack.removeLast()
    }

    fun pushSelection() {
        pushEdgeSelection()
        pushVertexSelection()
    }

    fun popSelection() {
        popEdgeSelection()
        popVertexSelection()
    }

    /**
     * the selected vertex
     */
    val vertex: ContourAdjusterVertex
        get() {
            return vertices.first()
        }

    val vertices: Sequence<ContourAdjusterVertex>
        get() {
            vertexWorkingSet = vertexSelection
            applyBeforeAdjustment()
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
            return edges.first()
        }

    val edges: Sequence<ContourAdjusterEdge>
        get() {
            edgeWorkingSet = edgeSelection
            applyBeforeAdjustment()

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
        vertexSelection = listOf(index)
    }

    /**
     * deselect a vertex by index
     */
    fun deselectVertex(index: Int) {
        vertexSelection = vertexSelection.filter { it != index }
    }

    /**
     * select multiple vertices
     */
    fun selectVertices(vararg indices: Int) {
        vertexSelection = indices.toList().distinct()
    }

    /**
     * select multiple vertices using an index based [predicate]
     */
    fun selectVertices(predicate: (Int) -> Boolean) {
        vertexSelection =
            (0 until if (contour.closed) contour.segments.size else contour.segments.size + 1).filter(predicate)
    }

    /**
     * select multiple vertices using an index-vertex based [predicate]
     */
    fun selectVertices(predicate: (Int, ContourVertex) -> Boolean) {
        vertexSelection =
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
        edgeSelection = indices.toList().distinct()
    }

    /**
     * select multiple vertices using an index based [predicate]
     */
    fun selectEdges(predicate: (Int) -> Boolean) {
        edgeSelection =
            contour.segments.indices.filter(predicate)
    }

    /**
     * select multiple edges using an index-edge based [predicate]
     */
    fun selectEdges(predicate: (Int, ContourEdge) -> Boolean) {
        vertexSelection =
            (0 until if (contour.closed) contour.segments.size else contour.segments.size + 1).filter { index ->
                predicate(index, ContourEdge(contour, index))
            }
    }

    private fun applyBeforeAdjustment() {
        if (parameters.clearSelectedEdges) {
            edgeSelection = emptyList()
        }
        if (parameters.clearSelectedVertices) {
            vertexSelection = emptyList()
        }
    }

    fun updateSelection(adjustments: List<SegmentOperation>) {
        for (adjustment in adjustments) {
            when (adjustment) {
                is SegmentOperation.Insert -> {
                    fun insert(list: List<Int>, selectInserted: Boolean = false) =
                        if (!selectInserted) {
                            list.map {
                                if (it >= adjustment.index) {
                                    it + adjustment.amount
                                } else {
                                    it
                                }
                            }
                        } else {
                            (list.flatMap {
                                if (it >= adjustment.index) {
                                    listOf(it + adjustment.amount) + (it + 1..it + adjustment.amount)
                                } else {
                                    listOf(it)
                                }
                            } + (adjustment.index..<adjustment.index + adjustment.amount)).distinct()
                        }

                    for ((i, selection) in vertexSelectionStack.withIndex()) {
                        vertexSelectionStack[i] = insert(selection, false)
                    }
                    for ((i, selection) in edgeSelectionStack.withIndex()) {
                        edgeSelectionStack[i] = insert(selection, false)
                    }
                    vertexSelection = insert(vertexSelection, parameters.selectInsertedVertices)
                    edgeSelection = insert(edgeSelection, parameters.selectInsertedEdges)
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
                    for ((i, selection) in vertexSelectionStack.withIndex()) {
                        vertexSelectionStack[i] = remove(selection)
                    }
                    for ((i, selection) in edgeSelectionStack.withIndex()) {
                        edgeSelectionStack[i] = remove(selection)
                    }


                    vertexSelection = remove(vertexSelection)
                    edgeSelection = remove(edgeSelection)
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