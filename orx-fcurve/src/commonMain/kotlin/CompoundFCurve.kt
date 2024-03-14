package org.openrndr.extra.fcurve

import org.openrndr.color.ColorRGBa
import org.openrndr.math.*

abstract class CompoundFCurve<T>(val compounds: List<FCurve?>) {
    val duration: Double
        get() {
            return compounds.maxOf { it?.duration ?: 0.0 }
        }

    abstract fun sampler(normalized: Boolean = false): (Double) -> T
}

class BooleanFCurve(value: FCurve?, val default: Boolean = true) :
    CompoundFCurve<Boolean>(listOf(value)) {
    override fun sampler(normalized: Boolean): (Double) -> Boolean {
        val sampler = compounds[0]?.sampler(normalized) ?: { if (default) 1.0 else 0.0 }
        return { t -> sampler(t) >= 1.0 }
    }
}

class DoubleFCurve(value: FCurve?, val default: Double = 0.0) :
    CompoundFCurve<Double>(listOf(value)) {
    override fun sampler(normalized: Boolean): (Double) -> Double {
        val sampler = compounds[0]?.sampler(normalized) ?: { default }
        return { t -> sampler(t) }
    }
}

class IntFCurve(value: FCurve?, val default: Int = 0) :
    CompoundFCurve<Int>(listOf(value)) {
    override fun sampler(normalized: Boolean): (Double) -> Int {
        val sampler = compounds[0]?.sampler(normalized) ?: { default.toDouble() }
        return { t -> sampler(t).toInt() }
    }
}

class Vector2FCurve(x: FCurve?, y: FCurve?, val default: Vector2 = Vector2.ZERO) :
    CompoundFCurve<Vector2>(listOf(x, y)) {
    override fun sampler(normalized: Boolean): (Double) -> Vector2 {
        val xSampler = compounds[0]?.sampler(normalized) ?: { default.x }
        val ySampler = compounds[1]?.sampler(normalized) ?: { default.y }
        return { t -> Vector2(xSampler(t), ySampler(t)) }
    }
}

class Vector3FCurve(x: FCurve?, y: FCurve?, z: FCurve?, val default: Vector3 = Vector3.ZERO) :
    CompoundFCurve<Vector3>(listOf(x, y, z)) {
    override fun sampler(normalized: Boolean): (Double) -> Vector3 {
        val xSampler = compounds[0]?.sampler(normalized) ?: { default.x }
        val ySampler = compounds[1]?.sampler(normalized) ?: { default.y }
        val zSampler = compounds[2]?.sampler(normalized) ?: { default.z }
        return { t -> Vector3(xSampler(t), ySampler(t), zSampler(t)) }
    }
}

class Vector4FCurve(x: FCurve?, y: FCurve?, z: FCurve?, w: FCurve?, val default: Vector4 = Vector4.ZERO) :
    CompoundFCurve<Vector4>(listOf(x, y, z, w)) {
    override fun sampler(normalized: Boolean): (Double) -> Vector4 {
        val xSampler = compounds[0]?.sampler(normalized) ?: { default.x }
        val ySampler = compounds[1]?.sampler(normalized) ?: { default.y }
        val zSampler = compounds[2]?.sampler(normalized) ?: { default.z }
        val wSampler = compounds[3]?.sampler(normalized) ?: { default.w }
        return { t -> Vector4(xSampler(t), ySampler(t), zSampler(t), wSampler(t)) }
    }
}

class RgbFCurve(r: FCurve?, g: FCurve?, b: FCurve?, val default: ColorRGBa = ColorRGBa.WHITE) :
    CompoundFCurve<ColorRGBa>(listOf(r, g, b)) {
    override fun sampler(normalized: Boolean): (Double) -> ColorRGBa {
        val rSampler = compounds[0]?.sampler(normalized) ?: { default.r }
        val gSampler = compounds[1]?.sampler(normalized) ?: { default.g }
        val bSampler = compounds[2]?.sampler(normalized) ?: { default.b }
        return { t -> ColorRGBa(rSampler(t), gSampler(t), bSampler(t)) }
    }
}

class RgbaFCurve(r: FCurve?, g: FCurve?, b: FCurve?, a: FCurve?, val default: ColorRGBa = ColorRGBa.WHITE) :
    CompoundFCurve<ColorRGBa>(listOf(r, g, b, a)) {
    override fun sampler(normalized: Boolean): (Double) -> ColorRGBa {
        val rSampler = compounds[0]?.sampler(normalized) ?: { default.r }
        val gSampler = compounds[1]?.sampler(normalized) ?: { default.g }
        val bSampler = compounds[2]?.sampler(normalized) ?: { default.b }
        val aSampler = compounds[3]?.sampler(normalized) ?: { default.alpha }
        return { t -> ColorRGBa(rSampler(t), gSampler(t), bSampler(t), aSampler(t)) }
    }
}

class PolarFCurve(angleInDegrees: FCurve?, radius: FCurve?, val default: Polar = Polar(0.0, 1.0)) :
    CompoundFCurve<Polar>(listOf(angleInDegrees, radius)) {
    override fun sampler(normalized: Boolean): (Double) -> Polar {
        val angleSampler = compounds[0]?.sampler(normalized) ?: { default.theta }
        val radiusSampler = compounds[1]?.sampler(normalized) ?: { default.radius }
        return { t -> Polar(angleSampler(t), radiusSampler(t)) }
    }
}

class SphericalFCurve(
    thetaInDegrees: FCurve?,
    phiInDegrees: FCurve?,
    radius: FCurve?,
    val default: Spherical = Spherical(0.0, 1.0, 1.0)
) :
    CompoundFCurve<Spherical>(listOf(thetaInDegrees, phiInDegrees, radius)) {
    override fun sampler(normalized: Boolean): (Double) -> Spherical {
        val thetaSampler = compounds[0]?.sampler(normalized) ?: { default.theta }
        val phiSampler = compounds[0]?.sampler(normalized) ?: { default.theta }
        val radiusSampler = compounds[2]?.sampler(normalized) ?: { default.radius }
        return { t -> Spherical(thetaSampler(t), phiSampler(t), radiusSampler(t)) }
    }
}

