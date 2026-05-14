package org.openrndr.extra.shapes.polygon

class TreeSet<E>(val comparator: Comparator<in E>? = null) : Iterable<E> {
    private var root: Node<E>? = null
    var size: Int = 0
        private set

    private class Node<E>(
        var item: E,
        var left: Node<E>? = null,
        var right: Node<E>? = null,
        var parent: Node<E>? = null,
        var color: Boolean = RED
    ) {
        companion object {
            const val RED = true
            const val BLACK = false
        }
    }

    private fun compare(e1: E, e2: E): Int {
        return if (comparator != null) {
            comparator.compare(e1, e2)
        } else {
            (e1 as Comparable<E>).compareTo(e2)
        }
    }

    fun contains(element: E): Boolean {
        return findNode(element) != null
    }

    private fun findNode(element: E): Node<E>? {
        var n = root
        while (n != null) {
            val cmp = compare(element, n.item)
            n = when {
                cmp == 0 -> return n
                cmp < 0 -> n.left
                else -> n.right
            }
        }
        return null
    }

    fun add(element: E): Boolean {
        var t = root
        if (t == null) {
            root = Node(element, color = Node.BLACK)
            size = 1
            return true
        }
        var cmp: Int
        var parent: Node<E>
        do {
            parent = t!!
            cmp = compare(element, t.item)
            if (cmp < 0) t = t.left
            else if (cmp > 0) t = t.right
            else return false
        } while (t != null)

        val e = Node(element, parent = parent)
        if (cmp < 0) parent.left = e
        else parent.right = e
        fixAfterInsertion(e)
        size++
        return true
    }

    fun addAll(elements: Collection<E>): Boolean {
        var modified = false
        for (e in elements) {
            if (add(e)) modified = true
        }
        return modified
    }

    fun remove(element: E): Boolean {
        val p = findNode(element) ?: return false
        deleteNode(p)
        return true
    }

    private fun deleteNode(p: Node<E>) {
        size--
        var node = p
        if (node.left != null && node.right != null) {
            val s = successor(node)!!
            node.item = s.item
            node = s
        }

        val replacement = if (node.left != null) node.left else node.right
        if (replacement != null) {
            replacement.parent = node.parent
            if (node.parent == null) root = replacement
            else if (node == node.parent!!.left) node.parent!!.left = replacement
            else node.parent!!.right = replacement

            node.left = null
            node.right = null
            node.parent = null

            if (node.color == Node.BLACK) fixAfterDeletion(replacement)
        } else if (node.parent == null) {
            root = null
        } else {
            if (node.color == Node.BLACK) fixAfterDeletion(node)
            if (node.parent != null) {
                if (node == node.parent!!.left) node.parent!!.left = null
                else if (node == node.parent!!.right) node.parent!!.right = null
                node.parent = null
            }
        }
    }

    fun first(): E = root?.let {
        var n = it
        while (n.left != null) n = n.left!!
        n.item
    } ?: throw NoSuchElementException()

    fun last(): E = root?.let {
        var n = it
        while (n.right != null) n = n.right!!
        n.item
    } ?: throw NoSuchElementException()

    fun lower(e: E): E? {
        var p = root
        var best: Node<E>? = null
        while (p != null) {
            val cmp = compare(e, p.item)
            if (cmp > 0) {
                best = p
                p = p.right
            } else {
                p = p.left
            }
        }
        return best?.item
    }

    fun higher(e: E): E? {
        var p = root
        var best: Node<E>? = null
        while (p != null) {
            val cmp = compare(e, p.item)
            if (cmp < 0) {
                best = p
                p = p.left
            } else {
                p = p.right
            }
        }
        return best?.item
    }

    fun floor(e: E): E? {
        var p = root
        var best: Node<E>? = null
        while (p != null) {
            val cmp = compare(e, p.item)
            if (cmp == 0) return p.item
            if (cmp > 0) {
                best = p
                p = p.right
            } else {
                p = p.left
            }
        }
        return best?.item
    }

    fun ceiling(e: E): E? {
        var p = root
        var best: Node<E>? = null
        while (p != null) {
            val cmp = compare(e, p.item)
            if (cmp == 0) return p.item
            if (cmp < 0) {
                best = p
                p = p.left
            } else {
                p = p.right
            }
        }
        return best?.item
    }

    fun isEmpty(): Boolean = size == 0

    fun clear() {
        root = null
        size = 0
    }

    override fun iterator(): Iterator<E> = object : Iterator<E> {
        private var next = root?.let {
            var n = it
            while (n.left != null) n = n.left!!
            n
        }
        private var lastReturned: Node<E>? = null

        override fun hasNext(): Boolean = next != null

        override fun next(): E {
            val e = next ?: throw NoSuchElementException()
            lastReturned = e
            next = successor(e)
            return e.item
        }
    }

    private fun colorOf(p: Node<E>?): Boolean = p?.color ?: Node.BLACK
    private fun parentOf(p: Node<E>?): Node<E>? = p?.parent
    private fun leftOf(p: Node<E>?): Node<E>? = p?.left
    private fun rightOf(p: Node<E>?): Node<E>? = p?.right

    private fun setColor(p: Node<E>?, c: Boolean) {
        if (p != null) p.color = c
    }

    private fun rotateLeft(p: Node<E>?) {
        if (p != null) {
            val r = p.right
            p.right = r?.left
            if (r?.left != null) r.left!!.parent = p
            r?.parent = p.parent
            if (p.parent == null) root = r
            else if (p.parent!!.left == p) p.parent!!.left = r
            else p.parent!!.right = r
            r?.left = p
            p.parent = r
        }
    }

    private fun rotateRight(p: Node<E>?) {
        if (p != null) {
            val l = p.left
            p.left = l?.right
            if (l?.right != null) l.right!!.parent = p
            l?.parent = p.parent
            if (p.parent == null) root = l
            else if (p.parent!!.right == p) p.parent!!.right = l
            else p.parent!!.left = l
            l?.right = p
            p.parent = l
        }
    }

    private fun fixAfterInsertion(x: Node<E>?) {
        var node = x
        node?.color = Node.RED
        while (node != null && node != root && node.parent!!.color == Node.RED) {
            if (parentOf(node) == leftOf(parentOf(parentOf(node)))) {
                val y = rightOf(parentOf(parentOf(node)))
                if (colorOf(y) == Node.RED) {
                    setColor(parentOf(node), Node.BLACK)
                    setColor(y, Node.BLACK)
                    setColor(parentOf(parentOf(node)), Node.RED)
                    node = parentOf(parentOf(node))
                } else {
                    if (node == rightOf(parentOf(node))) {
                        node = parentOf(node)
                        rotateLeft(node)
                    }
                    setColor(parentOf(node), Node.BLACK)
                    setColor(parentOf(parentOf(node)), Node.RED)
                    rotateRight(parentOf(parentOf(node)))
                }
            } else {
                val y = leftOf(parentOf(parentOf(node)))
                if (colorOf(y) == Node.RED) {
                    setColor(parentOf(node), Node.BLACK)
                    setColor(y, Node.BLACK)
                    setColor(parentOf(parentOf(node)), Node.RED)
                    node = parentOf(parentOf(node))
                } else {
                    if (node == leftOf(parentOf(node))) {
                        node = parentOf(node)
                        rotateRight(node)
                    }
                    setColor(parentOf(node), Node.BLACK)
                    setColor(parentOf(parentOf(node)), Node.RED)
                    rotateLeft(parentOf(parentOf(node)))
                }
            }
        }
        root?.color = Node.BLACK
    }

    private fun fixAfterDeletion(x: Node<E>?) {
        var node = x
        while (node != root && colorOf(node) == Node.BLACK) {
            if (node == leftOf(parentOf(node))) {
                var sib = rightOf(parentOf(node))
                if (colorOf(sib) == Node.RED) {
                    setColor(sib, Node.BLACK)
                    setColor(parentOf(node), Node.RED)
                    rotateLeft(parentOf(node))
                    sib = rightOf(parentOf(node))
                }
                if (colorOf(leftOf(sib)) == Node.BLACK && colorOf(rightOf(sib)) == Node.BLACK) {
                    setColor(sib, Node.RED)
                    node = parentOf(node)
                } else {
                    if (colorOf(rightOf(sib)) == Node.BLACK) {
                        setColor(leftOf(sib), Node.BLACK)
                        setColor(sib, Node.RED)
                        rotateRight(sib)
                        sib = rightOf(parentOf(node))
                    }
                    setColor(sib, colorOf(parentOf(node)))
                    setColor(parentOf(node), Node.BLACK)
                    setColor(rightOf(sib), Node.BLACK)
                    rotateLeft(parentOf(node))
                    node = root
                }
            } else {
                var sib = leftOf(parentOf(node))
                if (colorOf(sib) == Node.RED) {
                    setColor(sib, Node.BLACK)
                    setColor(parentOf(node), Node.RED)
                    rotateRight(parentOf(node))
                    sib = leftOf(parentOf(node))
                }
                if (colorOf(rightOf(sib)) == Node.BLACK && colorOf(leftOf(sib)) == Node.BLACK) {
                    setColor(sib, Node.RED)
                    node = parentOf(node)
                } else {
                    if (colorOf(leftOf(sib)) == Node.BLACK) {
                        setColor(rightOf(sib), Node.BLACK)
                        setColor(sib, Node.RED)
                        rotateLeft(sib)
                        sib = leftOf(parentOf(node))
                    }
                    setColor(sib, colorOf(parentOf(node)))
                    setColor(parentOf(node), Node.BLACK)
                    setColor(leftOf(sib), Node.BLACK)
                    rotateRight(parentOf(node))
                    node = root
                }
            }
        }
        setColor(node, Node.BLACK)
    }

    private fun successor(t: Node<E>?): Node<E>? {
        if (t == null) return null
        else if (t.right != null) {
            var p = t.right!!
            while (p.left != null) p = p.left!!
            return p
        } else {
            var p = t.parent
            var ch = t
            while (p != null && ch == p.right) {
                ch = p
                p = p.parent
            }
            return p
        }
    }
}