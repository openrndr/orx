package org.openrndr.extra.shapes.polygon

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TestPriorityQueue {
    @Test
    fun testBasicOps() {
        val pq = PriorityQueue<Int>()
        assertTrue(pq.isEmpty())
        assertEquals(0, pq.size)

        pq.add(5)
        pq.add(2)
        pq.add(8)
        pq.add(1)

        assertFalse(pq.isEmpty())
        assertEquals(4, pq.size)
        assertEquals(1, pq.peek())

        assertEquals(1, pq.poll())
        assertEquals(2, pq.poll())
        assertEquals(2, pq.size)
        assertEquals(5, pq.peek())
        assertEquals(5, pq.poll())
        assertEquals(8, pq.poll())
        assertTrue(pq.isEmpty())
    }

    @Test
    fun testDuplicates() {
        val pq = PriorityQueue<Int>()
        pq.add(5)
        pq.add(2)
        pq.add(5)
        pq.add(1)

        assertEquals(4, pq.size)
        assertEquals(1, pq.poll())
        assertEquals(2, pq.poll())
        assertEquals(5, pq.poll())
        assertEquals(5, pq.poll())
        assertTrue(pq.isEmpty())
    }

    @Test
    fun testComparator() {
        val pq = PriorityQueue<Int>(PriorityQueue.Comparator { a, b -> b.compareTo(a) })
        pq.add(5)
        pq.add(2)
        pq.add(8)
        pq.add(1)

        assertEquals(8, pq.poll())
        assertEquals(5, pq.poll())
        assertEquals(2, pq.poll())
        assertEquals(1, pq.poll())
        assertTrue(pq.isEmpty())
    }
}
