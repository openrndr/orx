fun calculateHistogramRGB(buffer: ColorBuffer,
                          binCount: Int = 16,
                          weighting: ColorRGBa.() -> Double = { 1.0 },
                          downloadShadow: Boolean = true): RGBHistogram {
    val bins = Array(binCount) { Array(binCount) { DoubleArray(binCount) } }
    if (downloadShadow) {
        buffer.shadow.download()
    }

    var totalWeight = 0.0
    val s = buffer.shadow
    for (y in 0 until buffer.height) {
        for (x in 0 until buffer.width) {
            val c = s[x, y]
            val weight = c.weighting()
            val (rb, gb, bb) = c.binIndex(binCount)
            bins[rb][gb][bb] += weight
            totalWeight += weight
        }
    }

    if (totalWeight > 0)
        for (r in 0 until binCount) {
            for (g in 0 until binCount) {
                for (b in 0 until binCount) {
                    bins[r][g][b] /= totalWeight
                }
            }
        }
    return RGBHistogram(bins, binCount)
}


class RGBHistogram(val freqs: Array<Array<DoubleArray>>, val binCount: Int) {
    fun frequency(color: ColorRGBa): Double {
        val (rb, gb, bb) = color.binIndex(binCount)
        return freqs[rb][gb][bb]
    }

    fun color(rBin: Int, gBin: Int, bBin: Int): ColorRGBa =
        ColorRGBa(rBin / (binCount - 1.0), gBin / (binCount - 1.0), bBin / (binCount - 1.0))

    fun sample(random: Random = Random.Default): ColorRGBa {
        val x = random.nextDouble()
        var sum = 0.0
        for (r in 0 until binCount) {
            for (g in 0 until binCount) {
                for (b in 0 until binCount) {
                    sum += freqs[r][g][b]
                    if (sum >= x) {
                        return color(r, g, b)
                    }
                }
            }
        }
        return color(binCount - 1, binCount - 1, binCount - 1)
    }

    fun sortedColors(): List<Pair<ColorRGBa, Double>> {
        val result = mutableListOf<Pair<ColorRGBa, Double>>()
        for (r in 0 until binCount) {
            for (g in 0 until binCount) {
                for (b in 0 until binCount) {
                    result += Pair(
                        ColorRGBa(r / (binCount - 1.0), g / (binCount - 1.0), b / (binCount - 1.0)),
                        freqs[r][g][b]
                    )
                }
            }
        }
        return result.sortedByDescending { it.second }
    }
}