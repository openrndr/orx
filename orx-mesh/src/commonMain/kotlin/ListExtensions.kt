package org.openrndr.extra.mesh

fun <T> List<T>.indicesOf(premise: (T) -> Boolean): List<Int> {
    return indices.filter { premise(this[it]) }
}