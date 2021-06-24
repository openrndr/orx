package org.openrndr.extra.noise

internal fun sumDistribution(probabilities: List<Double>): List<Double> = probabilities.foldIndexed(mutableListOf()) {
    index: Int, list: MutableList<Double>, prob: Double ->
    val prev = list.elementAtOrNull(index - 1) ?: 0.0
    list.add(prev + prob)
    list
}

internal fun createDecreasingOdds(size: Int): List<Double> {
    var den = 4.0;
    var t = 1.0 + (1.0 / (size / 3.0))

    return (1 until size).map {
        val prob = t / den

        t -= prob
        den += 1.0
        prob
    }
}

object Distribute {
    fun equal(size: Int): List<Double> = sumDistribution(List(size) { 1.0 / size })
    fun decreasing(size: Int): List<Double> = sumDistribution(createDecreasingOdds(size))
    fun increasing(size: Int): List<Double> = sumDistribution(createDecreasingOdds(size)).asReversed()
}