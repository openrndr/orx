package org.openrndr.extra.noise

inline fun fbm(seed: Int, x: Double, y: Double, z: Double, crossinline noise: (Int, Double, Double, Double) -> Double,
               octaves: Int = 8, lacunarity: Double = 0.5, gain: Double = 0.5): Double {
    var sum = noise(seed, x, y, z)
    var amp = 1.0

    var x = x
    var y = y
    var z = z
    for (i in 1 until octaves) {
        x *= lacunarity
        y *= lacunarity
        z *= lacunarity
        amp *= gain
        sum += noise(seed + i, x, y, z) * amp
    }
    return sum
}


inline fun fbm(seed: Int, x: Double, y: Double, crossinline noise: (Int, Double, Double) -> Double,
               octaves: Int = 8, lacunarity: Double = 0.5, gain: Double = 0.5): Double {
    var sum = noise(seed, x, y)
    var amp = 1.0

    var x = x
    var y = y
    for (i in 1 until octaves) {
        x *= lacunarity
        y *= lacunarity
        amp *= gain
        sum += noise(seed + i, x, y) * amp
    }
    return sum
}

inline fun billow(seed: Int, x: Double, y: Double, z: Double, crossinline noise: (Int, Double, Double, Double) -> Double,
                  octaves: Int = 8, lacunarity: Double = 0.5, gain: Double = 0.5): Double {
    var sum = Math.abs(noise(seed, x, y, z) * 2.0 - 1.0)
    var amp = 1.0

    var x = x
    var y = y
    var z = z
    for (i in 1 until octaves) {
        x *= lacunarity
        y *= lacunarity
        z *= lacunarity
        amp *= gain
        sum += Math.abs(noise(seed + i, x, y, z) * 2.0 - 1.0) * amp
    }
    return sum
}

inline fun billow(seed: Int, x: Double, y: Double, crossinline noise: (Int, Double, Double) -> Double,
                  octaves: Int = 8, lacunarity: Double = 0.5, gain: Double = 0.5): Double {
    var sum = Math.abs(noise(seed, x, y) * 2.0 - 1.0)
    var amp = 1.0

    var x = x
    var y = y
    for (i in 1 until octaves) {
        x *= lacunarity
        y *= lacunarity
        amp *= gain
        sum += Math.abs(noise(seed + i, x, y) * 2.0 - 1.0) * amp
    }
    return sum
}

inline fun rigid(seed: Int, x: Double, y: Double, crossinline noise: (Int, Double, Double) -> Double,
                 octaves: Int = 8, lacunarity: Double = 0.5, gain: Double = 0.5): Double {
    var sum = 1.0 - Math.abs(noise(seed, x, y))
    var amp = 1.0

    var x = x
    var y = y
    for (i in 1 until octaves) {
        x *= lacunarity
        y *= lacunarity
        amp *= gain
        sum -= (1.0 - Math.abs(noise(seed + i, x, y))) * amp
    }
    return sum
}

inline fun rigid(seed: Int, x: Double, y: Double, z: Double, crossinline noise: (Int, Double, Double, Double) -> Double,
                 octaves: Int = 8, lacunarity: Double = 0.5, gain: Double = 0.5): Double {
    var sum = 1.0 - Math.abs(noise(seed, x, y, z))
    var amp = 1.0

    var x = x
    var y = y
    var z = z
    for (i in 1 until octaves) {
        x *= lacunarity
        y *= lacunarity
        z *= lacunarity
        amp *= gain
        sum -= (1.0 - Math.abs(noise(seed + i, x, y, z))) * amp
    }
    return sum
}

