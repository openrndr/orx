package org.openrndr.extra.kdtree


actual fun <T> buildKDTree(items: MutableList<T>, dimensions: Int, mapper: (T, Int) -> Double): KDTreeNode<T> {
    val root = KDTreeNode<T>(dimensions, mapper)

    fun <T> buildTreeTask(
        node: KDTreeNode<T>,
        items: MutableList<T>,
        dimensions: Int,
        levels: Int,
        mapper: (T, Int) -> Double
    ): KDTreeNode<T> {

        if (items.size > 0) {
            val dimension = levels % dimensions
            val values = ArrayList<T>()
            for (item in items) {
                values.add(item)
            }

            node.dimension = dimension
            val median = selectNth(items, items.size / 2) { mapper(it, dimension) }

            val leftItems = mutableListOf<T>()
            val rightItems = mutableListOf<T>()

            node.median = mapper(median, dimension)
            node.item = median
            for (item in items) {
                if (item === median) {
                    continue
                }
                if (mapper(item, dimension) < node.median) {
                    leftItems.add(item)
                } else {
                    rightItems.add(item)
                }
            }

            // validate split
            if (leftItems.size + rightItems.size + 1 != items.size) {
                throw IllegalStateException("left: ${leftItems.size}, right: ${rightItems.size}, items: ${items.size}")
            }

            if (leftItems.size > 0) {
                node.children[0] = KDTreeNode(dimensions, mapper)
                node.children[0]?.let {
                    it.parent = node


                    buildTreeTask(it, leftItems, dimensions, levels + 1, mapper)

                }
            }
            if (rightItems.size > 0) {
                node.children[1] = KDTreeNode(dimensions, mapper)
                node.children[1]?.let {
                    it.parent = node
                    buildTreeTask(it, rightItems, dimensions, levels + 1, mapper)

                }
            }
        }
        return node
    }


    buildTreeTask(root, items, dimensions, 0, mapper)
    return root
}
