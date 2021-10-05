package org.openrndr.extra.noise

import org.openrndr.math.*
import kotlin.jvm.JvmName

fun ((Double) -> Double).withSeedAsOffset(offset: Double): (Int, Double) -> Double = { seed, x ->
    this(x + seed * offset)
}

fun ((Int, Double) -> Double).withFixedSeed(seed: Int): (Double) -> Double = { x -> this(seed, x) }


fun ((Int, Double) -> Double).gradient(epsilon: Double = 1e-6): (Int, Double) -> Double = { seed, x ->
    (this(seed, x + epsilon) - this(seed, x - epsilon)) / (2 * epsilon)
}

fun ((Int, Double, Double) -> Vector2).gradient(epsilon: Double = 1e-6): (Int, Double, Double) -> Vector2 =
    { seed, x, y ->
        val dfdx = (this(seed, x + epsilon, y) - this(seed, x - epsilon, y)) / (2 * epsilon)
        val dfdy = (this(seed, x, y + epsilon) - this(seed, x, y - epsilon)) / (2 * epsilon)
        dfdx + dfdy
    }

fun ((Int, Double) -> Double).crossFade(
    start: Double,
    end: Double,
    width: Double = 0.5
): (Int, Double) -> Double {
    return { seed, t ->
        val a = t.map(start, end, 0.0, 1.0).mod_(1.0)
        val f = (a / width).coerceAtMost(1.0)
        val o = this(seed, a.map(0.0, 1.0, start, end)) * f
        val s = this(seed, (a + 1.0).map(0.0, 1.0, start, end)) * (1.0 - f)
        o + s
    }
}

fun ((Int, Double, Double) -> Double).crossFade(
    start: Double,
    end: Double,
    width: Double = 0.5
): (Int, Double, Double) -> Double {
    return { seed, x, t ->
        val a = t.map(start, end, 0.0, 1.0).mod_(1.0)
        val f = (a / width).coerceAtMost(1.0)
        val o = this(seed, x, a.map(0.0, 1.0, start, end)) * f
        val s = this(seed, x, (a + 1.0).map(0.0, 1.0, start, end) )* (1.0 - f)
        o + s
    }
}

fun ((Int, Double, Double, Double) -> Double).crossFade(
    start: Double,
    end: Double,
    width: Double = 0.5
): (Int, Double, Double, Double) -> Double {
    return { seed, x, y, t ->
        val a = t.map(start, end, 0.0, 1.0).mod_(1.0)
        val f = (a / width).coerceAtMost(1.0)
        val o = this(seed, x, y, a.map(0.0, 1.0, start, end)) * f
        val s = this(seed, x, y, (a + 1.0).map(0.0, 1.0, start, end)) * (1.0 - f)
        o + s
    }
}

fun ((Int, Double, Double, Double) -> Vector2).gradient(epsilon: Double = 1e-2 / 2.0): (Int, Double, Double, Double) -> Vector2 =
    { seed, x, y, z ->
        val dfdx = (this(seed, x + epsilon, y, z) - this(seed, x - epsilon, y, z)) / (2 * epsilon)
        val dfdy = (this(seed, x, y + epsilon, z) - this(seed, x, y - epsilon, z)) / (2 * epsilon)
        dfdx + dfdy
    }


fun ((Int, Double) -> Double).scaleShiftInput(scaleT: Double = 1.0, shiftT: Double = 0.0) =
    { seed: Int, t: Double -> this(seed, t * scaleT + shiftT) }

fun ((Int, Double, Double) -> Double).scaleShiftInput(
    scaleX: Double = 1.0,
    shiftX: Double = 0.0,
    scaleT: Double = 1.0,
    shiftT: Double = 0.0
) = { seed: Int, x: Double, t: Double -> this(seed, x * scaleX + shiftX, t * scaleT + shiftT) }

fun ((Int, Double, Double, Double) -> Double).scaleShiftInput(
    scaleX: Double = 1.0,
    shiftX: Double = 0.0,
    scaleY: Double = 1.0,
    shiftY: Double = 0.0,
    scaleT: Double = 1.0,
    shiftT: Double = 0.0
) = { seed: Int, x: Double, y: Double, t: Double ->
    this(
        seed,
        x * scaleX + shiftX,
        y * scaleY + shiftY,
        t * scaleT + shiftT
    )
}

fun ((Int, Double, Double, Double, Double) -> Double).scaleShiftInput(
    scaleX: Double = 1.0,
    shiftX: Double = 0.0,
    scaleY: Double = 1.0,
    shiftY: Double = 0.0,
    scaleZ: Double = 1.0,
    shiftZ: Double = 0.0,
    scaleT: Double = 1.0,
    shiftT: Double = 0.0
) = { seed: Int, x: Double, y: Double, z: Double, t: Double ->
    this(
        seed,
        x * scaleX + shiftX,
        y * scaleY + shiftY,
        z * scaleZ + shiftZ,
        t * scaleT + shiftT
    )
}



fun ((Int, Double) -> Double).scaleShiftOutput(
    scale: Double = 1.0,
    bias: Double = 0.0
): (Int, Double) -> Double = { seed, x ->
    this(seed, x) * scale + bias
}

fun ((Int, Double) -> Double).mapOutput(map: (Double) -> Double): (Int, Double) -> Double = { seed, x ->
    map(this(seed, x))
}

fun ((Int, Double, Double) -> Double).withVector2Input(): (Int, Vector2) -> Double = { seed, v ->
    this(seed, v.x, v.y)
}

@JvmName("scaleBiasVector2")
fun ((Int, Vector2) -> Double).scaleShiftOutput(
    scale: Double = 1.0,
    bias: Double = 0.0
): (Int, Vector2) -> Double = { seed, v ->
    this(seed, v) * scale + bias
}

fun ((Int, Double, Double) -> Double).scaleShiftOutput(
    scale: Double = 1.0,
    bias: Double = 0.0
): (Int, Double, Double) -> Double = { seed, x, y ->
    this(seed, x, y) * scale + bias
}

fun ((Int, Vector2) -> Double).withScalarInput(): (Int, Double, Double) -> Double = { seed, x, y ->
    this(seed, Vector2(x, y))
}

fun ((Int, Double, Double, Double) -> Double).withVector3Input(): (Int, Vector3) -> Double = { seed, v ->
    this(seed, v.x, v.y, v.z)
}

fun ((Int, Vector3) -> Double).withScalarInput(): (Int, Double, Double, Double) -> Double = { seed, x, y, z ->
    this(seed, Vector3(x, y, z))
}

fun ((Int, Double, Double, Double, Double) -> Double).withVector4Input(): (Int, Vector4) -> Double = { seed, v ->
    this(seed, v.x, v.y, v.z, v.w)
}

@JvmName("perturb1")
fun ((Int, Double) -> Double).perturb(distort: (Double) -> Double): (Int, Double) -> Double =
    { seed, x ->
        this(seed, distort(x))
    }

@JvmName("perturb2v")
inline fun ((Int, Vector2) -> Double).perturb(crossinline distort: (Vector2) -> Vector2): (Int, Vector2) -> Double =
    { seed, v ->
        this(seed, distort(v))
    }

@JvmName("perturb2ds")
inline fun ((Int, Double, Double) -> Double).perturb(crossinline distort: (Vector2) -> Vector2): (Int, Double, Double) -> Double =
    { seed, x, y ->
        val d = distort(Vector2(x, y))
        this(seed, d.x, d.y)
    }

@JvmName("perturb3")
inline fun ((Int, Vector3) -> Double).perturb(crossinline distort: (Vector3) -> Vector3): (Int, Vector3) -> Double =
    { seed, v ->
        this(seed, distort(v))
    }

@JvmName("perturb3ds")
inline fun ((Int, Double, Double, Double) -> Double).perturb(crossinline distort: (Vector3) -> Vector3): (Int, Double, Double, Double) -> Double =
    { seed, x, y, z ->
        val d = distort(Vector3(x, y, z))
        this(seed, d.x, d.y, d.z)
    }

@JvmName("perturb4")
inline fun ((Int, Vector4) -> Double).perturb(crossinline distort: (Vector4) -> Vector4): (Int, Vector4) -> Double =
    { seed, v ->
        this(seed, distort(v))
    }

typealias IDDD_D = ((Int, Double, Double, Double) -> Double)

