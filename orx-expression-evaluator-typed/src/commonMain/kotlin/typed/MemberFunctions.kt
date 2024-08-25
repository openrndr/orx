package org.openrndr.extra.expressions.typed

import kotlin.math.roundToInt

internal fun String.memberFunctions(n: String): ((Array<Any>) -> Any)? {
    return when (n) {
        "take" -> { n -> this.take((n[0] as Number).toInt()) }
        "drop" -> { n -> this.drop((n[0] as Number).toInt()) }
        "takeLast" -> { n -> this.takeLast((n[0] as Number).toInt()) }
        "dropLast" -> { n -> this.takeLast((n[0] as Number).toInt()) }
        else -> null
    }
}

internal fun List<*>.memberFunctions(n: String): ((Array<Any>) -> Any)? {
    return when (n) {
        "first" -> { n -> this.first() ?: error("empty list") }
        "last" -> { n -> this.last() ?: error("empty list") }
        "take" -> { n -> this.take((n[0] as Number).toInt()) }
        "drop" -> { n -> this.drop((n[0] as Number).toInt()) }
        "takeLast" -> { n -> this.takeLast((n[0] as Number).toInt()) }
        "dropLast" -> { n -> this.takeLast((n[0] as Number).toInt()) }
        "map" -> { n -> @Suppress("UNCHECKED_CAST") val lambda = (n[0] as (Any) -> Any); this.map { lambda(it!!) } }
        "filter" -> { n ->
            val lambda = (n[0] as (Any) -> Any); this.filter { (lambda(it!!) as Double).roundToInt() != 0 }
        }

        "max" -> { n ->
            @Suppress("UNCHECKED_CAST")
            (this as List<Comparable<Any>>).max()
        }
        "min" -> { n ->
            @Suppress("UNCHECKED_CAST")
            (this as List<Comparable<Any>>).min()
        }
        "maxBy" -> { n ->
            @Suppress("UNCHECKED_CAST") val lambda = (n[0] as (Any) -> Any); this.maxByOrNull { lambda(it!!) as Comparable<Any> } ?: error("no max")
        }

        "minBy" -> { n ->
            @Suppress("UNCHECKED_CAST") val lambda = (n[0] as (Any) -> Any); this.minByOrNull { lambda(it!!) as Comparable<Any> } ?: error("no max")
        }

        "sorted" -> { n ->
            @Suppress("UNCHECKED_CAST")
            (this as List<Comparable<Any>>).sorted()
        }
        "sortedBy" -> { n ->
            @Suppress("UNCHECKED_CAST") val lambda = (n[0] as (Any) -> Any); this.sortedBy { lambda(it!!) as Comparable<Any> }
        }

        "sortedByDescending" -> { n ->
            @Suppress("UNCHECKED_CAST") val lambda = (n[0] as (Any) -> Any); this.sortedByDescending { lambda(it!!) as Comparable<Any> }
        }

        "reversed" -> { n -> this.reversed() }
        "zip" -> { n ->
            @Suppress("UNCHECKED_CAST")
            this.zip(n[0] as List<Any>).map { listOf(it.first, it.second) }
        }

        else -> null
    }
}