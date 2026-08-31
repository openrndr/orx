package org.openrndr.extra.bvh.sap

import org.openrndr.shape.Rectangle

class BoxedPointOrEdge(val bounds: Rectangle, val index: Int, val type: Int, val listIndex: Int)

fun sortAndSweepBipartite(
    points: List<BoxedPointOrEdge>,
    edges: List<BoxedPointOrEdge>, onIntersect: (Int, Int) -> Unit
) {


    val tagged = (points + edges).sortedBy { it.bounds.x }

    val activePoints = mutableListOf<BoxedPointOrEdge>()
    val activeEdges = mutableListOf<BoxedPointOrEdge>()

    for (body in tagged) {
        activePoints.removeAll { (it.bounds.x + it.bounds.width) < body.bounds.x }
        activeEdges.removeAll { (it.bounds.x + it.bounds.width) < body.bounds.x }

        if (body.type == 0) {
            for (edge in activeEdges) {
                if (body.bounds.intersects(edge.bounds)) {
                    onIntersect(body.index, edge.index)
                }
            }
            activePoints.add(body)
        } else {
            for (point in activePoints) {
                if (point.bounds.intersects(body.bounds)) {
                    onIntersect(point.index, body.index)

                }
            }
            activeEdges.add(body)
        }
    }
}

fun findNearest(points: List<BoxedPointOrEdge>,
                edges: List<BoxedPointOrEdge>,
                distance: (Int, Int) -> Double,
                ) : Pair<IntArray, DoubleArray>
{
    val tagged = (points + edges).sortedBy { it.bounds.x }

    val nearestEdges = IntArray(points.size) { -1 }
    val nearestDistances = DoubleArray(points.size) { Double.POSITIVE_INFINITY }

    val activePoints = mutableListOf<BoxedPointOrEdge>()
    val activeEdges = mutableListOf<BoxedPointOrEdge>()

    for (body in tagged) {
        activePoints.removeAll { (it.bounds.x + it.bounds.width) < body.bounds.x }
        activeEdges.removeAll { (it.bounds.x + it.bounds.width) < body.bounds.x }

        if (body.type == 0) {
            for (edge in activeEdges) {
                if (body.bounds.intersects(edge.bounds)) {
                    val d = distance(body.index, edge.index)
                    if (d < nearestDistances[body.listIndex]) {
                        nearestDistances[body.listIndex] = d
                        nearestEdges[body.listIndex] = edge.index
                    }
                }
            }
            activePoints.add(body)
        } else {
            for (point in activePoints) {
                if (point.bounds.intersects(body.bounds)) {
                    val d = distance(point.index, body.index)
                    if (d < nearestDistances[point.listIndex]) {
                        nearestDistances[point.listIndex] = d
                        nearestEdges[point.listIndex] = body.index
                    }
                }
            }
            activeEdges.add(body)
        }
    }
    return nearestEdges to nearestDistances
}
