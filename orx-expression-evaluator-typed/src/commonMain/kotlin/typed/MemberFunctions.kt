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
        "map" -> { n -> val lambda = (n[0] as (Any) -> Any); this.map { lambda(it!!) } }
        "filter" -> { n ->
            val lambda = (n[0] as (Any) -> Any); this.filter { (lambda(it!!) as Double).roundToInt() != 0 }
        }

        "max" -> { n -> (this as List<Comparable<Any>>).max() }
        "min" -> { n -> (this as List<Comparable<Any>>).min() }
        "maxBy" -> { n ->
            val lambda = (n[0] as (Any) -> Any); this.maxByOrNull { lambda(it!!) as Comparable<Any> } ?: error("no max")
        }

        "minBy" -> { n ->
            val lambda = (n[0] as (Any) -> Any); this.minByOrNull { lambda(it!!) as Comparable<Any> } ?: error("no max")
        }
        "sorted" -> { n -> (this as List<Comparable<Any>>).sorted() }
        "sortedBy" -> { n ->
            val lambda = (n[0] as (Any) -> Any); this.sortedBy { lambda(it!!) as Comparable<Any> }
        }
        "sortedByDescending" -> { n ->
            val lambda = (n[0] as (Any) -> Any); this.sortedByDescending { lambda(it!!) as Comparable<Any> }
        }
        "reversed" -> { n -> this.reversed() }

        else -> null
    }
}