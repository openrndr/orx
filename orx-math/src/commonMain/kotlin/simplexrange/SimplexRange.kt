package org.openrndr.extra.math.simplexrange

import org.openrndr.math.LinearType
import org.openrndr.math.Parametric2D
import org.openrndr.math.Parametric3D
import org.openrndr.math.Parametric4D
import kotlin.jvm.JvmRecord

import kotlin.math.cbrt
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Transforms a given array of coordinates into an array of coefficients
 * for use in simplex-based calculations.
 *
 * @param b An array of doubles representing coordinates.
 *          The size of this array determines the dimensionality of the input simplex.
 * @return A new array of doubles representing the transformed coefficients,
 *         with one additional element compared to the input array.
 */
fun simplexUpscale(b: DoubleArray): DoubleArray {
    val transformed = DoubleArray(b.size) {
        b[it].pow(1.0 / (b.size - it))
    }
    var m = 1.0
    val result = DoubleArray(b.size + 1) {
        val neg = if (it < transformed.size) 1.0 - transformed[it] else 1.0
        val v = m * neg
        m *= if (it < transformed.size) transformed[it] else 1.0
        v
    }
    return result
}

/**
 * Represents a 2D simplex range interpolated in a parametric space.
 * This class defines a triangular range in 2D space, parameterized by three control points `x0`, `x1`, and `x2`.
 * It implements the `Parametric2D` interface, allowing evaluation of linear combinations
 * of these control points based on two parameters `u` and `v`.
 *
 * @param T The type parameter constrained to types that implement `LinearType<T>`, enabling
 *          operations such as addition, multiplication, and scalar interpolation.
 * @property x0 The first control point of the simplex.
 * @property x1 The second control point of the simplex.
 * @property x2 The third control point of the simplex.
 */
@JvmRecord
data class SimplexRange2D<T : LinearType<T>>(val x0: T, val x1: T, val x2: T) : Parametric2D<T> {
    override fun value(u: Double, v: Double): T {
        val r1 = sqrt(u)
        val r2 = v

        val a = 1 - r1
        val b = r1 * (1 - r2)
        val c = r1 * r2
        return x0 * a + x1 * b + x2 * c
    }
}

/**
 * Represents a 3D parametric simplex range defined by four control points.
 *
 * @param T The type of the coordinate values in the 3D space, which must extend LinearType.
 * @property x0 The first control point defining the simplex.
 * @property x1 The second control point defining the simplex.
 * @property x2 The third control point defining the simplex.
 * @property x3 The fourth control point defining the simplex.
 */
@JvmRecord
data class SimplexRange3D<T : LinearType<T>>(val x0: T, val x1: T, val x2: T, val x3: T) : Parametric3D<T> {
    override fun value(u: Double, v: Double, w: Double): T {
        val r1 = cbrt(u)
        val r2 = sqrt(v)
        val r3 = w

        val a = 1 - r1
        val b = r1 * (1 - r2)
        val c = r1 * r2 * (1 - r3)
        val d = r1 * r2 * r3
        return x0 * a + x1 * b + x2 * c + x3 * d
    }
}

/**
 * Represents a 4D parametric simplex range defined by five control points of type `T`.
 *
 * This class computes a value within the simplex range based on four parametric inputs (u, v, w, t).
 * The control points x0, x1, x2, x3, and x4 determine the shape of the simplex, and the resulting value
 * is calculated as a weighted combination of the control points using barycentric-like coordinates derived
 * from the parametric inputs.
 *
 * The generic type `T` must extend `LinearType<T>`, as the calculation requires linear operations.
 *
 * @param T the type of each control point, constrained to types that implement `LinearType`.
 * @property x0 the first control point of the simplex.
 * @property x1 the second control point of the simplex.
 * @property x2 the third control point of the simplex.
 * @property x3 the fourth control point of the simplex.
 * @property x4 the fifth control point of the simplex.
 */
@JvmRecord
data class SimplexRange4D<T : LinearType<T>>(val x0: T, val x1: T, val x2: T, val x3: T, val x4: T) : Parametric4D<T> {
    override fun value(u: Double, v: Double, w: Double, t: Double): T {
        val r1 = u.pow(1.0 / 4.0)
        val r2 = cbrt(v)
        val r3 = sqrt(w)
        val r4 = t

        val a = 1 - r1
        val b = r1 * (1 - r2)
        val c = r1 * r2 * (1 - r3)
        val d = r1 * r2 * r3 * (1 - r4)
        val e = r1 * r2 * r3 * r4
        return x0 * a + x1 * b + x2 * c + x3 * d + x4 * e
    }
}

/**
 * Represents a value defined over an N-dimensional simplex range.
 *
 * This class is constructed using a list of elements of a generic type `T` that
 * conforms to the `LinearType` interface. The `SimplexRangeND` allows evaluating
 * a value within the simplex defined by these elements, which are scaled and
 * combined based on a provided set of barycentric coordinates.
 *
 * @param T The type of elements in the simplex, which must implement the `LinearType` interface.
 * @property x The list of elements representing the vertices of the N-dimensional simplex.
 */
class SimplexRangeND<T: LinearType<T>>(val x: List<T>) {
    /**
     * Computes a value determined by the coordinates given in the input array.
     *
     * The method uses the `simplexUpscale` function to transform the input array, resulting in a set of
     * coefficients. These coefficients are then used to calculate a weighted combination of the elements
     * in the simplex, represented by the `x` property of the class.
     *
     * @param u An array of doubles representing the coordinates for the simplex.
     *          The size of the array must be one less than the number of elements in the simplex.
     * @return A value of type `T` computed as the weighted combination of the simplex elements.
     */
    fun value(u : DoubleArray): T {
        val b = simplexUpscale(u)
        var r = x[0] * b [0]
        for (i in 1 until x.size) {
            r += x[i] * b[i]
        }
        return r
    }
}