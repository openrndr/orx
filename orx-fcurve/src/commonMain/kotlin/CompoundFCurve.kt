package org.openrndr.extra.fcurve

import org.openrndr.color.ColorRGBa
import org.openrndr.math.*

abstract class CompoundFCurve<T>(val compounds: List<FCurve?>, val compoundNames: List<String>) {
    val duration: Double
        get() {
            return compounds.maxOf { it?.duration ?: 0.0 }
        }

    abstract fun value(t: Double, overrides: Map<String, Double>? = null): T
    abstract fun sampler(normalized: Boolean = false): (Double) -> T
}

class BooleanFCurve(value: Pair<String, FCurve?>, val default: Boolean = true) :
    CompoundFCurve<Boolean>(listOf(value.second), listOf(value.first)) {
    override fun value(t: Double, overrides: Map<String, Double>?): Boolean {
        val d = overrides?.get(compoundNames[0]) ?: compounds[0]?.value(t)
        return if (d != null) {
            d >= 1.0
        } else {
            default
        }
    }

    override fun sampler(normalized: Boolean): (Double) -> Boolean {
        val sampler = compounds[0]?.sampler(normalized) ?: { if (default) 1.0 else 0.0 }
        return { t -> sampler(t) >= 1.0 }
    }
}

class DoubleFCurve(value: Pair<String, FCurve?>, val default: Double = 0.0) :
    CompoundFCurve<Double>(listOf(value.second), listOf(value.first)) {

    override fun value(t: Double, overrides: Map<String, Double>?): Double {
        return overrides?.get(compoundNames[0]) ?: compounds[0]?.value(t) ?: default
    }

    override fun sampler(normalized: Boolean): (Double) -> Double {
        val sampler = compounds[0]?.sampler(normalized) ?: { default }
        return { t -> sampler(t) }
    }
}

class IntFCurve(value: Pair<String, FCurve?>, val default: Int = 0) :
    CompoundFCurve<Int>(listOf(value.second), listOf(value.first)) {

    override fun value(t: Double, overrides: Map<String, Double>?): Int {
        val d = overrides?.get(compoundNames[0]) ?: compounds[0]?.value(t)
        return if (d != null) {
            d.toInt()
        } else {
            default
        }
    }

    override fun sampler(normalized: Boolean): (Double) -> Int {
        val sampler = compounds[0]?.sampler(normalized) ?: { default.toDouble() }
        return { t -> sampler(t).toInt() }
    }
}

class Vector2FCurve(
    x: Pair<String, FCurve?>, y: Pair<String, FCurve?>,
    val default: Vector2 = Vector2.ZERO
) :
    CompoundFCurve<Vector2>(listOf(x.second, y.second), listOf(x.first, y.first)) {
    override fun value(t: Double, overrides: Map<String, Double>?): Vector2 {
        val x = overrides?.get(compoundNames[0]) ?: compounds[0]?.value(t) ?: default.x
        val y = overrides?.get(compoundNames[1]) ?: compounds[1]?.value(t) ?: default.y
        return Vector2(x, y)
    }

    override fun sampler(normalized: Boolean): (Double) -> Vector2 {
        val xSampler = compounds[0]?.sampler(normalized) ?: { default.x }
        val ySampler = compounds[1]?.sampler(normalized) ?: { default.y }
        return { t -> Vector2(xSampler(t), ySampler(t)) }
    }
}

class Vector3FCurve(
    x: Pair<String, FCurve?>,
    y: Pair<String, FCurve?>,
    z: Pair<String, FCurve?>,
    val default: Vector3 = Vector3.ZERO
) :
    CompoundFCurve<Vector3>(listOf(x.second, y.second, z.second), listOf(x.first, y.first, z.first)) {
    override fun value(t: Double, overrides: Map<String, Double>?): Vector3 {
        val x = overrides?.get(compoundNames[0]) ?: compounds[0]?.value(t) ?: default.x
        val y = overrides?.get(compoundNames[1]) ?: compounds[1]?.value(t) ?: default.y
        val z = overrides?.get(compoundNames[2]) ?: compounds[2]?.value(t) ?: default.z
        return Vector3(x, y, z)
    }

    override fun sampler(normalized: Boolean): (Double) -> Vector3 {
        val xSampler = compounds[0]?.sampler(normalized) ?: { default.x }
        val ySampler = compounds[1]?.sampler(normalized) ?: { default.y }
        val zSampler = compounds[2]?.sampler(normalized) ?: { default.z }
        return { t -> Vector3(xSampler(t), ySampler(t), zSampler(t)) }
    }
}

class Vector4FCurve(
    x: Pair<String, FCurve?>,
    y: Pair<String, FCurve?>,
    z: Pair<String, FCurve?>,
    w: Pair<String, FCurve?>,
    val default: Vector4 = Vector4.ZERO
) :
    CompoundFCurve<Vector4>(
        listOf(x.second, y.second, z.second, w.second),
        listOf(x.first, y.first, z.first, w.first)
    ) {

    override fun value(t: Double, overrides: Map<String, Double>?): Vector4 {
        val x = overrides?.get(compoundNames[0]) ?: compounds[0]?.value(t) ?: default.x
        val y = overrides?.get(compoundNames[1]) ?: compounds[1]?.value(t) ?: default.y
        val z = overrides?.get(compoundNames[2]) ?: compounds[2]?.value(t) ?: default.z
        val w = overrides?.get(compoundNames[3]) ?: compounds[3]?.value(t) ?: default.w
        return Vector4(x, y, z, w)
    }

    override fun sampler(normalized: Boolean): (Double) -> Vector4 {
        val xSampler = compounds[0]?.sampler(normalized) ?: { default.x }
        val ySampler = compounds[1]?.sampler(normalized) ?: { default.y }
        val zSampler = compounds[2]?.sampler(normalized) ?: { default.z }
        val wSampler = compounds[3]?.sampler(normalized) ?: { default.w }
        return { t -> Vector4(xSampler(t), ySampler(t), zSampler(t), wSampler(t)) }
    }
}

class RgbFCurve(
    r: Pair<String, FCurve?>,
    g: Pair<String, FCurve?>,
    b: Pair<String, FCurve?>,
    val default: ColorRGBa = ColorRGBa.WHITE
) :
    CompoundFCurve<ColorRGBa>(listOf(r.second, g.second, b.second), listOf(r.first, g.first, b.first)) {

    override fun value(t: Double, overrides: Map<String, Double>?): ColorRGBa {
        val r = overrides?.get(compoundNames[0]) ?: compounds[0]?.value(t) ?: default.r
        val g = overrides?.get(compoundNames[1]) ?: compounds[1]?.value(t) ?: default.g
        val b = overrides?.get(compoundNames[2]) ?: compounds[2]?.value(t) ?: default.g
        return ColorRGBa(r, g, b)
    }

    override fun sampler(normalized: Boolean): (Double) -> ColorRGBa {
        val rSampler = compounds[0]?.sampler(normalized) ?: { default.r }
        val gSampler = compounds[1]?.sampler(normalized) ?: { default.g }
        val bSampler = compounds[2]?.sampler(normalized) ?: { default.b }
        return { t -> ColorRGBa(rSampler(t), gSampler(t), bSampler(t)) }
    }
}

class RgbaFCurve(
    r: Pair<String, FCurve?>,
    g: Pair<String, FCurve?>,
    b: Pair<String, FCurve?>,
    a: Pair<String, FCurve?>,
    val default: ColorRGBa = ColorRGBa.WHITE
) :
    CompoundFCurve<ColorRGBa>(
        listOf(r.second, g.second, b.second, a.second),
        listOf(r.first, g.first, b.first, a.first)
    ) {

    override fun value(t: Double, overrides: Map<String, Double>?): ColorRGBa {
        val r = overrides?.get(compoundNames[0]) ?: compounds[0]?.value(t) ?: default.r
        val g = overrides?.get(compoundNames[1]) ?: compounds[1]?.value(t) ?: default.g
        val b = overrides?.get(compoundNames[2]) ?: compounds[2]?.value(t) ?: default.g
        val a = overrides?.get(compoundNames[3]) ?: compounds[3]?.value(t) ?: default.alpha
        return ColorRGBa(r, g, b, a)
    }

    override fun sampler(normalized: Boolean): (Double) -> ColorRGBa {
        val rSampler = compounds[0]?.sampler(normalized) ?: { default.r }
        val gSampler = compounds[1]?.sampler(normalized) ?: { default.g }
        val bSampler = compounds[2]?.sampler(normalized) ?: { default.b }
        val aSampler = compounds[3]?.sampler(normalized) ?: { default.alpha }
        return { t -> ColorRGBa(rSampler(t), gSampler(t), bSampler(t), aSampler(t)) }
    }
}

class PolarFCurve(
    angleInDegrees: Pair<String, FCurve?>,
    radius: Pair<String, FCurve?>,
    val default: Polar = Polar(0.0, 1.0)
) :
    CompoundFCurve<Polar>(listOf(angleInDegrees.second, radius.second), listOf(angleInDegrees.first, radius.first)) {

    override fun value(t: Double, overrides: Map<String, Double>?): Polar {
        val theta = overrides?.get(compoundNames[0]) ?: compounds[0]?.value(t) ?: default.theta
        val radius = overrides?.get(compoundNames[1]) ?: compounds[1]?.value(t) ?: default.radius
        return Polar(theta, radius)
    }

    override fun sampler(normalized: Boolean): (Double) -> Polar {
        val angleSampler = compounds[0]?.sampler(normalized) ?: { default.theta }
        val radiusSampler = compounds[1]?.sampler(normalized) ?: { default.radius }
        return { t -> Polar(angleSampler(t), radiusSampler(t)) }
    }
}

class SphericalFCurve(
    thetaInDegrees: Pair<String, FCurve?>,
    phiInDegrees: Pair<String, FCurve?>,
    radius: Pair<String, FCurve?>,
    val default: Spherical = Spherical(0.0, 1.0, 1.0)
) :
    CompoundFCurve<Spherical>(
        listOf(thetaInDegrees.second, phiInDegrees.second, radius.second),
        listOf(thetaInDegrees.first, phiInDegrees.first, radius.first)
    ) {
    override fun value(t: Double, overrides: Map<String, Double>?): Spherical {
        val theta = overrides?.get(compoundNames[0]) ?: compounds[0]?.value(t) ?: default.theta
        val phi = overrides?.get(compoundNames[1]) ?: compounds[1]?.value(t) ?: default.phi
        val radius = overrides?.get(compoundNames[2]) ?: compounds[2]?.value(t) ?: default.radius
        return Spherical(theta, phi, radius)
    }

    override fun sampler(normalized: Boolean): (Double) -> Spherical {
        val thetaSampler = compounds[0]?.sampler(normalized) ?: { default.theta }
        val phiSampler = compounds[0]?.sampler(normalized) ?: { default.theta }
        val radiusSampler = compounds[2]?.sampler(normalized) ?: { default.radius }
        return { t -> Spherical(thetaSampler(t), phiSampler(t), radiusSampler(t)) }
    }
}

