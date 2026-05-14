//package org.openrndr.extra.shapes.polygon
//
//class PriorityQueue2<E>(val comparator: Comparator<in E>? = null) {
//    fun interface Comparator<T> {
//        fun compare(o1: T, o2: T): Int
//    }
//
//    private val treeSet = TreeSet<IndexedValue<E>>(object : kotlin.Comparator<IndexedValue<E>> {
//        override fun compare(o1: IndexedValue<E>, o2: IndexedValue<E>): Int {
//            val res = if (this@PriorityQueue.comparator != null) {
//                this@PriorityQueue.comparator.compare(o1.value, o2.value)
//            } else {
//                @Suppress("UNCHECKED_CAST")
//                (o1.value as Comparable<E>).compareTo(o2.value)
//            }
//            return if (res == 0) o1.index.compareTo(o2.index) else res
//        }
//    })
//
//    private var counter = 0L
//
//    val size: Int get() = treeSet.size
//
//    fun add(element: E) {
//        treeSet.add(IndexedValue(counter++, element))
//    }
//
//    fun poll(): E? {
//        if (treeSet.isEmpty()) return null
//        val first = treeSet.first()
//        treeSet.remove(first)
//        return first.value
//    }
//
//    fun peek(): E? {
//        if (treeSet.isEmpty()) return null
//        return treeSet.first().value
//    }
//
//    fun isEmpty(): Boolean = treeSet.isEmpty()
//
//    private data class IndexedValue<E>(val index: Long, val value: E)
//}