package org.openrndr.extra.noise

import kotlin.math.abs


inline fun fbm(seed: Int, x: Double, y: Double, z: Double, w: Double, crossinline noise: (Int, Double, Double, Double, Double) -> Double,
               octaves: Int = 8, lacunarity: Double = 0.5, gain: Double = 0.5): Double {
    var sum = noise(seed, x, y, z, w)
    var amp = 1.0

    var lx = x
    var ly = y
    var lz = z
    var lw = w
    for (i in 1 until octaves) {
        lx *= lacunarity
        ly *= lacunarity
        lz *= lacunarity
        lw *= lacunarity
        amp *= gain
        sum += noise(seed + i, lx, ly, lz, lw) * amp
    }
    return sum
}

inline fun fbm(seed: Int, x: Double, y: Double, z: Double, crossinline noise: (Int, Double, Double, Double) -> Double,
               octaves: Int = 8, lacunarity: Double = 0.5, gain: Double = 0.5): Double {
    var sum = noise(seed, x, y, z)
    var amp = 1.0

    var lx = x
    var ly = y
    var lz = z
    for (i in 1 until octaves) {
        lx *= lacunarity
        ly *= lacunarity
        lz *= lacunarity
        amp *= gain
        sum += noise(seed + i, lx, ly, lz) * amp
    }
    return sum
}

inline fun fbm(seed: Int, x: Double, y: Double, crossinline noise: (Int, Double, Double) -> Double,
               octaves: Int = 8, lacunarity: Double = 0.5, gain: Double = 0.5): Double {
    var sum = noise(seed, x, y)
    var amp = 1.0

    var lx = x
    var ly = y
    for (i in 1 until octaves) {
        lx *= lacunarity
        ly *= lacunarity
        amp *= gain
        sum += noise(seed + i, lx, ly) * amp
    }
    return sum
}

inline fun fbm(seed: Int, x: Double, crossinline noise: (Int, Double) -> Double,
               octaves: Int = 8, lacunarity: Double = 0.5, gain: Double = 0.5): Double {
    var sum = noise(seed, x)
    var amp = 1.0

    var lx = x
    for (i in 1 until octaves) {
        lx *= lacunarity
        amp *= gain
        sum += noise(seed + i, lx) * amp
    }
    return sum
}

inline fun fbmFunc1D(crossinline noise: (Int, Double) -> Double, octaves: Int = 8, lacunarity: Double = 0.5, gain: Double = 0.5): (Int, Double) -> Double {
    return { seed, x ->
        fbm(seed, x, noise, octaves, lacunarity, gain)
    }
}

inline fun fbmFunc2D(crossinline noise: (Int, Double, Double) -> Double, octaves: Int = 8, lacunarity: Double = 0.5, gain: Double = 0.5): (Int, Double, Double) -> Double {
    return { seed, x, y ->
        fbm(seed, x, y, noise, octaves, lacunarity, gain)
    }
}

inline fun fbmFunc3D(crossinline noise: (Int, Double, Double, Double) -> Double, octaves: Int = 8, lacunarity: Double = 0.5, gain: Double = 0.5): (Int, Double, Double, Double) -> Double {
    return { seed, x, y, z ->
        fbm(seed, x, y, z, noise, octaves, lacunarity, gain)
    }
}

inline fun fbmFunc4D(crossinline noise: (Int, Double, Double, Double, Double) -> Double, octaves: Int = 8, lacunarity: Double = 0.5, gain: Double = 0.5): (Int, Double, Double, Double, Double) -> Double {
    return { seed, x, y, z, w ->
        fbm(seed, x, y, z, w, noise, octaves, lacunarity, gain)
    }
}



inline fun billow(seed: Int, x: Double, y: Double, z: Double, w: Double, crossinline noise: (Int, Double, Double, Double, Double) -> Double,
                  octaves: Int = 8, lacunarity: Double = 0.5, gain: Double = 0.5): Double {
    var sum = Math.abs(noise(seed, x, y, z, w) * 2.0 - 1.0)
    var amp = 1.0

    var x = x
    var y = y
    var z = z
    var w = w
    for (i in 1 until octaves) {
        x *= lacunarity
        y *= lacunarity
        z *= lacunarity
        w *= lacunarity
        amp *= gain
        sum += Math.abs(noise(seed + i, x, y, z, w) * 2.0 - 1.0) * amp
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
    var sum = abs(noise(seed, x, y) * 2.0 - 1.0)
    var amp = 1.0

    var x = x
    var y = y
    for (i in 1 until octaves) {
        x *= lacunarity
        y *= lacunarity
        amp *= gain
        sum += abs(noise(seed + i, x, y) * 2.0 - 1.0) * amp
    }
    return sum
}

inline fun billow(seed: Int, x: Double, crossinline noise: (Int, Double) -> Double,
                  octaves: Int = 8, lacunarity: Double = 0.5, gain: Double = 0.5): Double {
    var sum = abs(noise(seed, x) * 2.0 - 1.0)
    var amp = 1.0

    var x = x
    for (i in 1 until octaves) {
        x *= lacunarity
        amp *= gain
        sum += abs(noise(seed + i, x) * 2.0 - 1.0) * amp
    }
    return sum
}

inline fun billowFunc1D(crossinline noise: (Int, Double) -> Double, octaves: Int = 8, lacunarity: Double = 0.5, gain: Double = 0.5): (Int, Double) -> Double {
    return { seed, x ->
        billow(seed, x, noise, octaves, lacunarity, gain)
    }
}

inline fun billowFunc2D(crossinline noise: (Int, Double, Double) -> Double, octaves: Int = 8, lacunarity: Double = 0.5, gain: Double = 0.5): (Int, Double, Double) -> Double {
    return { seed, x, y ->
        billow(seed, x, y, noise, octaves, lacunarity, gain)
    }
}

inline fun billowFunc3D(crossinline noise: (Int, Double, Double, Double) -> Double, octaves: Int = 8, lacunarity: Double = 0.5, gain: Double = 0.5): (Int, Double, Double, Double) -> Double {
    return { seed, x, y, z ->
        billow(seed, x, y, z, noise, octaves, lacunarity, gain)
    }
}

inline fun billowFunc4D(crossinline noise: (Int, Double, Double, Double, Double) -> Double, octaves: Int = 8, lacunarity: Double = 0.5, gain: Double = 0.5): (Int, Double, Double, Double, Double) -> Double {
    return { seed, x, y, z, w ->
        billow(seed, x, y, z, w, noise, octaves, lacunarity, gain)
    }
}

inline fun rigid(seed: Int, x: Double, y: Double, z: Double, w: Double, crossinline noise: (Int, Double, Double, Double, Double) -> Double,
                 octaves: Int = 8, lacunarity: Double = 0.5, gain: Double = 0.5): Double {
    var sum = 1.0 - Math.abs(noise(seed, x, y, z, w))
    var amp = 1.0

    var x = x
    var y = y
    var z = z
    var w = w
    for (i in 1 until octaves) {
        x *= lacunarity
        y *= lacunarity
        z *= lacunarity
        w *= lacunarity
        amp *= gain
        sum -= (1.0 - abs(noise(seed + i, x, y, z, w))) * amp
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
        sum -= (1.0 - abs(noise(seed + i, x, y, z))) * amp
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

inline fun rigid(seed: Int, x: Double, crossinline noise: (Int, Double) -> Double,
                 octaves: Int = 8, lacunarity: Double = 0.5, gain: Double = 0.5): Double {
    var sum = 1.0 - abs(noise(seed, x))
    var amp = 1.0

    var x = x
    for (i in 1 until octaves) {
        x *= lacunarity
        amp *= gain
        sum -= (1.0 - abs(noise(seed + i, x))) * amp
    }
    return sum
}

inline fun rigidFunc1D(crossinline noise: (Int, Double) -> Double, octaves: Int = 8, lacunarity: Double = 0.5, gain: Double = 0.5): (Int, Double) -> Double {
    return { seed, x ->
        rigid(seed, x, noise, octaves, lacunarity, gain)
    }
}

inline fun rigidFunc2D(crossinline noise: (Int, Double, Double) -> Double, octaves: Int = 8, lacunarity: Double = 0.5, gain: Double = 0.5): (Int, Double, Double) -> Double {
    return { seed, x, y ->
        rigid(seed, x, y, noise, octaves, lacunarity, gain)
    }
}

inline fun rigidFunc3D(crossinline noise: (Int, Double, Double, Double) -> Double, octaves: Int = 8, lacunarity: Double = 0.5, gain: Double = 0.5): (Int, Double, Double, Double) -> Double {
    return { seed, x, y, z ->
        rigid(seed, x, y, z, noise, octaves, lacunarity, gain)
    }
}

inline fun rigidFunc4D(crossinline noise: (Int, Double, Double, Double, Double) -> Double, octaves: Int = 8, lacunarity: Double = 0.5, gain: Double = 0.5): (Int, Double, Double, Double, Double) -> Double {
    return { seed, x, y, z, w ->
        rigid(seed, x, y, z, w, noise, octaves, lacunarity, gain)
    }
}
