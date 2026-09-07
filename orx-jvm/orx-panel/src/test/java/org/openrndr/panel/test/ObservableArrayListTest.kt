package org.openrndr.panel.test

import org.openrndr.panel.collections.ObservableArrayList
import java.util.function.Predicate
import java.util.function.UnaryOperator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ObservableArrayListTest {
    /**
     * Wrapper class to simplify tests below
     */
    class ObservableArrayListTestHelper<E> : ObservableArrayList<E>() {
        var eventCount = 0
        var eventSource: ObservableArrayList<E>? = null

        init {
            changed.listen {
                eventCount++
                eventSource = it
            }
        }
    }

    @Test
    fun testAdd() {
        val list = ObservableArrayListTestHelper<String>()
        val result = list.add("item1")
        assertTrue(result)
        assertEquals(1, list.size)
        assertEquals("item1", list[0])
        assertEquals(1, list.eventCount)
        assertEquals(list, list.eventSource)
    }

    @Test
    fun testAddAtIndex() {
        val list = ObservableArrayListTestHelper<String>()
        list.add("a")
        list.add("c")
        list.add(1, "b")
        assertEquals(3, list.size)
        assertEquals(listOf("a", "b", "c"), list)
        assertEquals(3, list.eventCount)
        assertEquals(list, list.eventSource)
    }

    @Test
    fun testAddAll() {
        val list = ObservableArrayListTestHelper<String>()
        val added = list.addAll(listOf("a", "b", "c"))
        assertTrue(added)
        assertEquals(3, list.size)
        assertEquals(listOf("a", "b", "c"), list)
        assertEquals(1, list.eventCount)
        assertEquals(list, list.eventSource)

        // Adding empty collection returns false and does not trigger changed event
        val addedEmpty = list.addAll(emptyList())
        assertFalse(addedEmpty)
        assertEquals(1, list.eventCount)
    }

    @Test
    fun testAddAllAtIndex() {
        val list = ObservableArrayListTestHelper<String>()
        list.add("a")
        list.add("d")

        val added = list.addAll(1, listOf("b", "c"))
        assertTrue(added)
        assertEquals(listOf("a", "b", "c", "d"), list)
        assertEquals(3, list.eventCount)
        assertEquals(list, list.eventSource)

        // Adding empty collection at index returns false and does not trigger changed event
        val addedEmpty = list.addAll(1, emptyList())
        assertFalse(addedEmpty)
        assertEquals(3, list.eventCount)
    }

    @Test
    fun testClear() {
        val list = ObservableArrayListTestHelper<String>()
        list.addAll(listOf("a", "b", "c"))
        list.clear()
        assertTrue(list.isEmpty())
        assertEquals(2, list.eventCount)
        assertEquals(list, list.eventSource)
    }

    @Test
    fun testRemove() {
        val list = ObservableArrayListTestHelper<String>()
        list.addAll(listOf("a", "b", "c"))

        val removed = list.remove("b")
        assertTrue(removed)
        assertEquals(listOf("a", "c"), list)
        assertEquals(2, list.eventCount)
        assertEquals(list, list.eventSource)

        val removedNonExisting = list.remove("z")
        assertFalse(removedNonExisting)
        assertEquals(2, list.eventCount)
    }

    @Test
    fun testRemoveAll() {
        val list = ObservableArrayListTestHelper<String>()
        list.addAll(listOf("a", "b", "c", "d"))

        val removed = list.removeAll(listOf("b", "d"))
        assertTrue(removed)
        assertEquals(listOf("a", "c"), list)
        assertEquals(2, list.eventCount)
        assertEquals(list, list.eventSource)

        val removedNonExisting = list.removeAll(listOf("x", "y"))
        assertFalse(removedNonExisting)
        assertEquals(2, list.eventCount)
    }

    @Test
    fun testRemoveAt() {
        val list = ObservableArrayListTestHelper<String>()
        list.addAll(listOf("a", "b", "c"))
        val removedElement = list.removeAt(1)
        assertEquals("b", removedElement)
        assertEquals(listOf("a", "c"), list)
        assertEquals(2, list.eventCount)
        assertEquals(list, list.eventSource)
    }

    @Test
    fun testRemoveIf() {
        val list = ObservableArrayListTestHelper<String>()
        list.addAll(listOf("a1", "b", "a2", "c"))

        val removed = list.removeIf(Predicate { it.startsWith("a") })
        assertTrue(removed)
        assertEquals(listOf("b", "c"), list)
        assertEquals(2, list.eventCount)
        assertEquals(list, list.eventSource)

        val removedNone = list.removeIf(Predicate { it.startsWith("z") })
        assertFalse(removedNone)
        assertEquals(2, list.eventCount)
    }

    @Test
    fun testRetainAll() {
        val list = ObservableArrayListTestHelper<String>()
        list.addAll(listOf("a", "b", "c", "d"))

        val retained = list.retainAll(listOf("a", "c", "z"))
        assertTrue(retained)
        assertEquals(listOf("a", "c"), list)
        assertEquals(2, list.eventCount)
        assertEquals(list, list.eventSource)

        val retainSame = list.retainAll(listOf("a", "c"))
        assertFalse(retainSame)
        assertEquals(2, list.eventCount)
    }

    @Test
    fun testReplaceAll() {
        val list = ObservableArrayListTestHelper<String>()
        list.addAll(listOf("a", "b", "c"))

        list.replaceAll(UnaryOperator { it.uppercase() })
        assertEquals(listOf("A", "B", "C"), list)
        assertEquals(2, list.eventCount)
        assertEquals(list, list.eventSource)
    }

    @Test
    fun testSort() {
        val list = ObservableArrayListTestHelper<Int>()
        list.addAll(listOf(3, 1, 4, 1, 5, 9, 2, 6))

        list.sortDescending()
        assertEquals(listOf(9, 6, 5, 4, 3, 2, 1, 1), list)
        assertEquals(2, list.eventCount)
        assertEquals(list, list.eventSource)

        list.sort()
        assertEquals(listOf(1, 1, 2, 3, 4, 5, 6, 9), list)
        assertEquals(3, list.eventCount)
    }

    @Test
    fun testSet() {
        val list = ObservableArrayListTestHelper<String>()
        list.addAll(listOf("a", "b", "c"))

        val previous = list.set(1, "replaced")
        assertEquals("b", previous)
        assertEquals(listOf("a", "replaced", "c"), list)
        assertEquals(2, list.eventCount)
        assertEquals(list, list.eventSource)
    }

    @Test
    fun testClose() {
        val list = ObservableArrayListTestHelper<String>()
        list.addAll(listOf("a", "b", "c"))

        list.close()
        assertTrue(list.isEmpty())
        // Closing should not emit events
        assertEquals(1, list.eventCount)

        // Modifying list after close should not trigger events because event is closed
        list.add("d")
        assertEquals(1, list.eventCount)
    }
}

