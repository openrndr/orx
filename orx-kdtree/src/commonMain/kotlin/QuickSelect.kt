package org.openrndr.extra.kdtree

fun <T> selectNth(items: MutableList<T>, n: Int, mapper: (T)->Double): T {
    var from = 0
    var to = items.size - 1

    // if from == to we reached the kth element
    while (from < to) {
        var r = from
        var w = to
        val mid = mapper(items[(r + w) / 2])

        // stop when the reader and writer meet
        while (r < w) {
            if (mapper(items[r]) >= mid) { // put the large values at the end
                val tmp = items[w]
                items[w] = items[r]
                items[r] = tmp
                w--
            } else { // the value is smaller than the pivot, skip
                r++
            }
        }

        // if we stepped up (r++) we need to step one down
        if (mapper(items[r]) > mid)
            r--

        // the r pointer is on the end of the first k elements
        if (n <= r) {
            to = r
        } else {
            from = r + 1
        }
    }
    return items[n]
}