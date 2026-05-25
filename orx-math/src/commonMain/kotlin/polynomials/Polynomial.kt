import org.openrndr.math.fma
import kotlin.jvm.JvmInline
import kotlin.math.*

/**
 * Class representing a polynomial.
 */
@JvmInline
value class Polynomial(val coefficients: DoubleArray) {
    val degree: Int
        get() = coefficients.size - 1

    /**
     * @param coefficients coefficients in order of increasing power eg 1 + 2x + 3x^2 - 12x^3 => [1, 2, 3, -12]
     */

    fun evaluate(x: Double): Double {
        var xx = 1.0
        var result = 0.0
        for (i in coefficients.indices) {
            result = fma(coefficients[i], xx, result)
            xx *= x
        }
        return result
    }

    fun derivative(): Polynomial {
        val newCoefficients = DoubleArray(coefficients.size-1)
        for (i in 1 until coefficients.size) {
            newCoefficients[i - 1] = coefficients[i] * i
        }
        return Polynomial(newCoefficients)
    }
}

/**
 * Find the roots of polynomial using the method described in
 * High-Performance Polynomial Root Finding for Graphics (Yuksel 2022)
 *
 * @param f polynomial to find the roots of
 * @param startInterval beginning of interval to search (defaults to -1000)
 * @param endInterval end of interval to search (defaults to 1000)
 * @param epsilon tolerance for root finding (defaults to 1e-8)
 * @return A list of roots
 */
fun polynomialRoots(
    f: Polynomial,
    startInterval: Double = -1000.0,
    endInterval: Double = 1000.0,
    epsilon: Double = 1e-8
): List<Double> {
    if (f.degree == 2) {
        return findQuadraticRoots(f, startInterval, endInterval)
    }

    val derivative = f.derivative()

    val rootsOfDerivative = polynomialRoots(derivative, startInterval, endInterval, epsilon).toMutableList()
    rootsOfDerivative.add(endInterval)

    var a = startInterval
    var fa = f.evaluate(startInterval)

    val roots = mutableListOf<Double>()

    for (i in rootsOfDerivative.indices) {
        val b = rootsOfDerivative[i]
        val fb = f.evaluate(b)

        if (sign(fa) != sign(fb)) {
            val r = findRoot(f, derivative, a, b, fa, epsilon)
            if (f.degree == 3) {
                val deflated = deflate(f, r)
                return listOf(r) + findQuadraticRoots(deflated, startInterval, endInterval)
            }

            roots.add(r)
        }

        a = b
        fa = fb
    }

    return roots
}

private fun deflate(f: Polynomial, xr: Double): Polynomial {
    val c = f.coefficients[1]
    val b = f.coefficients[2]
    val a = f.coefficients[3]
    val ap = a
    val bp = fma(ap, xr, b)
    val cp = fma(bp, xr, c)
    //val bp = b + ap * xr
    //val cp = c + bp * xr
    return Polynomial(doubleArrayOf(cp, bp, ap))
}

private fun findQuadraticRoots(f: Polynomial, startInterval: Double, endInterval: Double): List<Double> {
    val c = f.coefficients[0]
    val b = f.coefficients[1]
    val a = f.coefficients[2]
    val delta = b * b - 4 * a * c

    if (delta >= 0) {
        val d = sqrt(delta)
        val q = -0.5 * (b + multSign(d, b))
        val rv0 = q / a
        val rv1 = c / q

        val res = mutableListOf<Double>()
        val aa = min(rv0, rv1)
        val bb = max(rv0, rv1)

        if (aa >= startInterval && aa <= endInterval) {
            res.add(aa)
        }

        if (bb >= startInterval && bb <= endInterval) {
            res.add(bb)
        }

        return res
    }

    return emptyList()
}

private fun multSign(v: Double, sign: Double): Double {
    return v * (if (sign < 0) -1 else 1)
}

// Following http://www.cemyuksel.com/research/polynomials/polynomial_roots_hpg2022_supplemental.pdf
private fun findRoot(
    f: Polynomial,
    deriv: Polynomial,
    x1: Double,
    x2: Double,
    fx1: Double,
    epsilon: Double
): Double {
    var xr = (x1 + x2) / 2
    var currentX1 = x1
    var currentX2 = x2

    if (abs(x2 - x1) <= 2 * epsilon) {
        return xr
    }

    if (f.degree == 3) {
        var xn: Double
        for (i in 0 until 10) {
            xn = xr - f.evaluate(xr) / deriv.evaluate(xr)
            xn = max(currentX1, min(currentX2, xn))

            if (abs(xr - xn) <= epsilon) {
                return xn
            }

            xr = xn
        }

        if (xr < currentX1 || xr > currentX2) {
            xr = (currentX1 + currentX2) / 2
        }
    }

    val y1 = fx1
    var yr = f.evaluate(xr)

    while (true) {
        if (sign(yr) == sign(y1)) {
            currentX1 = xr
        } else {
            currentX2 = xr
        }

        val xn = xr - yr / deriv.evaluate(xr)

        if (currentX1 < xn && xn < currentX2) {
            if (abs(xr - xn) > epsilon) {
                xr = xn
                yr = f.evaluate(xr)
            } else {
                xr = if (sign(yr) == sign(y1)) {
                    xn + epsilon
                } else {
                    xn - epsilon
                }

                val y = f.evaluate(xr)
                if (sign(y) != sign(yr)) {
                    return xn
                } else {
                    yr = y
                }
            }
        } else {
            xr = (currentX1 + currentX2) / 2
            if (xr == currentX1 || xr == currentX2 || currentX2 - currentX1 <= 2 * epsilon) {
                return xr
            } else {
                yr = f.evaluate(xr)
            }
        }
    }
}