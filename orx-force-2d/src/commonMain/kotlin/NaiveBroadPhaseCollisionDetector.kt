package org.openrndr.extra.force2d

import org.openrndr.shape.Rectangle

fun findOverlappingPairs(rectangles: List<Rectangle>): List<Pair<Int, Int>> {

    val result = mutableListOf<Pair<Int, Int>>()
    for (j in rectangles.indices) {
        for (i in j + 1 until rectangles.size) {
            if (rectangles[j].intersects(rectangles[i])) {
                result.add(j to i)
            }
        }
    }
    return result

}

class NaiveBroadPhaseCollisionDetector: BroadPhaseCollisionDetector {
    override suspend fun findOverlappingPairs(bodies: List<Body>): List<Pair<Int, Int>> {
        return findOverlappingPairs(bodies.map { it.bounds })
    }
}

fun ForceSimulation.naiveBroadPhaseCollisionDetector() {
    broadPhaseCollisionDetector = NaiveBroadPhaseCollisionDetector()
}