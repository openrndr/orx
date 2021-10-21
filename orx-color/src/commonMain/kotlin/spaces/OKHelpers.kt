package org.openrndr.extra.color.spaces


import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

// verbatim copies of https://github.com/bottosson/bottosson.github.io/blob/master/misc/colorpicker/colorconversion.js

internal fun max(a: Double, b: Double, c: Double, d: Double): Double {
    return max(max(a, b), max(c, d))
}


fun toe(x: Double): Double {
    val k_1 = 0.206
    val k_2 = 0.03
    val k_3 = (1 + k_1) / (1 + k_2)

    return 0.5 * (k_3 * x - k_1 + sqrt((k_3 * x - k_1) * (k_3 * x - k_1) + 4 * k_2 * k_3 * x))
}

fun toe_inv(x: Double): Double {
    val k_1 = 0.206
    val k_2 = 0.03
    val k_3 = (1 + k_1) / (1 + k_2)
    return (x * x + k_1 * x) / (k_3 * (x + k_2))
}

internal fun compute_max_saturation(a: Double, b: Double): Double {
    // Max saturation will be when one of r, g or b goes below zero.

    // Select different coefficients depending on which component goes below zero first
    val k0: Double
    val k1: Double
    val k2: Double
    val k3: Double
    val k4: Double
    val wl: Double
    val wm: Double
    val ws: Double

    if (-1.88170328 * a - 0.80936493 * b > 1) {
        // Red component
        k0 = +1.19086277; k1 = +1.76576728; k2 = +0.59662641; k3 = +0.75515197; k4 = +0.56771245
        wl = +4.0767416621; wm = -3.3077115913; ws = +0.2309699292
    } else if (1.81444104 * a - 1.19445276 * b > 1) {
        // Green component
        k0 = +0.73956515; k1 = -0.45954404; k2 = +0.08285427; k3 = +0.12541070; k4 = +0.14503204
        wl = -1.2684380046; wm = +2.6097574011; ws = -0.3413193965
    } else {
        // Blue component
        k0 = +1.35733652; k1 = -0.00915799; k2 = -1.15130210; k3 = -0.50559606; k4 = +0.00692167
        wl = -0.0041960863; wm = -0.7034186147; ws = +1.7076147010
    }

    // Approximate max saturation using a polynomial:
    val S = k0 + k1 * a + k2 * b + k3 * a * a + k4 * a * b

    // Do one step Halley's method to get closer
    // this gives an error less than 10e6, except for some blue hues where the dS/dh is close to infinite
    // this should be sufficient for most applications, otherwise do two/three steps

    val k_l = +0.3963377774 * a + 0.2158037573 * b
    val k_m = -0.1055613458 * a - 0.0638541728 * b
    val k_s = -0.0894841775 * a - 1.2914855480 * b

    return run {
        val l_ = 1 + S * k_l
        val m_ = 1 + S * k_m
        val s_ = 1 + S * k_s

        val l = l_ * l_ * l_
        val m = m_ * m_ * m_
        val s = s_ * s_ * s_

        val l_dS = 3 * k_l * l_ * l_
        val m_dS = 3 * k_m * m_ * m_
        val s_dS = 3 * k_s * s_ * s_

        val l_dS2 = 6 * k_l * k_l * l_
        val m_dS2 = 6 * k_m * k_m * m_
        val s_dS2 = 6 * k_s * k_s * s_

        val f = wl * l + wm * m + ws * s
        val f1 = wl * l_dS + wm * m_dS + ws * s_dS
        val f2 = wl * l_dS2 + wm * m_dS2 + ws * s_dS2

        S - f * f1 / (f1 * f1 - 0.5 * f * f2)
    }
}

internal fun find_cusp(a: Double, b: Double): DoubleArray {
    // First, find the maximum saturation (saturation S = C/L)
    val S_cusp = compute_max_saturation(a, b)
    val rgb_at_max = ColorOKLABa(1.0, S_cusp * a, S_cusp * b).toRGBa().toLinear()
    val L_cusp = (1.0 / max(max(rgb_at_max.r, rgb_at_max.g), rgb_at_max.b)).pow(1.0 / 3.0)
    val C_cusp = L_cusp * S_cusp

    return doubleArrayOf(L_cusp, C_cusp)
}

internal fun get_ST_max(a: Double, b: Double, cusp: DoubleArray? = null): DoubleArray {
    val cusp = cusp ?: find_cusp(a, b)

    val L = cusp[0]
    val C = cusp[1]
    return doubleArrayOf(C / L, C / (1.0 - L))
}

fun get_ST_mid(a_: Double, b_: Double): DoubleArray {
    val S = 0.11516993 + 1 / (
            +7.44778970 + 4.15901240 * b_
                    + a_ * (-2.19557347 + 1.75198401 * b_
                    + a_ * (-2.13704948 - 10.02301043 * b_
                    + a_ * (-4.24894561 + 5.38770819 * b_ + 4.69891013 * a_
                    )))
            )

    val T = 0.11239642 + 1 / (
            +1.61320320 - 0.68124379 * b_
                    + a_ * (+0.40370612 + 0.90148123 * b_
                    + a_ * (-0.27087943 + 0.61223990 * b_
                    + a_ * (+0.00299215 - 0.45399568 * b_ - 0.14661872 * a_
                    )))
            )

    return doubleArrayOf(S, T)
}

fun get_Cs(L: Double, a_: Double, b_: Double): DoubleArray {
    val cusp = find_cusp(a_, b_)

    val C_max = find_gamut_intersection(a_, b_, L, 1.0, L, cusp)
    val ST_max = get_ST_max(a_, b_, cusp)

    val S_mid = 0.11516993 + 1 / (
            +7.44778970 + 4.15901240 * b_
                    + a_ * (-2.19557347 + 1.75198401 * b_
                    + a_ * (-2.13704948 - 10.02301043 * b_
                    + a_ * (-4.24894561 + 5.38770819 * b_ + 4.69891013 * a_
                    )))
            )

    val T_mid = 0.11239642 + 1 / (
            +1.61320320 - 0.68124379 * b_
                    + a_ * (+0.40370612 + 0.90148123 * b_
                    + a_ * (-0.27087943 + 0.61223990 * b_
                    + a_ * (+0.00299215 - 0.45399568 * b_ - 0.14661872 * a_
                    )))
            )

    val k: Double = C_max / min((L * ST_max[0]), (1 - L) * ST_max[1])

    val C_mid: Double
    run {
        val C_a = L * S_mid
        val C_b = (1 - L) * T_mid

        C_mid = (0.9 * k) * sqrt(sqrt(1 / (1 / (C_a * C_a * C_a * C_a) + 1 / (C_b * C_b * C_b * C_b))))
    }

    val C_0: Double
    run {
        val C_a = L * 0.4
        val C_b = (1 - L) * 0.8

        C_0 = sqrt(1 / (1 / (C_a * C_a) + 1 / (C_b * C_b)))
    }

    return doubleArrayOf(C_0, C_mid, C_max)
}

fun find_gamut_intersection(
    a: Double,
    b: Double,
    L1: Double,
    C1: Double,
    L0: Double,
    cusp: DoubleArray? = null
): Double {
    val cusp = cusp ?: find_cusp(a, b)


    // Find the intersection for upper and lower half seprately
    var t: Double
    if (((L1 - L0) * cusp[1] - (cusp[0] - L0) * C1) <= 0) {
        // Lower half

        t = cusp[1] * L0 / (C1 * cusp[0] + cusp[1] * (L0 - L1))
    } else {
        // Upper half

        // First intersect with triangle
        t = cusp[1] * (L0 - 1) / (C1 * (cusp[0] - 1) + cusp[1] * (L0 - L1))

        // Then one step Halley's method
        run {
            val dL = L1 - L0
            val dC = C1

            val k_l = +0.3963377774 * a + 0.2158037573 * b
            val k_m = -0.1055613458 * a - 0.0638541728 * b
            val k_s = -0.0894841775 * a - 1.2914855480 * b

            val l_dt = dL + dC * k_l
            val m_dt = dL + dC * k_m
            val s_dt = dL + dC * k_s;


            // If higher accuracy is required, 2 or 3 iterations of the following block can be used:
            {
                val L = L0 * (1 - t) + t * L1
                val C = t * C1

                val l_ = L + C * k_l
                val m_ = L + C * k_m
                val s_ = L + C * k_s

                val l = l_ * l_ * l_
                val m = m_ * m_ * m_
                val s = s_ * s_ * s_

                val ldt = 3 * l_dt * l_ * l_
                val mdt = 3 * m_dt * m_ * m_
                val sdt = 3 * s_dt * s_ * s_

                val ldt2 = 6 * l_dt * l_dt * l_
                val mdt2 = 6 * m_dt * m_dt * m_
                val sdt2 = 6 * s_dt * s_dt * s_

                val r = 4.0767416621 * l - 3.3077115913 * m + 0.2309699292 * s - 1
                val r1 = 4.0767416621 * ldt - 3.3077115913 * mdt + 0.2309699292 * sdt
                val r2 = 4.0767416621 * ldt2 - 3.3077115913 * mdt2 + 0.2309699292 * sdt2

                val u_r = r1 / (r1 * r1 - 0.5 * r * r2)
                var t_r = -r * u_r

                val g = -1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s - 1
                val g1 = -1.2684380046 * ldt + 2.6097574011 * mdt - 0.3413193965 * sdt
                val g2 = -1.2684380046 * ldt2 + 2.6097574011 * mdt2 - 0.3413193965 * sdt2

                val u_g = g1 / (g1 * g1 - 0.5 * g * g2)
                var t_g = -g * u_g

                val b = -0.0041960863 * l - 0.7034186147 * m + 1.7076147010 * s - 1
                val b1 = -0.0041960863 * ldt - 0.7034186147 * mdt + 1.7076147010 * sdt
                val b2 = -0.0041960863 * ldt2 - 0.7034186147 * mdt2 + 1.7076147010 * sdt2

                val u_b = b1 / (b1 * b1 - 0.5 * b * b2)
                var t_b = -b * u_b

                t_r = if (u_r >= 0) t_r else 10e5
                t_g = if (u_g >= 0) t_g else 10e5
                t_b = if (u_b >= 0) t_b else 10e5

                t += min(t_r, min(t_g, t_b))
            }
        }
    }

    return t
}
