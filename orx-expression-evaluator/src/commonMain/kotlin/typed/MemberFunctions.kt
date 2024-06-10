package org.openrndr.extra.expressions.typed

fun String.memberFunctions(n: String): ((Array<Any>) -> Any)? {
    return when (n) {
        "take" -> { n -> this.take((n[0] as Number).toInt()) }
        "drop" -> { n -> this.drop((n[0] as Number).toInt()) }
        "takeLast" -> { n -> this.takeLast((n[0] as Number).toInt()) }
        "dropLast" -> { n -> this.takeLast((n[0] as Number).toInt()) }
        else -> null
    }
}