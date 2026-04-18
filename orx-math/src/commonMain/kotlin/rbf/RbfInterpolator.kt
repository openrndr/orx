package org.openrndr.extra.math.rbf

import org.openrndr.extra.math.matrix.Matrix
import org.openrndr.extra.math.matrix.columnMean
import org.openrndr.extra.math.matrix.invertMatrixCholesky
import org.openrndr.extra.math.matrix.minus
import org.openrndr.math.EuclideanVector
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.math.Vector4
import kotlin.math.exp
import kotlin.math.sqrt

typealias Rbf = (Double) -> Double

/**
 * Creates a Gaussian radial basis function (RBF) with the given scale parameter.
 * The resulting RBF computes the exponential decay based on the squared distance scaled by the parameter.
 *
 * @param scale The scale parameter influencing the width of the Gaussian RBF. Smaller values result in a steeper decay.
 * @return A function representing the Gaussian RBF, which takes a square of the distance as input and returns the RBF value.
 */
fun rbfGaussian(scale: Double): Rbf {
    val scale2 = scale * scale
    return { d ->
        exp(-d * scale2)
    }
}

/**
 * Radial basis function (RBF) using the inverse quadratic formula.
 *
 * Creates an RBF that calculates the inverse quadratic function based on the given scale.
 *
 * @param scale A scaling factor that determines the influence range of the RBF.
 * @return A lambda function representing the inverse quadratic RBF.
 */
fun rbfInverseQuadratic(scale: Double): Rbf {
    val scale2 = scale * scale
    return { d ->
        1.0 / (1.0 + d * scale2)
    }
}

/**
 * Generates a radial basis function (RBF) using the inverse multiquadratic kernel.
 *
 * @param scale The scaling factor that influences the spread and shape of the RBF.
 * @return A function representing the inverse multiquadratic RBF, which computes the value
 *         based on the given squared distance.
 */
fun rbfInverseMultiQuadratic(scale: Double): Rbf {
    val scale2 = scale * scale
    return { d ->
        1.0 / sqrt(1.0 + d * scale2)
    }
}

/**
 * Represents a Radial Basis Function (RBF) interpolator for multidimensional data.
 *
 * This class implements an interpolator that takes a set of points in a multidimensional space,
 * applies a radial basis function to interpolate values at new points.
 *
 * @param T The type of vector used in the interpolation, which must implement the `EuclideanVector<T>` interface.
 * @property points A list of vectors representing the input points in the multidimensional space.
 * @property weights A 2D array containing the weights calculated for the RBF interpolation.
 * @property values A 2D array of the target values corresponding to the input points, where each row maps to a point.
 * @property rbf A radial basis function that computes values based on a squared distance (e.g., Gaussian, cubic, etc.).
 * @property mean A 1D array representing the mean offset values applied to the interpolation result.
 */
class RbfNDInterpolator<T: EuclideanVector<T>>(
    val points: List<T>,
    val weights: Array<DoubleArray>,
    val values: Array<DoubleArray>,
    val rbf: (Double) -> Double,
    val mean: DoubleArray
) {
    fun interpolate(x: T): DoubleArray {
        val c = DoubleArray(values[0].size)
        for (j in points.indices) {
            val r = rbf(points[j].squaredDistanceTo(x))
            for (i in 0 until c.size) {
                c[i] += weights[j][i] * r
            }
        }
        for (i in 0 until c.size) {
            c[i] += mean[i]
        }
        return c
    }
}

fun <T: EuclideanVector<T>> RbfNDInterpolator(
    points: List<T>,
    values: Array<DoubleArray>,
    smoothing: Double = 0.0,
    rbf: Rbf
): RbfNDInterpolator<T> {

    val rmat = Matrix(points.size, points.size)
    for (j in points.indices) {
        for (i in points.indices) {
            rmat[i, j] = rbf(points[i].squaredDistanceTo(points[j])) + if (j == i) smoothing else 0.0
        }
    }

    val imat = invertMatrixCholesky(rmat)

    val vmat = Matrix(points.size, values[0].size)
    for (j in points.indices) {
        for (i in values[0].indices) {
            vmat[j, i] = values[j][i]
        }
    }
    val mean = vmat.columnMean()
    val vwmat = vmat - mean

    val wmat = imat * vwmat
    return RbfNDInterpolator(points, wmat.data, values, rbf, mean.data[0])
}

typealias Rbf2DInterpolator = RbfNDInterpolator<Vector2>
typealias Rbf3DInterpolator = RbfNDInterpolator<Vector3>
typealias Rbf4DInterpolator = RbfNDInterpolator<Vector4>

fun Rbf2DInterpolator(
    points: List<Vector2>,
    values: Array<DoubleArray>,
    smoothing: Double = 0.0,
    rbf: Rbf
) = RbfNDInterpolator(points, values, smoothing, rbf)

fun Rbf3DInterpolator(
    points: List<Vector3>,
    values: Array<DoubleArray>,
    smoothing: Double = 0.0,
    rbf: Rbf
) = RbfNDInterpolator(points, values, smoothing, rbf)

fun Rbf4DInterpolator(
    points: List<Vector4>,
    values: Array<DoubleArray>,
    smoothing: Double = 0.0,
    rbf: Rbf
) = RbfNDInterpolator(points, values, smoothing, rbf)