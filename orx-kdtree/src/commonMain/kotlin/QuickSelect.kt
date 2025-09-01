package org.openrndr.extra.kdtree

/**
 * Selects the nth element from the given list after partially sorting it based on a mapping function.
 *
 * This method modifies the input list to reorder its elements such that the nth element is placed
 * at the correct sorted position if the list were fully sorted according to the provided mapper.
 *
 * @param items The list of elements to process and partially sort.
 * @param n The zero-based index of the element to select after partial sorting.
 * @param mapper A lambda function that maps an element of type T to a Double, used to determine sorting order.
 * @return The nth element in the reordered list.
 */
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