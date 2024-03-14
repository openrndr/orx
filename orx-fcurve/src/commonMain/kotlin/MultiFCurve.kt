package org.openrndr.extra.fcurve
import org.openrndr.color.ColorRGBa
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3

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


open class MultiFCurve(val compounds: Map<String, FCurve?>) {
    fun changeSpeed(speed: Double): MultiFCurve {
        if (speed == 1.0) {
            return this
        } else {
            return MultiFCurve(compounds.mapValues { it.value?.changeSpeed(speed) })
        }
    }

    val duration by lazy { compounds.values.maxOfOrNull { it?.duration ?: 0.0 } ?: 0.0 }
    operator fun get(name: String): FCurve? {
        return compounds[name]
    }

    fun boolean(value: String, default: Boolean = true) = BooleanFCurve(this[value], default)
    fun double(value: String, default: Double = 0.0) = DoubleFCurve(this[value], default)

    fun int(value: String, default: Int = 0) = IntFCurve(this[value], default)

    fun vector2(x: String, y: String, default: Vector2 = Vector2.ZERO) = Vector2FCurve(this[x], this[y], default)
    fun vector3(x: String, y: String, z: String, default: Vector3 = Vector3.ZERO) =
        Vector3FCurve(this[x], this[y], this[z], default)

    fun rgb(r: String, g: String, b: String, default: ColorRGBa = ColorRGBa.WHITE) =
        RgbFCurve(this[r], this[g], this[b], default)

    fun rgba(r: String, g: String, b: String, a: String, default: ColorRGBa = ColorRGBa.WHITE) =
        RgbaFCurve(this[r], this[g], this[b], this[a], default)
}

