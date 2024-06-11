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
        "take" -> { n -> this.take((n[0] as Number).toInt()) }
        "drop" -> { n -> this.drop((n[0] as Number).toInt()) }
        "takeLast" -> { n -> this.takeLast((n[0] as Number).toInt()) }
        "dropLast" -> { n -> this.takeLast((n[0] as Number).toInt()) }
        "map" -> { n -> val lambda = (n[0] as (Any)->Any); this.map { lambda(it!!)  } }
        "filter" -> { n -> val lambda = (n[0] as (Any)->Any); this.filter { (lambda(it!!) as Double).roundToInt() != 0  } }
        else -> null
    }
}