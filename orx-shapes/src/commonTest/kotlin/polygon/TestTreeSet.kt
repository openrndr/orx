package org.openrndr.extra.shapes.polygon

import kotlin.test.*

class TestTreeSet {
    @Test
    fun testBasicOps() {
        val set = TreeSet<Int>()
        assertTrue(set.add(10))
        assertTrue(set.add(5))
        assertTrue(set.add(15))
        assertFalse(set.add(10))
        
        assertEquals(3, set.size)
        assertTrue(set.contains(5))
        assertTrue(set.contains(10))
        assertTrue(set.contains(15))
        assertFalse(set.contains(20))
        
        assertTrue(set.remove(10))
        assertFalse(set.remove(10))
        assertEquals(2, set.size)
        assertFalse(set.contains(10))
    }

    @Test
    fun testOrdering() {
        val set = TreeSet<Int>()
        val elements = listOf(50, 20, 70, 10, 30, 60, 80)
        elements.forEach { set.add(it) }
        
        assertEquals(elements.sorted(), set.toList())
    }

    @Test
    fun testNavigation() {
        val set = TreeSet<Int>()
        val elements = listOf(10, 20, 30, 40, 50)
        elements.forEach { set.add(it) }
        
        assertEquals(10, set.first())
        assertEquals(50, set.last())
        
        assertEquals(20, set.higher(10))
        assertEquals(10, set.lower(20))
        
        assertEquals(20, set.ceiling(15))
        assertEquals(10, set.floor(15))
        
        assertEquals(20, set.ceiling(20))
        assertEquals(20, set.floor(20))
        
        assertNull(set.lower(10))
        assertNull(set.higher(50))
    }

    @Test
    fun testDeletion() {
        val set = TreeSet<Int>()
        val elements = (1..20).toList().shuffled()
        elements.forEach { set.add(it) }

        val toRemove = elements.shuffled().take(10)
        toRemove.forEach { 
            assertTrue(set.remove(it), "Failed to remove $it")
            assertFalse(set.contains(it), "Set still contains $it after removal")
        }
        
        assertEquals(10, set.size)
        val remaining = elements.filter { it !in toRemove }.sorted()
        assertEquals(remaining, set.toList())
    }

    @Test
    fun testComparator() {
        val set = TreeSet<String>(compareByDescending { it })
        set.add("apple")
        set.add("banana")
        set.add("cherry")
        
        assertEquals(listOf("cherry", "banana", "apple"), set.toList())
    }
}
