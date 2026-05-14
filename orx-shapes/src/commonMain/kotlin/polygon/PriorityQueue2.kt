/*
 * Copyright (c) 2003, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package org.openrndr.extra.shapes.polygon

import kotlin.jvm.JvmOverloads
import kotlin.jvm.Transient
import kotlin.math.max

/**
 * An unbounded priority [queue][Queue] based on a priority heap.
 * The elements of the priority queue are ordered according to their
 * [natural ordering][Comparable], or by a [Comparator]
 * provided at queue construction time, depending on which constructor is
 * used.  A priority queue does not permit `null` elements.
 * A priority queue relying on natural ordering also does not permit
 * insertion of non-comparable objects (doing so may result in
 * `ClassCastException`).
 *
 *
 * The *head* of this queue is the *least* element
 * with respect to the specified ordering.  If multiple elements are
 * tied for least value, the head is one of those elements -- ties are
 * broken arbitrarily.  The queue retrieval operations `poll`,
 * `remove`, `peek`, and `element` access the
 * element at the head of the queue.
 *
 *
 * A priority queue is unbounded, but has an internal
 * *capacity* governing the size of an array used to store the
 * elements on the queue.  It is always at least as large as the queue
 * size.  As elements are added to a priority queue, its capacity
 * grows automatically.  The details of the growth policy are not
 * specified.
 *
 *
 * This class and its iterator implement all of the
 * *optional* methods of the [Collection] and [ ] interfaces.  The Iterator provided in method [ ][.iterator] and the Spliterator provided in method [.spliterator]
 * are *not* guaranteed to traverse the elements of
 * the priority queue in any particular order. If you need ordered
 * traversal, consider using `Arrays.sort(pq.toArray())`.
 *
 *
 * **Note that this implementation is not synchronized.**
 * Multiple threads should not access a `PriorityQueue`
 * instance concurrently if any of the threads modifies the queue.
 * Instead, use the thread-safe [ ] class.
 *
 *
 * Implementation note: this implementation provides
 * O(log(n)) time for the enqueuing and dequeuing methods
 * (`offer`, `poll`, `remove()` and `add`);
 * linear time for the `remove(Object)` and `contains(Object)`
 * methods; and constant time for the retrieval methods
 * (`peek`, `element`, and `size`).
 *
 *
 * This class is a member of the
 * [
 * Java Collections Framework]({@docRoot}/java.base/java/util/package-summary.html#CollectionsFramework).
 *
 * @since 1.5
 * @author Josh Bloch, Doug Lea
 * @param <E> the type of elements held in this queue
</E> */
class PriorityQueue<E>  {
    /**
     * Priority queue represented as a balanced binary heap: the two
     * children of queue[n] are queue[2*n+1] and queue[2*(n+1)].  The
     * priority queue is ordered by comparator, or by the elements'
     * natural ordering, if comparator is null: For each node n in the
     * heap and each descendant d of n, n <= d.  The element with the
     * lowest value is in queue[0], assuming the queue is nonempty.
     */
    @Transient
    lateinit var queue: Array<Any?> // non-private to simplify nested class access

    /**
     * The number of elements in the priority queue.
     */
    var size: Int = 0

    /**
     * The comparator, or null if priority queue uses elements'
     * natural ordering.
     */
    private val comparator: Comparator<in E?>?

    /**
     * The number of times this priority queue has been
     * *structurally modified*.  See AbstractList for gory details.
     */
    @Transient
    var modCount: Int = 0 // non-private to simplify nested class access

    /**
     * Creates a `PriorityQueue` with the default initial capacity and
     * whose elements are ordered according to the specified comparator.
     *
     * @param  comparator the comparator that will be used to order this
     * priority queue.  If `null`, the [         natural ordering][Comparable] of the elements will be used.
     * @since 1.8
     */
    constructor(comparator: Comparator<in E?>?) : this(DEFAULT_INITIAL_CAPACITY, comparator)

    /**
     * Creates a `PriorityQueue` with the specified initial capacity
     * that orders its elements according to the specified comparator.
     *
     * @param  initialCapacity the initial capacity for this priority queue
     * @param  comparator the comparator that will be used to order this
     * priority queue.  If `null`, the [         natural ordering][Comparable] of the elements will be used.
     * @throws IllegalArgumentException if `initialCapacity` is
     * less than 1
     */
    /**
     * Creates a `PriorityQueue` with the default initial
     * capacity (11) that orders its elements according to their
     * [natural ordering][Comparable].
     */
    /**
     * Creates a `PriorityQueue` with the specified initial
     * capacity that orders its elements according to their
     * [natural ordering][Comparable].
     *
     * @param initialCapacity the initial capacity for this priority queue
     * @throws IllegalArgumentException if `initialCapacity` is less
     * than 1
     */
    @JvmOverloads
    constructor(
        initialCapacity: Int = DEFAULT_INITIAL_CAPACITY,
        comparator: Comparator<in E?>? = null
    ) {
        // Note: This restriction of at least one is not actually needed,
        // but continues for 1.5 compatibility
        require(initialCapacity >= 1)
        this.queue = arrayOfNulls<Any>(initialCapacity)
        this.comparator = comparator
    }




    /**
     * Increases the capacity of the array.
     *
     * @param minCapacity the desired minimum capacity
     */
    private fun grow(minCapacity: Int) {
        val oldCapacity = queue.size
        // Double size if small; else grow by 50%
        var newCapacity = oldCapacity + (if (oldCapacity < 64) (oldCapacity + 2) else (oldCapacity shr 1))
        // overflow-conscious code
        if (newCapacity - MAX_ARRAY_SIZE > 0) newCapacity = hugeCapacity(minCapacity)
        queue = queue.copyOf<Any?>(newCapacity)
    }

    /**
     * Inserts the specified element into this priority queue.
     *
     * @return `true` (as specified by [Collection.add])
     * @throws ClassCastException if the specified element cannot be
     * compared with elements currently in this priority queue
     * according to the priority queue's ordering
     * @throws NullPointerException if the specified element is null
     */
    fun add(e: E): Boolean {
        return offer(e)
    }

    /**
     * Inserts the specified element into this priority queue.
     *
     * @return `true` (as specified by [Queue.offer])
     * @throws ClassCastException if the specified element cannot be
     * compared with elements currently in this priority queue
     * according to the priority queue's ordering
     * @throws NullPointerException if the specified element is null
     */
     fun offer(e: E?): Boolean {
        if (e == null) throw NullPointerException()
        modCount++
        val i = size
        if (i >= queue.size) grow(i + 1)
        siftUp(i, e)
        size = i + 1
        return true
    }

    fun peek(): E? {
        return queue[0] as E?
    }

    private fun indexOf(o: Any?): Int {
        if (o != null) {
            val es = queue
            var i = 0
            val n = size
            while (i < n) {
                if (o == es[i]) return i
                i++
            }
        }
        return -1
    }

    /**
     * Removes a single instance of the specified element from this queue,
     * if it is present.  More formally, removes an element `e` such
     * that `o.equals(e)`, if this queue contains one or more such
     * elements.  Returns `true` if and only if this queue contained
     * the specified element (or equivalently, if this queue changed as a
     * result of the call).
     *
     * @param o element to be removed from this queue, if present
     * @return `true` if this queue changed as a result of the call
     */
     fun remove(o: Any?): Boolean {
        val i = indexOf(o)
        if (i == -1) return false
        else {
            removeAt(i)
            return true
        }
    }

    /**
     * Identity-based version for use in Itr.remove.
     *
     * @param o element to be removed from this queue, if present
     */
    fun removeEq(o: Any?) {
        val es = queue
        var i = 0
        val n = size
        while (i < n) {
            if (o === es[i]) {
                removeAt(i)
                break
            }
            i++
        }
    }

    /**
     * Returns `true` if this queue contains the specified element.
     * More formally, returns `true` if and only if this queue contains
     * at least one element `e` such that `o.equals(e)`.
     *
     * @param o object to be checked for containment in this queue
     * @return `true` if this queue contains the specified element
     */
    fun contains(o: Any?): Boolean {
        return indexOf(o) >= 0
    }




    fun size(): Int {
        return size
    }

    /**
     * Removes all of the elements from this priority queue.
     * The queue will be empty after this call returns.
     */
    fun clear() {
        modCount++
        val es = queue
        var i = 0
        val n = size
        while (i < n) {
            es[i] = null
            i++
        }
        size = 0
    }

    fun poll(): E? {
        val es: Array<Any?>?
        val result: E?

        if (((((queue.also { es = it })[0]) as E?).also { result = it }) != null) {
            modCount++
            val n: Int
            val x = es!![((--size).also { n = it })] as E?
            es[n] = null
            if (n > 0) {
                val cmp: Comparator<in E?>?
                if ((comparator.also { cmp = it }) == null) Companion.siftDownComparable<E?>(0, x, es, n)
                else Companion.siftDownUsingComparator<E?>(0, x, es, n, cmp!!)
            }
        }
        return result
    }

    /**
     * Removes the ith element from queue.
     *
     * Normally this method leaves the elements at up to i-1,
     * inclusive, untouched.  Under these circumstances, it returns
     * null.  Occasionally, in order to maintain the heap invariant,
     * it must swap a later element of the list with one earlier than
     * i.  Under these circumstances, this method returns the element
     * that was previously at the end of the list and is now at some
     * position before i. This fact is used by iterator.remove so as to
     * avoid missing traversing elements.
     */
    fun removeAt(i: Int): E? {
        // assert i >= 0 && i < size;
        val es = queue
        modCount++
        val s = --size
        if (s == i)  // removed last element
            es[i] = null
        else {
            val moved = es[s] as E?
            es[s] = null
            siftDown(i, moved)
            if (es[i] === moved) {
                siftUp(i, moved)
                if (es[i] !== moved) return moved
            }
        }
        return null
    }

    /**
     * Inserts item x at position k, maintaining heap invariant by
     * promoting x up the tree until it is greater than or equal to
     * its parent, or is the root.
     *
     * To simplify and speed up coercions and comparisons, the
     * Comparable and Comparator versions are separated into different
     * methods that are otherwise identical. (Similarly for siftDown.)
     *
     * @param k the position to fill
     * @param x the item to insert
     */
    private fun siftUp(k: Int, x: E?) {
        if (comparator != null) siftUpUsingComparator<E?>(k, x, queue, comparator)
        else siftUpComparable<E?>(k, x, queue)
    }

    /**
     * Inserts item x at position k, maintaining heap invariant by
     * demoting x down the tree repeatedly until it is less than or
     * equal to its children or is a leaf.
     *
     * @param k the position to fill
     * @param x the item to insert
     */
    private fun siftDown(k: Int, x: E?) {
        if (comparator != null) siftDownUsingComparator<E?>(k, x, queue, size, comparator)
        else siftDownComparable<E?>(k, x, queue, size)
    }

    /**
     * Establishes the heap invariant (described above) in the entire tree,
     * assuming nothing about the order of the elements prior to the call.
     * This classic algorithm due to Floyd (1964) is known to be O(size).
     */
    private fun heapify() {
        val es = queue
        val n = size
        var i = (n ushr 1) - 1
        val cmp: Comparator<in E?>?
        if ((comparator.also { cmp = it }) == null) while (i >= 0) {
            siftDownComparable<E?>(i, es[i] as E?, es, n)
            i--
        }
        else while (i >= 0) {
            siftDownUsingComparator<E?>(i, es[i] as E?, es, n, cmp!!)
            i--
        }
    }

    /**
     * Returns the comparator used to order the elements in this
     * queue, or `null` if this queue is sorted according to
     * the [natural ordering][Comparable] of its elements.
     *
     * @return the comparator used to order this queue, or
     * `null` if this queue is sorted according to the
     * natural ordering of its elements
     */
    fun comparator(): Comparator<in E?>? {
        return comparator
    }








    companion object {

        private const val DEFAULT_INITIAL_CAPACITY = 11


        /**
         * The maximum size of array to allocate.
         * Some VMs reserve some header words in an array.
         * Attempts to allocate larger arrays may result in
         * OutOfMemoryError: Requested array size exceeds VM limit
         */
        private val MAX_ARRAY_SIZE = Int.MAX_VALUE - 8

        private fun hugeCapacity(minCapacity: Int): Int {
            if (minCapacity < 0)  // overflow
                throw RuntimeException("Negative initial capacity: " + minCapacity)
            return if (minCapacity > MAX_ARRAY_SIZE) Int.MAX_VALUE else MAX_ARRAY_SIZE
        }

        private fun <T> siftUpComparable(k: Int, x: T?, es: Array<Any?>) {
            var k = k
            val key = x as Comparable<in T?>
            while (k > 0) {
                val parent = (k - 1) ushr 1
                val e: Any? = es[parent]
                if (key.compareTo(e as T?) >= 0) break
                es[k] = e!!
                k = parent
            }
            es[k] = key
        }

        private fun <T> siftUpUsingComparator(
            k: Int, x: T?, es: Array<Any?>, cmp: Comparator<in T?>
        ) {
            var k = k
            while (k > 0) {
                val parent = (k - 1) ushr 1
                val e: Any? = es[parent]
                if (cmp.compare(x, e as T?) >= 0) break
                es[k] = e!!
                k = parent
            }
            es[k] = x!!
        }

        private fun <T> siftDownComparable(k: Int, x: T?, es: Array<Any?>, n: Int) {
            // assert n > 0;
            var k = k
            val key = x as Comparable<in T?>
            val half = n ushr 1 // loop while a non-leaf
            while (k < half) {
                var child = (k shl 1) + 1 // assume left child is least
                var c = es[child]
                val right = child + 1
                if (right < n &&
                    (c as Comparable<in T?>).compareTo(es[right] as T?) > 0
                ) c = es[right.also { child = it }]
                if (key.compareTo(c as T?) <= 0) break
                es[k] = c
                k = child
            }
            es[k] = key
        }

        private fun <T> siftDownUsingComparator(
            k: Int, x: T?, es: Array<Any?>, n: Int, cmp: Comparator<in T?>
        ) {
            // assert n > 0;
            var k = k
            val half = n ushr 1
            while (k < half) {
                var child = (k shl 1) + 1
                var c: Any? = es[child]
                val right = child + 1
                if (right < n && cmp.compare(c as T?, es[right] as T?) > 0) c = es[right.also { child = it }]
                if (cmp.compare(x, c as T?) <= 0) break
                es[k] = c!!
                k = child
            }
            es[k] = x!!
        }

        // A tiny bit set implementation
        private fun nBits(n: Int): LongArray {
            return LongArray(((n - 1) shr 6) + 1)
        }

        private fun setBit(bits: LongArray, i: Int) {
            bits[i shr 6] = bits[i shr 6] or (1L shl i)
        }

        private fun isClear(bits: LongArray, i: Int): Boolean {
            return (bits[i shr 6] and (1L shl i)) == 0L
        }
    }
}