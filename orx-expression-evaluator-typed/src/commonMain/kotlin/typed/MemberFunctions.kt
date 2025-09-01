package org.openrndr.extra.expressions.typed

import kotlin.math.roundToInt

internal fun String.memberFunctions(n: String): ((Array<Any>) -> Any)? {
    return when (n) {
        "take" -> { nn -> this.take((nn[0] as Number).toInt()) }
        "drop" -> { nn -> this.drop((nn[0] as Number).toInt()) }
        "takeLast" -> { nn -> this.takeLast((nn[0] as Number).toInt()) }
        "dropLast" -> { nn -> this.takeLast((nn[0] as Number).toInt()) }
        else -> null
    }
}

internal fun List<*>.memberFunctions(n: String): ((Array<Any>) -> Any)? {
    return when (n) {
        "first" -> { _ -> this.first() ?: error("empty list") }
        "last" -> { _ -> this.last() ?: error("empty list") }
        "take" -> { nn -> this.take((nn[0] as Number).toInt()) }
        "drop" -> { nn -> this.drop((nn[0] as Number).toInt()) }
        "takeLast" -> { nn -> this.takeLast((nn[0] as Number).toInt()) }
        "dropLast" -> { nn -> this.takeLast((nn[0] as Number).toInt()) }
        "map" -> { nn -> @Suppress("UNCHECKED_CAST") val lambda = (nn[0] as (Any) -> Any); this.map { lambda(it!!) } }
        "filter" -> { nn ->
            @Suppress("UNCHECKED_CAST", "UNCHECKED_CAST") val lambda =
                (nn[0] as (Any) -> Any); this.filter { (lambda(it!!) as Double).roundToInt() != 0 }
        }

        "max" -> { _ ->
            @Suppress("UNCHECKED_CAST")
            (this as List<Comparable<Any>>).max()
        }

        "min" -> { _ ->
            @Suppress("UNCHECKED_CAST")
            (this as List<Comparable<Any>>).min()
        }

        "maxBy" -> { nn ->
            @Suppress("UNCHECKED_CAST") val lambda =
                (nn[0] as (Any) -> Any); this.maxByOrNull {
            @Suppress("UNCHECKED_CAST")
            lambda(it!!) as Comparable<Any>
        } ?: error("no max")
        }

        "minBy" -> { nn ->
            @Suppress("UNCHECKED_CAST") val lambda =
                (nn[0] as (Any) -> Any); this.minByOrNull {
            @Suppress("UNCHECKED_CAST")
            lambda(it!!) as Comparable<Any>
        } ?: error("no max")
        }

        "sorted" -> { _ ->
            @Suppress("UNCHECKED_CAST")
            (this as List<Comparable<Any>>).sorted()
        }

        "sortedBy" -> { nn ->
            @Suppress("UNCHECKED_CAST") val lambda =
                (nn[0] as (Any) -> Any); this.sortedBy {
            @Suppress("UNCHECKED_CAST")
            lambda(it!!) as Comparable<Any>
        }
        }

        "sortedByDescending" -> { nn ->
            @Suppress("UNCHECKED_CAST") val lambda = (nn[0] as (Any) -> Any); this.sortedByDescending {
            @Suppress("UNCHECKED_CAST")
            lambda(it!!) as Comparable<Any>
        }
        }

        "reversed" -> { _ -> this.reversed() }
        "zip" -> { nn ->
            @Suppress("UNCHECKED_CAST")
            this.zip(nn[0] as List<Any>).map { listOf(it.first, it.second) }
        }

        else -> null
    }
}