package org.openrndr.extra.math.complex

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmRecord
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.cosh
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sinh

/**
 * Represents a complex number with a real and imaginary part.
 *
 * Provides functionality to perform common mathematical operations
 * with complex numbers, such as addition, subtraction, multiplication,
 * division, and more. Includes utility functions for magnitude,
 * argument, and conversions between polar and rectangular forms.
 *
 * @property real The real part of the complex number.
 * @property imaginary The imaginary part of the complex number.
 */
@JvmRecord
@Serializable
data class Complex(val real: Double, val imaginary: Double) {
    operator fun plus(other: Complex): Complex {
        return Complex(real + other.real, imaginary + other.imaginary)
    }

    operator fun times(other: Complex): Complex {
        return Complex(real * other.real - imaginary * other.imaginary, real * other.imaginary + imaginary * other.real)
    }

    operator fun unaryMinus(): Complex {
        return Complex(-real, -imaginary)
    }

    operator fun div(other: Complex): Complex {
        val c = other.real * other.real + other.imaginary * other.imaginary
        return Complex(
            (real * other.real + imaginary * other.imaginary) / c,
            (imaginary * other.real - real * other.imaginary) / c
        )
    }

    operator fun div(other: Double): Complex {
        return Complex(real / other, imaginary / other)
    }

    operator fun minus(other: Complex): Complex {
        return Complex(real - other.real, imaginary - other.imaginary)
    }

    operator fun times(other: Double): Complex {
        return Complex(real * other, imaginary * other)
    }

    /**
     * Calculates the magnitude (or absolute value) of the complex number.
     * The magnitude is computed as the square root of the sum of the squares
     * of the real and imaginary parts.
     *
     * @return The magnitude of the complex number.
     */
    fun magnitude(): Double {
        return kotlin.math.sqrt(real * real + imaginary * imaginary)
    }

    /**
     * Computes the squared magnitude (or squared absolute value) of the complex number.
     * The squared magnitude is determined as the sum of the squares of the real and imaginary parts.
     *
     * @return The squared magnitude of the complex number.
     */
    fun sqrMagnitude(): Double {
        return real * real + imaginary * imaginary
    }

    /**
     * Computes the conjugate of the complex number.
     * The conjugate of a complex number is formed by changing the sign of its imaginary part.
     *
     * @return A new instance of [Complex] representing the conjugate of the current complex number.
     */
    fun conjugate(): Complex {
        return Complex(real, -imaginary)
    }

    /**
     * Normalizes the complex number to a unit magnitude.
     * The normalized complex number retains the same direction in the complex plane
     * but has a magnitude of 1.
     *
     * @return A new instance of [Complex] representing the normalized complex number.
     */
    fun normalize(): Complex {
        val m = magnitude()
        return Complex(real / m, imaginary / m)
    }

    /**
     * Computes the principal square root of the complex number.
     * The square root is calculated based on the polar representation of the complex number.
     *
     * @return A new instance of [Complex] representing the square root of the current complex number.
     */
    fun sqrt(): Complex {
        val r = kotlin.math.sqrt(kotlin.math.sqrt(real * real + imaginary * imaginary))
        val t = atan2(imaginary, real) / 2.0
        return Complex(r * cos(t), r * sin(t))
    }

    /**
     * Raises the current complex number to the power of the given exponent.
     * The operation is performed in polar form, where the magnitude is raised
     * to the exponent and the argument is multiplied by the exponent.
     *
     * @param exponent The exponent to which the complex number is raised.
     * @return A new instance of [Complex] representing the result of the operation.
     */
    fun pow(exponent: Double): Complex {
        val m = magnitude().pow(exponent)
        val phi = argument() * exponent

        return Complex(m * cos(phi), m * sin(phi))
    }

    /**
     * Computes the argument (or angle) of the complex number in polar coordinates.
     * The argument is the angle formed by the positive real axis and the line representing the complex number
     * in the complex plane, measured in radians.
     *
     * @return The argument of the complex number in radians.
     */
    fun argument(): Double {
        return atan2(imaginary, real)
    }

    companion object {
        fun fromRadians(radians: Double): Complex {
            return Complex(cos(radians), sin(radians))
        }

        fun fromPolar(magnitude: Double, argument: Double): Complex {
            return Complex(magnitude * cos(argument), magnitude * sin(argument))
        }
    }
}

/**
 * Divides a double-precision floating-point number by a complex number and returns the result.
 *
 * The division is performed using the formula for dividing a real number by a complex number.
 *
 * @param other The complex number to divide by.
 * @return A new instance of [Complex] representing the result of the division.
 */
operator fun Double.div(other: Complex): Complex {
    val c = other.real * other.real + other.imaginary * other.imaginary
    return Complex((this * other.real) / c, (-this * other.imaginary) / c)
}

/**
 * Raises a real number to the power of a complex number.
 *
 * @param exponent The complex exponent to which the real number will be raised.
 * @return A [Complex] number representing the result of raising this real number
 *         to the power of the given complex exponent.
 */
fun Double.pow(exponent: Complex): Complex {
    val be = this.pow(exponent.real)
    val phase = exponent.imaginary * ln(this)
    return Complex(be * cos(phase), be * sin(phase))
}

/**
 * Computes the cosine of a complex number.
 * The cosine of a complex number is calculated using the formula:
 * cos(a + bi) = cos(a)cosh(b) - i*sin(a)sinh(b),
 * where a and b are the real and imaginary parts of the complex number, respectively.
 *
 * @param complex The complex number for which the cosine is to be calculated.
 * @return A new instance of [Complex] representing the cosine of the given complex number.
 */
fun cos(complex: Complex): Complex {
    return Complex(cos(complex.real) * cosh(complex.imaginary), -sin(complex.real) * sinh(complex.imaginary))
}

/**
 * Computes the sine of a given complex number using the formula:
 * sin(z) = sin(a) * cosh(b) - i * cos(a) * sinh(b),
 * where z = a + bi is the complex number, a is the real part, and b is the imaginary part.
 *
 * @param complex The complex number for which the sine is computed.
 * @return A new instance of [Complex] representing the sine of the given complex number.
 */
fun sin(complex: Complex): Complex {
    return Complex(sin(complex.real) * cosh(complex.imaginary), -cos(complex.real) * sinh(complex.imaginary))
}

/**
 * Computes the tangent of a given complex number.
 * The tangent of a complex number is calculated as the quotient of its sine and cosine.
 *
 * @param complex The complex number for which the tangent is to be calculated.
 * @return A new instance of [Complex] representing the tangent of the given complex number.
 */
fun tan(complex: Complex): Complex {
    return sin(complex) / cos(complex)
}

/**
 * Computes the cotangent of a complex number.
 * The cotangent is calculated using the formula cot(z) = cos(z) / sin(z),
 * where z is the complex number.
 *
 * @param complex The complex number for which the cotangent is calculated.
 * @return A new instance of [Complex] representing the cotangent of the given complex number.
 */
fun cot(complex: Complex): Complex {
    return cos(complex) / sin(complex)
}

/**
 * Computes the natural logarithm of a complex number.
 * The natural logarithm is calculated using the formula:
 * ln(z) = ln(|z|) + i * arg(z)
 * where |z| is the magnitude and arg(z) is the argument of the complex number.
 *
 * @param complex The complex number for which the natural logarithm is calculated.
 * @return A new instance of [Complex] representing the natural logarithm of the given complex number.
 */
fun ln(complex: Complex): Complex {
    return Complex(ln(complex.magnitude()), complex.argument())
}

/**
 * Computes the exponential of a complex number.
 * The exponential is calculated using the formula:
 * exp(a + bi) = e^a * (cos(b) + i*sin(b))
 * where a and b are the real and imaginary parts of the complex number, respectively.
 *
 * @param complex The complex number for which the exponential is calculated.
 * @return A new instance of [Complex] representing the exponential of the given complex number.
 */
fun exp(complex: Complex): Complex {
    val expReal = exp(complex.real)
    return Complex(expReal * cos(complex.imaginary), expReal * sin(complex.imaginary))
}

/**
 * Computes the logarithm of a complex number with a specified base.
 * The logarithm with base b is calculated using the formula:
 * log_b(z) = ln(z) / ln(b)
 * where z is the complex number and b is the base.
 *
 * @param x The complex number for which the logarithm is calculated.
 * @param base The base of the logarithm (must be positive and not equal to 1).
 * @return A new instance of [Complex] representing the logarithm of the given complex number with the specified base.
 */
fun log(x: Complex, base: Double): Complex {
    require(base > 0 && base != 1.0) { "Logarithm base must be positive and not equal to 1" }
    return ln(x) / ln(base)
}

/**
 * Computes the logarithm of a complex number with a specified complex base.
 * The logarithm with base b is calculated using the formula:
 * log_b(z) = ln(z) / ln(b)
 * where z is the complex number and b is the complex base.
 *
 * @param x The complex number for which the logarithm is calculated.
 * @param base The complex base of the logarithm.
 * @return A new instance of [Complex] representing the logarithm of the given complex number with the specified complex base.
 */
fun log(x: Complex, base: Complex): Complex {
    require(base != Complex(1.0, 0.0)) { "Logarithm base must not be equal to 1" }
    require(base.magnitude() > 0) { "Logarithm base must have non-zero magnitude" }
    return ln(x) / ln(base)
}

/**
 * Computes the arc cosine (inverse cosine) of a complex number.
 * The arc cosine is calculated using the formula:
 * acos(z) = -i * ln(z + i * sqrt(1 - z²))
 * where z is the complex number.
 *
 * @param complex The complex number for which the arc cosine is calculated.
 * @return A new instance of [Complex] representing the arc cosine of the given complex number.
 */
fun acos(complex: Complex): Complex {
    val z2 = complex * complex
    val oneMinusZ2 = Complex(1.0, 0.0) - z2
    val sqrt = oneMinusZ2.sqrt()
    val sum = complex + Complex(0.0, 1.0) * sqrt
    // The negative sign is applied to the entire result
    return Complex(0.0, 1.0) * ln(sum) * Complex(-1.0, 0.0)
}

/**
 * Computes the arc sine (inverse sine) of a complex number.
 * The arc sine is calculated using the formula:
 * asin(z) = -i * ln(i * z + sqrt(1 - z²))
 * where z is the complex number.
 *
 * @param complex The complex number for which the arc sine is calculated.
 * @return A new instance of [Complex] representing the arc sine of the given complex number.
 */
fun asin(complex: Complex): Complex {
    val z2 = complex * complex
    val oneMinusZ2 = Complex(1.0, 0.0) - z2
    val sqrt = oneMinusZ2.sqrt()
    val sum = Complex(0.0, 1.0) * complex + sqrt
    return Complex(0.0, -1.0) * ln(sum)
}

/**
 * Raises a real number to the power of the given exponent and returns the result as a complex number.
 *
 * This function internally converts the real number to a complex number with an imaginary part of 0,
 * and then performs the power operation.
 *
 * @param exponent The exponent to which the number is raised.
 * @return A [Complex] instance representing the result of raising the number to the given power.
 */
fun Double.cpow(exponent: Double): Complex = Complex(this, 0.0).pow(exponent)

/**
 * Computes the arc tangent (inverse tangent) of a complex number.
 * The arc tangent is calculated using the formula:
 * atan(z) = (i/2) * ln((i+z)/(i-z))
 * where z is the complex number.
 *
 * @param complex The complex number for which the arc tangent is calculated.
 * @return A new instance of [Complex] representing the arc tangent of the given complex number.
 */
fun atan(complex: Complex): Complex {
    val i = Complex(0.0, 1.0)
    val numerator = i + complex
    val denominator = i - complex
    val fraction = numerator / denominator
    return i * ln(fraction) * Complex(0.5, 0.0)
}

/**
 * Computes the hyperbolic cosine of a complex number.
 * The hyperbolic cosine is calculated using the formula:
 * cosh(a + bi) = cosh(a)cos(b) + i·sinh(a)sin(b),
 * where a and b are the real and imaginary parts of the complex number, respectively.
 *
 * @param complex The complex number for which the hyperbolic cosine is calculated.
 * @return A new instance of [Complex] representing the hyperbolic cosine of the given complex number.
 */
fun cosh(complex: Complex): Complex =
    Complex(cosh(complex.real) * cos(complex.imaginary), sinh(complex.real) * sin(complex.imaginary))

/**
 * Computes the hyperbolic sine of a complex number.
 * The hyperbolic sine is calculated using the formula:
 * sinh(a + bi) = sinh(a)cos(b) + i·cosh(a)sin(b),
 * where a and b are the real and imaginary parts of the complex number, respectively.
 *
 * @param complex The complex number for which the hyperbolic sine is calculated.
 * @return A new instance of [Complex] representing the hyperbolic sine of the given complex number.
 */
fun sinh(complex: Complex): Complex =
    Complex(sinh(complex.real) * cos(complex.imaginary), cosh(complex.real) * sin(complex.imaginary))

/**
 * Computes the inverse hyperbolic cosine of a complex number.
 * The inverse hyperbolic cosine is calculated using the formula:
 * acosh(z) = ln(z + sqrt(z² - 1))
 * where z is the complex number.
 *
 * @param complex The complex number for which the inverse hyperbolic cosine is calculated.
 * @return A new instance of [Complex] representing the inverse hyperbolic cosine of the given complex number.
 */
fun acosh(complex: Complex): Complex {
    val z2 = complex * complex
    val z2Minus1 = z2 - Complex(1.0, 0.0)
    val sqrt = z2Minus1.sqrt()
    val sum = complex + sqrt
    return ln(sum)
}

/**
 * Computes the inverse hyperbolic sine of a complex number.
 * The inverse hyperbolic sine is calculated using the formula:
 * asinh(z) = ln(z + sqrt(z² + 1))
 * where z is the complex number.
 *
 * @param complex The complex number for which the inverse hyperbolic sine is calculated.
 * @return A new instance of [Complex] representing the inverse hyperbolic sine of the given complex number.
 */
fun asinh(complex: Complex): Complex {
    val z2 = complex * complex
    val z2Plus1 = z2 + Complex(1.0, 0.0)
    val sqrt = z2Plus1.sqrt()
    val sum = complex + sqrt
    return ln(sum)
}

/**
 * Computes the hyperbolic tangent of a complex number.
 * The hyperbolic tangent is calculated using the formula:
 * tanh(z) = sinh(z) / cosh(z)
 * where z is the complex number.
 *
 * @param complex The complex number for which the hyperbolic tangent is calculated.
 * @return A new instance of [Complex] representing the hyperbolic tangent of the given complex number.
 */
fun tanh(complex: Complex): Complex = sinh(complex) / cosh(complex)

/**
 * Computes the inverse hyperbolic tangent of a complex number.
 * The inverse hyperbolic tangent is calculated using the formula:
 * atanh(z) = (1/2) * ln((1+z)/(1-z))
 * where z is the complex number.
 *
 * Special cases:
 * - For z = i (imaginary unit), atanh(i) = i*π/2
 * - For z = -i (negative imaginary unit), atanh(-i) = -i*π/2
 *
 * @param complex The complex number for which the inverse hyperbolic tangent is calculated.
 * @return A new instance of [Complex] representing the inverse hyperbolic tangent of the given complex number.
 */
fun atanh(complex: Complex): Complex {
    // Special cases for imaginary unit values
    if (complex.real == 0.0 && complex.imaginary == 1.0) {
        return Complex(0.0, kotlin.math.PI / 2)
    }
    if (complex.real == 0.0 && complex.imaginary == -1.0) {
        return Complex(0.0, -kotlin.math.PI / 2)
    }
    
    val one = Complex(1.0, 0.0)
    val numerator = one + complex
    val denominator = one - complex
    val fraction = numerator / denominator
    return ln(fraction) * Complex(0.5, 0.0)
}

