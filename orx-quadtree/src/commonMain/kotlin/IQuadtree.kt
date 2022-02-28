package org.openrndr.extra.quadtree

import org.openrndr.math.Vector2

interface IQuadtree<T> {
    /**
     * Clears the whole tree
     */
    fun clear()

    /**
     * Finds the nearest and neighbouring objects within a radius
     * (needs to have a different name so there is no ambiguity when the generic object type is Vector2)
     *
     * @param element
     * @param radius
     * @return
     */
    fun nearestToPoint(point: Vector2, radius: Double): QuadtreeQuery<T>?

    /**
     * Finds the nearest and neighbouring points within a radius
     *
     * @param element
     * @param radius
     * @return
     */
    fun nearest(element: T, radius: Double): QuadtreeQuery<T>?

    /**
     * Inserts the element in the appropriate node
     *
     * @param element
     * @return
     */
    fun insert(element: T): Boolean

    /**
     * Remove the given element
     *
     * @param element
     * @return true if the element was present
     */
    fun remove(element: T): Boolean

    /**
     * Finds which node the element is within (but not necessarily belonging to)
     *
     * @param element
     * @return
     */
    fun findNode(element: T): Quadtree<T>?
}