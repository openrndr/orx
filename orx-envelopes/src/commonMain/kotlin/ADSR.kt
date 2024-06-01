package org.openrndr.extra.envelopes

import org.openrndr.math.mix
import kotlin.jvm.JvmRecord
import kotlin.math.min

data class ADSR(
    val attackDuration: Double,
    val decayDuration: Double,
    val sustainValue: Double,
    val releaseDuration: Double
) : Envelope() {
    override fun value(t: Double, tOff: Double): Double {
        return adsr(attackDuration, decayDuration, sustainValue, releaseDuration, t, tOff)
    }

    override fun position(t: Double, tOff: Double): Double {
        return adsrPosition(attackDuration, decayDuration, releaseDuration, t, tOff)
    }

    override fun isActive(t: Double, tOff: Double): Boolean {
        return !(t - tOff > releaseDuration)
    }
}

fun adsr(
    attackDuration: Double,
    decayDuration: Double,
    sustainValue: Double,
    releaseDuration: Double,
    t: Double,
    tOff: Double = 1E10
): Double {
    val da = t / attackDuration
    val dc = (t - attackDuration) / decayDuration
    val vOn = mix(min(1.0, da), sustainValue, dc.coerceIn(0.0..1.0))
    return mix(vOn, 0.0, ((t - tOff) / releaseDuration).coerceIn(0.0..1.0))
}

fun adsrPosition(
    attackDuration: Double,
    decayDuration: Double,
    releaseDuration: Double,
    t: Double,
    tOff: Double
): Double {
    val ta = (t / attackDuration).coerceIn(0.0..1.0)
    val td = ((t - attackDuration) / decayDuration).coerceIn(0.0..1.0)
    val tr = ((t - tOff) / releaseDuration).coerceIn(0.0..1.0)
    return (ta + td + tr) / 3.0
}