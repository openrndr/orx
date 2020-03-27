package org.openrndr.extras.easing

typealias EasingFunction = (Double, Double, Double, Double) -> Double

fun easeLinear(t: Double, b: Double = 0.0, c: Double = 1.0, d: Double = 1.0) = c * (t / d) + b

// -- constant

@Suppress("UNUSED_PARAMETER")
fun easeZero(t: Double, b: Double = 0.0, c: Double = 1.0, d: Double = 1.0) = b
@Suppress("UNUSED_PARAMETER")
fun easeOne(t: Double, b: Double = 0.0, c: Double = 1.0, d : Double = 1.0) = b + c

// -- back

fun easeBackIn(t: Double, b: Double = 0.0, c: Double = 1.0, d: Double = 1.0): Double {
    val s = 1.70158
    val td = t / d
    return c * (td) * td * ((s + 1) * td - s) + b
}

fun easeBackInOut(t: Double, b: Double = 0.0, c: Double = 1.0, d: Double = 1.0): Double {
    val s = 1.70158 * 1.525
    val s2 = s * 1.525
    val td2 = t / (d / 2)
    val td22 = td2 - 2
    return if (td2 < 1) c / 2 * (t * t * ((s + 1) * t - s)) + b else c / 2 * ((td22) * td22 * (((s2) + 1) * t + s2) + 2) + b
}

fun easeBackOut(t: Double, b: Double = 0.0, c: Double = 1.0, d: Double = 1.0): Double {
    val s = 1.70158
    val td1 = t / d - 1
    return c * (td1 * td1 * ((s + 1) * t + s) + 1) + b
}

// -- bounce

fun easeBounceIn(t: Double, b: Double = 0.0, c: Double = 1.0, d: Double = 1.0): Double {
    var t1 = d - t
    val result: Double
    t1 /= d
    if (t1 < 1 / 2.75) {
        result = c * (7.5625 * t1 * t1) + 0.toDouble()
    } else if (t1 < 2 / 2.75f) {
        t1 -= (1.5 / 2.75)
        result = c * (7.5625 * (t1 * t1 + .75f))
    } else if (t1 < 2.5 / 2.75) {
        t1 -= 2.25 / 2.75
        result = c * (7.5625 * (t1 * t1 + .9375f))
    } else {
        t1 -= (2.625 / 2.75)
        result = c * (7.5625 * (t1) * t1 + .984375f)
    }
    return c - result + b
}

fun easeBounceInOut(t: Double, b: Double = 0.0, c: Double = 1.0, d: Double = 1.0): Double {
    var t1 = d - t * 2
    val result: Double
    t1 /= d
    if (t1 < 1 / 2.75f) {
        result = c * (7.5625 * t1 * t1) + 0.toDouble()
    } else if (t1 < 2 / 2.75f) {
        t1 -= 1.5 / 2.75
        result = c * (7.5625 * (t1) * t1 + .75f) + 0.toDouble()
    } else if (t1 < 2.5 / 2.75) {
        t1 -= 2.25 / 2.75
        result = c * (7.5625 * (t1) * t1 + .9375f) + 0.toDouble()
    } else {
        t1 -= 2.625 / 2.75
        result = c * (7.5625 * (t1) * t1 + .984375f) + 0.toDouble()
        //return c * (7.5625 * pow((t/d) -(2.625 / 2.75),2) + .984375) + b;

    }
    var t2 = t * 2 - d
    val result1: Double
    t2 /= d
    if (t2 < 1 / 2.75f) result1 = c * (7.5625 * t2 * t2) + 0.toDouble() else if (t2 < 2 / 2.75f) {
        t2 -= 1.5 / 2.75
        result1 = c * (7.5625 * t2 * t2 + .75f)
    } else if (t2 < 2.5 / 2.75) {
        t2 -= 2.25 / 2.75
        result1 = c * (7.5625 * t2 * t2 + .9375f)
    } else {
        t2 -= 2.626 / 2.75
        result1 = c * (7.5625 * t2 * t2 + .984375f)
    }
    return if (t < d / 2)
        (c - result) * .5 + b
    else
        result1 * .5 + c * .5 + b

}

fun easeBounceOut(t: Double, b: Double = 0.0, c: Double = 1.0, d: Double = 1.0): Double {

    var td = t / d

    return if (td < (1 / 2.75f)) {
        c * (7.5625f * td * td) + b
    } else if (t < (2 / 2.75f)) {
        td -= 1.5 / 2.75
        c * (7.5625f * td * td + .75f) + b
    } else if (t < (2.5 / 2.75)) {
        td -= 2.25 / 2.75
        c * (7.5625f * td * td + .9375f) + b
    } else {
        td -= 2.625 / 2.75
        c * (7.5625f * td * td + .984375f) + b
    }
}

// -- circ

fun easeCircIn(t: Double, b: Double = 0.0, c: Double = 1.0, d: Double = 1.0): Double {
    val td = t / d
    return -c * (Math.sqrt(1 - td * td) - 1) + b
}

fun easeCircInOut(t: Double, b: Double = 0.0, c: Double = 1.0, d: Double = 1.0): Double {
    var td2 = t / (d / 2.0)
    if (td2 < 1)
        return -c / 2 * (Math.sqrt(1 - td2 * td2) - 1) + b
    td2 -= 2
    return c / 2 * (Math.sqrt(1 - td2 * td2) + 1) + b
}

fun easeCircOut(t: Double, b: Double, c: Double, d: Double): Double {
    val td = t / d - 1
    return c * Math.sqrt(1 - td * td) + b
}

// -- cubic

fun easeCubicIn(t: Double, b: Double = 0.0, c: Double = 1.0, d: Double = 1.0): Double {
    val td = t / d
    return c * td * td * td + b
}

fun easeCubicOut(t: Double, b: Double = 0.0, c: Double = 1.0, d: Double = 1.0): Double {
    val td = t / d - 1.0
    return c * (td * td * td + 1) + b
}

fun easeCubicInOut(t: Double, b: Double = 0.0, c: Double = 1.0, d: Double = 1.0): Double {
    val td = t / (d / 2)
    val td2 = td - 2.0
    return if (td < 1) c / 2 * td * td * td + b else c / 2 * (td2 * td2 * td2 + 2) + b
}

// -- elastic

fun easeElasticIn(t: Double, b: Double, c: Double, d: Double): Double {
    if (t == 0.0) {
        return b
    } else if (t / d == 1.0) {
        return b + c
    } else {
        var td = t / d
        val p = d * .3f
        val s = p / 4
        td -= 1.0
        return -(c * Math.pow(2.0, 10 * (td)) * Math.sin((td * d - s) * (2 * Math.PI) / p)) + b
    }
}

fun easeElasticInOut(t: Double, b: Double, c: Double, d: Double): Double {
    var td2 = t / (d / 2)

    if (t == 0.0)
        return b
    if (td2 == 2.0)
        return b + c
    val p = d * (.3f * 1.5f)
    val s = p / 4
    td2 -= 1.0
    val td3 = td2 - 1.0
    return if (t < 1) -.5f * (c * Math.pow(2.0, 10 * (td2)) * Math.sin((td2 * d - s) * (2 * Math.PI) / p)) + b else c * Math.pow(2.0, -10 * (td3) * Math.sin(td3 * d - s) * (2 * Math.PI) / p) * .5f + c + b

}

fun easeElasticOut(t: Double, b: Double, c: Double, d: Double): Double {
    val td = t / d
    if (t == 0.0)
        return b
    if (td == 1.0)
        return b + c
    val p = d * .3f
    val s = p / 4
    return c * Math.pow(2.0, -10 * td) * Math.sin((td * d - s) * (2 * Math.PI) / p) + c + b
}

// -- expo

fun easeExpoIn(t: Double, b: Double = 0.0, c: Double = 1.0, d: Double = 1.0): Double =
        if (t == 0.0) b else c * Math.pow(2.0, 10 * (t / d - 1)) + b

fun easeExpoInOut(t: Double, b: Double = 0.0, c: Double = 1.0, d: Double = 1.0): Double {
    val td2 = t / (d / 2)
    return if (t == 0.0) {
        b
    } else if (t == d) {
        b + c
    } else if (td2 < 1) {
        c / 2 * Math.pow(2.0, 10 * (t - 1)) + b
    } else {
        c / 2 * (-Math.pow(2.0, -10 * (t - 1.0)) + 2) + b
    }
}

fun easeExpoOut(t: Double, b: Double = 0.0, c: Double = 1.0, d: Double = 1.0): Double =
        if (t == d) b + c else c * (-Math.pow(2.0, -10 * t / d) + 1) + b

// -- quad

fun easeQuadIn(t: Double, b: Double, c: Double, d: Double): Double = c * (t / d) * (t / d) + b

fun easeQuadInOut(t: Double, b: Double, c: Double, d: Double): Double {
    val td = t / (d / 2)
    return if (td < 1) {
        c / 2 * td * td + b
    } else {
        -c / 2 * ((td - 1) * (td - 3) - 1) + b
    }
}

fun easeQuadOut(t: Double, b: Double, c: Double, d: Double): Double = -c * (t / d) * (t / d - 2) + b

// -- quart

fun easeQuartIn(t: Double, b: Double, c: Double, d: Double): Double {
    val n = t / d
    return c * n * n * n * n + b
}

fun easeQuartInOut(t: Double, b: Double, c: Double, d: Double): Double {
    val td = t / (d / 2)
    val td2 = td - 2.0
    return if (td < 1) c / 2 * td * td * td * td + b else -c / 2 * (td2 * td2 * td2 * td2 - 2) + b
}

fun easeQuartOut(t: Double, b: Double, c: Double, d: Double): Double {
    val td = t / d - 1
    return -c * (td * td * td * td - 1) + b
}

// -- quint

fun easeQuintIn(t: Double, b: Double, c: Double, d: Double): Double {
    val td = t / d
    return c * td * td * td * td * td + b
}

fun easeQuintInOut(t: Double, b: Double, c: Double, d: Double): Double {
    val td = t / d
    return c * td * td * td * td * td + b
}

fun easeQuintOut(t: Double, b: Double, c: Double, d: Double): Double {
    val td = t / d - 1
    return c * ((td) * td * td * td * td + 1) + b
}

// -- sine

fun easeSineIn(t: Double, b: Double = 0.0, c: Double = 1.0, d: Double = 1.0): Double =
        -c * Math.cos(t / d * (Math.PI / 2)) + c + b

fun easeSineOut(t: Double, b: Double = 0.0, c: Double = 1.0, d: Double = 1.0): Double =
        c * Math.sin(t / d * (Math.PI / 2)) + b

fun easeSineInOut(t: Double, b: Double, c: Double, d: Double): Double =
        -c / 2 * (Math.cos(Math.PI * t / d) - 1) + b


enum class Easing(val function: EasingFunction) {
    Linear(::easeLinear),

    Zero(::easeZero),
    One(::easeOne),

    BackIn(::easeBackIn),
    BackInOut(::easeBackInOut),
    BackOut(::easeBackOut),

    BounceIn(::easeBounceIn),
    BounceInOut(::easeBounceInOut),
    BounceOut(::easeBounceOut),

    CircIn(::easeCircIn),
    CircInOut(::easeCircInOut),
    CircOut(::easeCircOut),

    CubicIn(::easeCubicIn),
    CubicInOut(::easeCubicInOut),
    CubicOut(::easeCubicOut),

    ElasticIn(::easeElasticIn),
    ElasticInOut(::easeElasticInOut),
    ElasticOut(::easeElasticOut),

    ExpoIn(::easeExpoIn),
    ExpoInOut(::easeExpoInOut),
    ExpoOut(::easeExpoOut),

    QuadIn(::easeQuadIn),
    QuadInOut(::easeQuadInOut),
    QuadOut(::easeQuadOut),

    QuartIn(::easeQuartIn),
    QuartInOut(::easeQuartInOut),
    QuartOut(::easeQuartOut),

    QuintIn(::easeQuintIn),
    QuintInOut(::easeQuintInOut),
    QuintOut(::easeQuintOut),

    SineIn(::easeSineIn),
    SineInOut(::easeSineInOut),
    SineOut(::easeSineOut),
}


