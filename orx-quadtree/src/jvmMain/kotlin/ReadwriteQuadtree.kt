package org.openrndr.extra.quadtree

import org.openrndr.math.Vector2
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * Wraps a quadtree with a ReentrantReadWriteLock, which allows multiple concurrent
 * readers or one writer at a time.
 */
class ReadwriteQuadtree<T>(val qt: Quadtree<T>) : IQuadtree<T> {
    val lock = ReentrantReadWriteLock()

    override fun clear() {
        lock.write {
            qt.clear()
        }
    }

    override fun nearestToPoint(point: Vector2, radius: Double): QuadtreeQuery<T>? {
        lock.read {
            return qt.nearestToPoint(point, radius)
        }
    }

    override fun nearest(element: T, radius: Double): QuadtreeQuery<T>? {
        lock.read {
            return qt.nearest(element, radius)
        }
    }

    override fun insert(element: T): Boolean {
        lock.write {
            return qt.insert(element)
        }
    }

    override fun findNode(element: T): Quadtree<T>? {
        lock.read {
            return qt.findNode(element)
        }
    }
}