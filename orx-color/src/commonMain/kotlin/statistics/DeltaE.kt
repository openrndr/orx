package org.openrndr.extra.color.statistics

import org.openrndr.color.ColorLABa
import org.openrndr.color.ConvertibleToColorRGBa
import org.openrndr.math.Vector3
import org.openrndr.math.asDegrees
import org.openrndr.math.asRadians
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Computes the CIE76 color difference (ΔE*76) between this color and another color.
 * The method calculates the Euclidean distance between the two colors in the LAB color space.
 * If either of the colors is not in LAB format, it will be converted to LAB before computation.
 *
 * @param other The second color to compare, which should implement the ConvertibleToColorRGBa interface.
 * @return The calculated CIE76 color difference as a Double.
 */
fun <T: ConvertibleToColorRGBa> T.deltaE76(other: T): Double {
    return if (this is ColorLABa && other is ColorLABa) {
        val tv = Vector3(l, a, b)
        val ov = Vector3(other.l, other.a, other.b)
        tv.distanceTo(ov)
    } else {
        val tLab = if (this is ColorLABa) this else this.toRGBa().toLABa()
        val oLab = if (other is ColorLABa) other else other.toRGBa().toLABa()
        tLab.deltaE76(oLab)
    }
}

/**
 * Computes the CIEDE2000 color difference (ΔE*00) between this color and another color, as
 * defined in Sharma, Wu & Dalal's "The CIEDE2000 Color-Difference Formula: Implementation
 * Notes, Supplementary Test Data, and Mathematical Observations" (2005). Unlike [deltaE76]'s
 * plain Euclidean distance, CIEDE2000 corrects for LAB's known perceptual non-uniformities --
 * weighting lightness/chroma/hue differences by where in the space they fall, and adding a
 * hue-rotation term for the blue region. So it tracks perceived difference much more closely,
 * at the cost of being considerably more involved to compute.
 *
 * If either of the colors is not in LAB format, it will be converted to LAB before computation.
 *
 * @param other The second color to compare, which should implement the ConvertibleToColorRGBa interface.
 * @return The calculated CIEDE2000 color difference as a Double.
 */
fun <T : ConvertibleToColorRGBa> T.deltaE2000(other: T): Double {
    val tLab = if (this is ColorLABa) this else this.toRGBa().toLABa()
    val oLab = if (other is ColorLABa) other else other.toRGBa().toLABa()
    return tLab.deltaE2000(oLab)
}

/**
 * Computes the CIEDE2000 color difference (ΔE*00) between two [ColorLABa] colors. See the
 * `T.deltaE2000` overload above for details on the formula.
 */
fun ColorLABa.deltaE2000(other: ColorLABa): Double {
    // Naming and structure follow Sharma, Wu & Dalal (2005) as closely as Kotlin identifiers
    // allow, so this can be checked line-by-line against the paper -- primed (') becomes an
    // `p` suffix, bar (‾) becomes `avg`.

    val c1 = sqrt(a * a + b * b)
    val c2 = sqrt(other.a * other.a + other.b * other.b)
    val cAvg = (c1 + c2) / 2.0

    // G corrects a* alone (not b*, not L*) towards 0 as average chroma drops -- LAB's a*/b*
    // plane overstates the perceptual difference between two near-neutral colors that differ
    // mainly along a*, and this is pure curve-fitting to compensate, with no geometric meaning
    // of its own (hence the 25^7 magic constant, chosen to fit perceptual test data).
    val g = 0.5 * (1.0 - sqrt(cAvg.pow(7) / (cAvg.pow(7) + 25.0.pow(7))))

    val a1p = a * (1.0 + g)
    val a2p = other.a * (1.0 + g)
    val c1p = sqrt(a1p * a1p + b * b)
    val c2p = sqrt(a2p * a2p + other.b * other.b)

    // hue' is explicitly defined as 0 when a'/b* are both 0 -- atan2(0, 0) is otherwise fine
    // (most platforms return 0.0 already), but this keeps that a documented choice, not an
    // accident of atan2's implementation.
    fun huePrime(ap: Double, bValue: Double): Double =
        if (ap == 0.0 && bValue == 0.0) 0.0 else atan2(bValue, ap).asDegrees.let { if (it < 0.0) it + 360.0 else it }

    val h1p = huePrime(a1p, b)
    val h2p = huePrime(a2p, other.b)

    val deltaLp = other.l - l
    val deltaCp = c2p - c1p

    val deltahp = when {
        c1p * c2p == 0.0 -> 0.0
        abs(h2p - h1p) <= 180.0 -> h2p - h1p
        h2p - h1p > 180.0 -> h2p - h1p - 360.0
        else -> h2p - h1p + 360.0
    }
    val deltaHp = 2.0 * sqrt(c1p * c2p) * sin((deltahp / 2.0).asRadians)

    val lAvgp = (l + other.l) / 2.0
    val cAvgp = (c1p + c2p) / 2.0

    val hAvgp = when {
        c1p * c2p == 0.0 -> h1p + h2p
        abs(h1p - h2p) <= 180.0 -> (h1p + h2p) / 2.0
        h1p + h2p < 360.0 -> (h1p + h2p + 360.0) / 2.0
        else -> (h1p + h2p - 360.0) / 2.0
    }

    val t = 1.0 -
        0.17 * cos((hAvgp - 30.0).asRadians) +
        0.24 * cos((2.0 * hAvgp).asRadians) +
        0.32 * cos((3.0 * hAvgp + 6.0).asRadians) -
        0.20 * cos((4.0 * hAvgp - 63.0).asRadians)

    val deltaTheta = 30.0 * exp(-((hAvgp - 275.0) / 25.0).pow(2))
    val rc = 2.0 * sqrt(cAvgp.pow(7) / (cAvgp.pow(7) + 25.0.pow(7)))
    val sl = 1.0 + (0.015 * (lAvgp - 50.0).pow(2)) / sqrt(20.0 + (lAvgp - 50.0).pow(2))
    val sc = 1.0 + 0.045 * cAvgp
    val sh = 1.0 + 0.015 * cAvgp * t
    val rt = -sin((2.0 * deltaTheta).asRadians) * rc

    val lTerm = deltaLp / sl
    val cTerm = deltaCp / sc
    val hTerm = deltaHp / sh

    return sqrt(lTerm * lTerm + cTerm * cTerm + hTerm * hTerm + rt * cTerm * hTerm)
}

