class IntervalNode<T>(val center: Double) {
    val overlapBegin = mutableListOf<Pair<Double, T>>()
    val overlapEnd = mutableListOf<Pair<Double, T>>()

    var left: IntervalNode<T>? = null
    var right: IntervalNode<T>? = null

    fun queryPoint(x: Double): MutableList<T> {
        val results = mutableListOf<T>()
        queryPoint(x, results)
        return results
    }

    private fun queryPoint(x: Double, results: MutableList<T>) {
        if (x < center) {
            for ((start, item) in overlapBegin) {
                if (start <= x) {
                    results.add(item)
                } else {
                    break
                }
            }
            left?.queryPoint(x, results)
        } else if (x > center) {
            for ((end, item) in overlapEnd) {
                if (end > x) {
                    results.add(item)
                } else {
                    break
                }
            }
            right?.queryPoint(x, results)
        } else if (x == center) {
            results.addAll(overlapBegin.map { it.second })
        }
    }
}


fun <T : Any> buildIntervalTree(items: List<T>, intervalFunction: (T) -> Pair<Double, Double>): IntervalNode<T> {
    val ranges = items.map { intervalFunction(it) }
    val center = ranges.sumByDouble { (it.first + it.second) / 2.0 } / ranges.size
    val node = IntervalNode<T>(center)
    val leftItems = mutableListOf<T>()
    val rightItems = mutableListOf<T>()
    for (item in items) {
        val interval = intervalFunction(item)
        if (interval.first <= center && interval.second > center) {
            node.overlapBegin.add(Pair(interval.first, item))
            node.overlapEnd.add(Pair(interval.second, item))
        } else if (interval.second <= center) {
            leftItems.add(item)
        } else if (interval.first > center) {
            rightItems.add(item)
        }
    }
    node.overlapBegin.sortBy { it.first }
    node.overlapEnd.sortByDescending { it.first }

    if (leftItems.isNotEmpty()) {
        node.left = buildIntervalTree(leftItems, intervalFunction)
    }
    if (rightItems.isNotEmpty()) {
        node.right = buildIntervalTree(rightItems, intervalFunction)
    }
    return node
}

fun time(f: () -> Unit) {
    val start = System.currentTimeMillis()
    f()
    val end = System.currentTimeMillis()
    println("that took ${end - start}ms")
}

fun main() {
    class Test(val start: Double, val end: Double)

    val items = List(100000) { Test(Math.random(), 1.0 + Math.random()) }
    val root = buildIntervalTree(items) {
        Pair(it.start, it.end)
    }

    time {
        for (i in 0 until 10000) {
            @Suppress("UNUSED_VARIABLE")
            val results = items.filter { it.start <= 0.05 && it.end > 0.05 }
        }
    }

    time {
        for (i in 0 until 10000) {
            root.queryPoint(0.05)
        }
    }
}