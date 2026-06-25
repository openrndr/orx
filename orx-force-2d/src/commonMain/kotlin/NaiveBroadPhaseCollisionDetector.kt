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

/**
 * A naive implementation of a broad-phase collision detection algorithm.
 *
 * The `NaiveBroadPhaseCollisionDetector` is a concrete implementation of the
 * [BroadPhaseCollisionDetector] interface. It identifies pairs of potentially
 * overlapping bodies in a physics simulation by performing an exhaustive
 * comparison of their bounding volumes. This approach provides straightforward
 * and simple collision detection but does not scale efficiently with large
 * numbers of bodies due to its O(n^2) complexity.
 *
 * The method utilizes the [findOverlappingPairs] function, which directly
 * checks intersections between bounding rectangles of all input bodies.
 */
class NaiveBroadPhaseCollisionDetector: BroadPhaseCollisionDetector {
    override suspend fun findOverlappingPairs(bodies: List<Body>): List<Pair<Int, Int>> {
        return findOverlappingPairs(bodies.map { it.bounds })
    }
}

/**
 * Configures the force simulation to use the naive broad-phase collision detection algorithm.
 *
 * The `naiveBroadPhaseCollisionDetector` method sets the simulation's broad-phase collision
 * detection strategy to an instance of [NaiveBroadPhaseCollisionDetector]. This algorithm
 * identifies pairs of potentially overlapping bodies by performing an exhaustive comparison
 * of their bounding volumes. While simple and straightforward, this approach has O(n^2)
 * complexity and may not efficiently scale for simulations involving many bodies.
 *
 * This method directly assigns the `NaiveBroadPhaseCollisionDetector` instance to the
 * `broadPhaseCollisionDetector` property of the simulation.
 */
fun ForceSimulation.naiveBroadPhaseCollisionDetector() {
    broadPhaseCollisionDetector = NaiveBroadPhaseCollisionDetector()
}