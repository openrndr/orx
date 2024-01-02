package org.openrndr.extra.kdtree


import org.openrndr.collections.PriorityQueue
import kotlin.math.abs



class KDTreeNode<T>(val dimensions: Int, val mapper: (T, Int) -> Double) {
    var parent: KDTreeNode<T>? = null
    var median: Double = 0.0
    var dimension: Int = 0
    var children: Array<KDTreeNode<T>?> = arrayOfNulls(2)
    var item: T? = null

    internal val isLeaf: Boolean
        get() = children[0] == null && children[1] == null


    fun insert(item: T): KDTreeNode<T> {
        return insert(this, item, dimensions, mapper)
    }

    fun remove(node: KDTreeNode<T>): KDTreeNode<T>? {
        return org.openrndr.extra.kdtree.remove(node, mapper)
    }


    fun findNearest(query: T, includeQuery: Boolean = false): T? = findNearest(this, query, includeQuery)

    fun findKNearest(query: T, k: Int, includeQuery: Boolean = false): List<T> {
        return findKNearest(this, query, k, includeQuery)
    }

    fun findAllInRadius(query: T, radius: Double, includeQuery: Boolean = false): List<T> {
        return findAllInRadius(this, query, radius, includeQuery)
    }

    override fun toString(): String {
        return "KDTreeNode{" +
                "median=" + median +
                ", item=" + item +
                ", dimension=" + dimension +
                ", children=" + children.joinToString(", ") { it.toString() } +
                "} " + super.toString()
    }
}

private fun <T> insertItem(root: KDTreeNode<T>, item: T): KDTreeNode<T> {
    return if (root.isLeaf) {
        root.item = item
        root
    } else {
        if (root.mapper(item, root.dimension) < root.median) {
            insertItem(root.children[0] ?: throw IllegalStateException("left is null"), item)
        } else {
            insertItem(root.children[1] ?: throw IllegalStateException("right is null"), item)
        }
    }
}

expect fun <T> buildKDTree(items: MutableList<T>, dimensions: Int, mapper: (T, Int) -> Double): KDTreeNode<T>



private fun <T> sqrDistance(left: T, right: T, dimensions: Int, mapper: (T, Int) -> Double): Double {
    var distance = 0.0

    for (i in 0 until dimensions) {
        val d = mapper(left, i) - mapper(right, i)
        distance += d * d
    }
    return distance
}

fun <T> findAllNodes(root: KDTreeNode<T>): List<KDTreeNode<T>> {
    val stack = mutableListOf<KDTreeNode<T>>()
    val all = mutableListOf<KDTreeNode<T>>()
    stack.add(root)
    while (!stack.isEmpty()) {
        val node = stack.removeLast()
        all.add(node)

        if (node.children[0] != null) {
            stack.add(node.children[0] !!)
        }
        if (node.children[1] != null) {
            stack.add(node.children[1] !!)
        }
    }
    return all
}


fun <T> findKNearest(
    root: KDTreeNode<T>,
    query: T,
    k: Int,
    includeQuery: Boolean = false
): List<T> {
    // max-heap with size k
    val queue = PriorityQueue<Pair<KDTreeNode<T>, Double>>(k + 1) { nodeA, nodeB ->
        compareValues(nodeB.second, nodeA.second)
    }

    fun nearest(node: KDTreeNode<T>?) {
        if (node != null) {
            val dimensionValue = node.mapper(query, node.dimension)
            val route: Int = if (dimensionValue < node.median) {
                nearest(node.children[0])
                0
            } else {
                nearest(node.children[1])
                1
            }

            val distance = sqrDistance(query, node.item ?: error("item is null"), node.dimensions, node.mapper)

            if (includeQuery || node.item !== query) {
                if (queue.size() < k || distance < queue.peek().second) {
                    queue.add(Pair(node, distance))
                    if (queue.size() > k) {
                        queue.poll()
                    }
                }
            }

            val d = abs(node.median - dimensionValue)
            if (queue.size() < k || d * d < queue.peek().second) {
                nearest(node.children[1 - route])
            }
        }
    }

    nearest(root)

    return generateSequence { queue.poll() }
        .map { it.first.item }
        .filterNotNull()
        .toList().reversed()
}

private fun <T> findNearest(root: KDTreeNode<T>, query: T, includeQuery: Boolean = false): T? {
    var nearest = Double.POSITIVE_INFINITY
    var nearestArg: KDTreeNode<T>? = null

    fun nearest(node: KDTreeNode<T>?) {
        if (node != null) {
            val route: Int = if (root.mapper(query, node.dimension) < node.median) {
                nearest(node.children[0])
                0
            } else {
                nearest(node.children[1])
                1
            }

            val distance = sqrDistance(
                query, node.item
                    ?: error("item is null"), root.dimensions, root.mapper
            )
            if (distance < nearest && (includeQuery || node.item !== query)) {
                nearest = distance
                nearestArg = node
            }

            val d = abs(node.median - root.mapper(query, node.dimension))
            if (d * d < nearest) {
                nearest(node.children[1 - route])
            }
        }
    }
    nearest(root)
    return nearestArg?.item
}

private fun <T> findAllInRadius(
    root: KDTreeNode<T>,
    query: T,
    radius: Double,
    includeQuery: Boolean = false
): List<T> {

    val sqrMaxDist = radius * radius
    val queue = ArrayDeque<KDTreeNode<T>>()
    queue.add(root)
    val results = mutableListOf<T?>()

    while (queue.isNotEmpty()) {
        val node = queue.removeFirst()
        val dimensionValue = node.mapper(query, node.dimension)
        val distance = sqrDistance(
            query, node.item
                ?: error("item is null"), node.dimensions, node.mapper
        )
        if (distance <= sqrMaxDist && (includeQuery || node.item != query)) {
            results.add(node.item)
        }

        val route: Int = if ((dimensionValue < node.median || node.children[1]==null) && node.children[0] != null )  {
            queue.add(node.children[0]!!)
            0
        } else if (node.children[1] != null) {
            queue.add(node.children[1]!!)
            1
        } else {
            -1
        }

        if (route != -1) {
            val d = abs(node.median - dimensionValue)
            if (d * d <= sqrMaxDist) {
                val c = node.children[1 - route]
                if (c != null) {
                    queue.add(c)
                }
            }
        }
    }

    return results.filterNotNull()
}

private fun <T> insert(root: KDTreeNode<T>, item: T, dimensions: Int, mapper: (T, Int) -> Double): KDTreeNode<T> {
    val stack = mutableListOf<KDTreeNode<T>>()
    stack.add(root)

    dive@ while (true) {

        val node = stack.last()

        val value = mapper(item, node.dimension)

        if (value < node.median) {
            if (node.children[0] != null) {
                stack.add(node.children[0]!!)
            } else {
                // sit here
                node.children[0] = KDTreeNode(dimensions, mapper)
                node.children[0]?.item = item
                node.children[0]?.dimension = (node.dimension + 1) % dimensions
                node.children[0]?.median = mapper(item, (node.dimension + 1) % dimensions)
                node.children[0]?.parent = node
                return node.children[0] ?: throw IllegalStateException("child is null")
            }
        } else {
            if (node.children[1] != null) {
                stack.add(node.children[1]!!)
            } else {
                // sit here
                node.children[1] = KDTreeNode(dimensions, mapper)
                node.children[1]?.item = item
                node.children[1]?.dimension = (node.dimension + 1) % dimensions
                node.children[1]?.median = mapper(item, (node.dimension + 1) % dimensions)
                node.children[1]?.parent = node
                return node.children[1] ?: throw IllegalStateException("child is null")

            }
        }
    }
}

private fun <T> remove(toRemove: KDTreeNode<T>, mapper: (T, Int) -> Double): KDTreeNode<T>? {
    // trivial case
    if (toRemove.isLeaf) {
        val p = toRemove.parent
        if (p != null) {
            when {
                p.children[0] === toRemove -> p.children[0] = null
                p.children[1] === toRemove -> p.children[1] = null
                else -> {
                    // broken!
                }
            }
        } else {
            toRemove.item = null
        }
    } else {
        val stack = mutableListOf<KDTreeNode<T>>()

        var branch = 0

        if (toRemove.children[0] != null) {
            stack.add(toRemove.children[0]!!)
            branch = 0
        } else {
            stack.add(toRemove.children[1]!!)
            branch = 1
        }

        var minValue: Double = Double.POSITIVE_INFINITY
        var maxValue: Double = Double.NEGATIVE_INFINITY
        var minArg: KDTreeNode<T>? = null
        var maxArg: KDTreeNode<T>? = null

        while (!stack.isEmpty()) {
            val node = stack.removeLast()

            val value = mapper(node.item ?: error("item is null"), toRemove.dimension)

            if (value < minValue) {
                minValue = value
                minArg = node
            }

            if (value > maxValue) {
                maxValue = value
                maxArg = node
            }

            if (node.dimension != toRemove.dimension) {
                if (node.children[0] != null) {
                    stack.add(node.children[0]!!)
                }
                if (node.children[1] != null) {
                    stack.add(node.children[1]!!)
                }
            } else {
                if (branch == 1) {
                    if (node.children[0] != null) {
                        stack.add(node.children[0]!!)
                    } else {
                        if (node.children[1] != null) {
                            stack.add(node.children[1]!!)
                        }
                    }
                }
                if (branch == 0) {
                    if (node.children[1] != null) {
                        stack.add(node.children[1]!!)
                    } else {
                        if (node.children[0] != null) {
                            stack.add(node.children[0]!!)
                        }
                    }
                }
            }
        }


        if (branch == 1) {
            toRemove.item = minArg?.item
            toRemove.median = mapper(minArg?.item ?: throw IllegalStateException("minArg is null"), toRemove.dimension)
            remove(minArg, mapper)
        }
        if (branch == 0) {
            toRemove.item = maxArg?.item
            toRemove.median = mapper(maxArg?.item ?: throw IllegalStateException("maxArg is null"), toRemove.dimension)
            remove(maxArg, mapper)
        }
    }
    return null
}
